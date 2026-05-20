"""
Video OCR Subtitle Extractor
从视频中提取硬字幕，生成 SRT 字幕文件
支持中文、英文及中英混合字幕

依赖安装:
    pip install easyocr opencv-python-headless numpy

用法:
    python video_ocr_subtitles.py input.mp4
    python video_ocr_subtitles.py input.mp4 -o output.srt --fps 2
    python video_ocr_subtitles.py input.mp4 --region bottom  # 只扫描底部区域
"""

import argparse
import sys
import os
import re
from pathlib import Path
from difflib import SequenceMatcher

import cv2
import easyocr
import numpy as np


# --------------- 工具函数 ---------------

def format_timestamp(seconds: float) -> str:
    """秒数转 SRT 时间戳格式 HH:MM:SS,mmm"""
    h = int(seconds // 3600)
    m = int((seconds % 3600) // 60)
    s = int(seconds % 60)
    ms = int((seconds % 1) * 1000)
    return f"{h:02d}:{m:02d}:{s:02d},{ms:03d}"


def text_similarity(a: str, b: str) -> float:
    """计算两段文字的相似度 (0~1)"""
    if not a and not b:
        return 1.0
    if not a or not b:
        return 0.0
    return SequenceMatcher(None, a, b).ratio()


def clean_text(raw_lines: list[str]) -> str:
    """清洗 OCR 结果，合并多行，去除噪音"""
    lines = []
    for line in raw_lines:
        # 去除首尾空白
        line = line.strip()
        # 去除纯符号/纯数字噪音（长度太短且无中英文）
        if len(line) < 2:
            continue
        # 去除常见水印关键词（按需添加）
        if re.match(r'^(www\.|http|@|#)', line.lower()):
            continue
        lines.append(line)
    return "\n".join(lines)


def extract_roi(frame: np.ndarray, region: str) -> np.ndarray:
    """根据区域参数裁剪画面"""
    h, w = frame.shape[:2]
    if region == "bottom":
        return frame[int(h * 0.55):h, :]
    elif region == "top":
        return frame[0:int(h * 0.35), :]
    elif region == "middle":
        return frame[int(h * 0.25):int(h * 0.75), :]
    else:  # "full"
        return frame


# --------------- 核心逻辑 ---------------

class SubtitleExtractor:
    def __init__(self, languages=None, gpu: bool = True):
        if languages is None:
            languages = ["ch_sim", "en"]
        print(f"初始化 EasyOCR (语言: {languages}, GPU: {gpu})...")
        self.reader = easyocr.Reader(languages, gpu=gpu)
        print("EasyOCR 初始化完成\n")

    def extract_frames(self, video_path: str, fps: float = 1.0):
        """从视频中按指定帧率抽取帧"""
        cap = cv2.VideoCapture(video_path)
        if not cap.isOpened():
            raise FileNotFoundError(f"无法打开视频: {video_path}")

        video_fps = cap.get(cv2.CAP_PROP_FPS)
        total_frames = int(cap.get(cv2.CAP_PROP_FRAME_COUNT))
        duration = total_frames / video_fps if video_fps > 0 else 0

        print(f"视频信息: {video_fps:.1f}fps, 总帧数: {total_frames}, 时长: {duration:.1f}s")
        print(f"抽帧间隔: 每 {1/fps:.2f}s 一帧 (目标 {fps} fps)\n")

        frame_interval = int(video_fps / fps)
        if frame_interval < 1:
            frame_interval = 1

        frame_idx = 0
        while True:
            ret, frame = cap.read()
            if not ret:
                break
            if frame_idx % frame_interval == 0:
                timestamp = frame_idx / video_fps
                yield timestamp, frame
            frame_idx += 1

        cap.release()

    def ocr_frame(self, frame: np.ndarray, region: str = "full") -> str:
        """对单帧进行 OCR 识别"""
        roi = extract_roi(frame, region)
        results = self.reader.readtext(roi, detail=0, paragraph=True)
        return clean_text(results)

    def process_video(
        self,
        video_path: str,
        output_path: str = None,
        fps: float = 1.0,
        region: str = "full",
        similarity_threshold: float = 0.75,
        min_duration: float = 0.5,
    ):
        """
        处理视频，生成 SRT 字幕

        参数:
            video_path: 视频文件路径
            output_path: 输出 SRT 路径（默认与视频同名）
            fps: 抽帧率（每秒帧数，越高越精确但越慢）
            region: 扫描区域 (full/bottom/top/middle)
            similarity_threshold: 相似度阈值，高于此值视为相同字幕
            min_duration: 字幕最短显示时长(秒)
        """
        if output_path is None:
            output_path = Path(video_path).with_suffix(".srt")

        # 收集所有帧的 OCR 结果
        entries = []  # [(timestamp, text), ...]
        frame_count = 0

        print("=" * 50)
        print(f"开始处理: {video_path}")
        print(f"输出文件: {output_path}")
        print(f"扫描区域: {region}")
        print("=" * 50)

        for timestamp, frame in self.extract_frames(video_path, fps):
            frame_count += 1
            text = self.ocr_frame(frame, region)

            if frame_count % 10 == 0:
                print(f"  已处理 {frame_count} 帧 ({timestamp:.1f}s)...")

            entries.append((timestamp, text))

        print(f"\n共处理 {frame_count} 帧，开始合并相似字幕...\n")

        # 合并相邻的相似/重复字幕
        subtitles = self._merge_entries(entries, similarity_threshold, min_duration)

        # 写入 SRT 文件
        self._write_srt(subtitles, output_path)

        print(f"完成! 共提取 {len(subtitles)} 条字幕")
        print(f"字幕文件已保存: {output_path}")
        return output_path

    def _merge_entries(
        self,
        entries: list[tuple[float, str]],
        threshold: float,
        min_duration: float,
    ) -> list[dict]:
        """合并相邻的相似字幕条目"""
        if not entries:
            return []

        merged = []
        current_start = entries[0][0]
        current_text = entries[0][1]

        for i in range(1, len(entries)):
            ts, text = entries[i]
            sim = text_similarity(current_text, text)

            if sim >= threshold and text:
                # 相似，延续当前字幕
                continue
            else:
                # 不同，保存当前字幕，开始新的
                if current_text.strip():
                    merged.append({
                        "start": current_start,
                        "end": ts,
                        "text": current_text,
                    })
                current_start = ts
                current_text = text

        # 保存最后一条
        if current_text.strip():
            last_ts = entries[-1][0] + 1.0  # 最后一帧额外加 1 秒
            merged.append({
                "start": current_start,
                "end": last_ts,
                "text": current_text,
            })

        # 确保最短显示时长
        for sub in merged:
            if sub["end"] - sub["start"] < min_duration:
                sub["end"] = sub["start"] + min_duration

        # 确保不重叠
        for i in range(1, len(merged)):
            if merged[i]["start"] < merged[i - 1]["end"]:
                merged[i]["start"] = merged[i - 1]["end"]

        return merged

    def _write_srt(self, subtitles: list[dict], output_path: str):
        """将字幕列表写入 SRT 文件"""
        with open(output_path, "w", encoding="utf-8") as f:
            for idx, sub in enumerate(subtitles, 1):
                start = format_timestamp(sub["start"])
                end = format_timestamp(sub["end"])
                f.write(f"{idx}\n")
                f.write(f"{start} --> {end}\n")
                f.write(f"{sub['text']}\n\n")


# --------------- CLI ---------------

def main():
    parser = argparse.ArgumentParser(
        description="从视频中提取硬字幕，生成 SRT 文件"
    )
    parser.add_argument("video", help="输入视频文件路径")
    parser.add_argument("-o", "--output", help="输出 SRT 文件路径 (默认与视频同名)")
    parser.add_argument(
        "--fps", type=float, default=1.0,
        help="抽帧率，每秒抽取几帧 (默认 1，越高越精确但越慢)"
    )
    parser.add_argument(
        "--region", choices=["full", "bottom", "top", "middle"],
        default="full",
        help="扫描区域: full=全画面, bottom=底部55%%, top=顶部35%%, middle=中间50%%"
    )
    parser.add_argument(
        "--threshold", type=float, default=0.75,
        help="字幕相似度阈值 (0~1，默认 0.75)"
    )
    parser.add_argument(
        "--no-gpu", action="store_true",
        help="禁用 GPU 加速"
    )
    parser.add_argument(
        "--lang", nargs="+", default=["ch_sim", "en"],
        help="识别语言 (默认 ch_sim en)"
    )

    args = parser.parse_args()

    if not os.path.exists(args.video):
        print(f"错误: 视频文件不存在: {args.video}")
        sys.exit(1)

    extractor = SubtitleExtractor(languages=args.lang, gpu=not args.no_gpu)
    extractor.process_video(
        video_path=args.video,
        output_path=args.output,
        fps=args.fps,
        region=args.region,
        similarity_threshold=args.threshold,
    )


if __name__ == "__main__":
    main()
