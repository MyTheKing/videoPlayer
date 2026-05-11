import { Search, RefreshCw, MoreVertical } from 'lucide-react';
import { TRACKS } from '../constants';
import { motion } from 'motion/react';
import { Track } from '../types';

interface LocalLibraryViewProps {
  onTrackAction: (track: Track) => void;
}

export default function LocalLibraryView({ onTrackAction }: LocalLibraryViewProps) {
  return (
    <div className="px-5 pb-32">
      <header className="pt-8 pb-6 border-b border-surface-container">
        <div className="flex justify-between items-center mb-6">
          <h1 className="text-2xl font-bold tracking-tight text-on-surface">本地库</h1>
          <button className="bg-brand-primary hover:bg-brand-primary-container text-white px-5 py-2 rounded-full text-sm font-semibold transition-all shadow-sm active:scale-95 flex items-center gap-2">
            <RefreshCw className="w-4 h-4" />
            扫描
          </button>
        </div>
        
        <div className="relative">
          <Search className="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-outline pointer-events-none" />
          <input 
            type="text" 
            placeholder="搜索视频..."
            className="w-full pl-11 pr-4 py-3 bg-surface-container border-none rounded-2xl text-sm placeholder:text-outline focus:ring-2 focus:ring-brand-primary transition-all"
          />
        </div>
      </header>

      <div className="my-6">
        <div className="flex items-center justify-between mb-4">
          <span className="text-[10px] font-bold text-outline uppercase tracking-wider">
            最近找到 ({TRACKS.length})
          </span>
          <button className="text-brand-primary text-xs font-semibold hover:underline">网格视图</button>
        </div>

        <div className="space-y-2">
          {TRACKS.map((track) => (
            <motion.div 
              key={track.id}
              className="flex items-center gap-4 p-2 rounded-2xl hover:bg-surface-container-low transition-colors cursor-pointer group"
              whileTap={{ scale: 0.98 }}
            >
              <div className="relative shrink-0 w-28 h-16 bg-surface-container rounded-lg overflow-hidden shadow-sm">
                <img 
                  src={track.thumbnail} 
                  alt={track.title} 
                  className="w-full h-full object-cover"
                  referrerPolicy="no-referrer"
                />
                <span className="absolute bottom-1 right-1 bg-black/70 text-white text-[10px] px-1.5 py-0.5 rounded font-medium">
                  {track.duration}
                </span>
              </div>
              <div className="flex-1 min-w-0">
                <h3 className="text-sm font-semibold text-on-surface truncate group-hover:text-brand-primary transition-colors">
                  {track.title}
                </h3>
                <div className="flex items-center text-xs text-outline mt-1 gap-2">
                  <span>{track.size}</span>
                  <span>•</span>
                  <span>{track.date}</span>
                </div>
              </div>
              <button 
                onClick={(e) => {
                  e.stopPropagation();
                  onTrackAction(track);
                }}
                className="p-2 text-outline hover:text-on-surface transition-colors"
              >
                <MoreVertical className="w-5 h-5" />
              </button>
            </motion.div>
          ))}
        </div>
      </div>

      <div className="py-12 flex flex-col items-center justify-center opacity-40">
        <div className="w-8 h-8 border-2 border-brand-primary border-t-transparent rounded-full animate-spin mb-3"></div>
        <p className="text-xs font-semibold">正在检查更多文件...</p>
      </div>
    </div>
  );
}
