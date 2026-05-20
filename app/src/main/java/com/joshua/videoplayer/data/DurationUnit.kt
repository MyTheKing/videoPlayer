package com.joshua.videoplayer.data

/**
 * 时长单位枚举，用于时长过滤器和定时器的内部逻辑。
 * UI 层通过 stringResource() 显示本地化文本。
 */
enum class DurationUnit(val multiplier: Long) {
    SECONDS(1000),
    MINUTES(60 * 1000),
    HOURS(60 * 60 * 1000);

    companion object {
        /**
         * 从显示文本解析时长单位（兼容旧的中文单位字符串）
         */
        fun fromDisplayText(text: String, localizedSeconds: String, localizedMinutes: String, localizedHours: String): DurationUnit {
            return when (text) {
                localizedSeconds, "秒" -> SECONDS
                localizedMinutes, "分" -> MINUTES
                localizedHours, "时" -> HOURS
                else -> SECONDS
            }
        }

        /**
         * 将毫秒转换为 (数值, 单位) 对
         */
        fun msToDisplayValue(ms: Long, defaultUnit: DurationUnit = SECONDS): Pair<String, DurationUnit> {
            return when {
                ms <= 0 -> "" to defaultUnit
                ms % (60 * 60 * 1000) == 0L -> (ms / (60 * 60 * 1000)).toString() to HOURS
                ms % (60 * 1000) == 0L -> (ms / (60 * 1000)).toString() to MINUTES
                else -> (ms / 1000).toString() to SECONDS
            }
        }
    }
}
