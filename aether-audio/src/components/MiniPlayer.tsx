import { Play, SkipForward, Pause, ListMusic } from 'lucide-react';
import { Track } from '../types';
import { motion } from 'motion/react';
import { cn } from '../lib/utils';

interface MiniPlayerProps {
  track: Track;
  isPlaying: boolean;
  onTogglePlay: () => void;
  onClick: () => void;
}

export default function MiniPlayer({ track, isPlaying, onTogglePlay, onClick }: MiniPlayerProps) {
  return (
    <motion.div 
      initial={{ y: 100, opacity: 0 }}
      animate={{ y: 0, opacity: 1 }}
      className="fixed bottom-[84px] left-4 right-4 z-40"
    >
      <div 
        onClick={onClick}
        className="glass rounded-2xl p-2 flex items-center justify-between gap-3 cursor-pointer group hover:bg-white/90 transition-colors"
      >
        <div className="flex items-center gap-3 flex-1 min-w-0">
          <div className="w-10 h-10 rounded-lg overflow-hidden bg-surface-container shrink-0 shadow-sm border border-white/40">
            <img 
              src={track.thumbnail} 
              alt={track.title} 
              className="w-full h-full object-cover"
              referrerPolicy="no-referrer"
            />
          </div>
          <div className="flex flex-col min-w-0">
            <span className="text-sm font-semibold text-on-surface truncate group-hover:text-brand-primary transition-colors">
              {track.title}
            </span>
            <span className="text-[10px] text-brand-primary font-bold tracking-wider uppercase">
              正在播放
            </span>
          </div>
        </div>

        <div className="flex items-center gap-1 pr-1">
          <button 
            onClick={(e) => {
              e.stopPropagation();
              onTogglePlay();
            }}
            className="p-2 text-brand-primary hover:scale-110 transition-all active:scale-90"
          >
            {isPlaying ? <Pause className="w-7 h-7 fill-current" /> : <Play className="w-7 h-7 fill-current" />}
          </button>
          <button 
            onClick={(e) => e.stopPropagation()}
            className="p-2 text-on-surface-variant hover:text-brand-primary transition-colors active:scale-90"
          >
            <SkipForward className="w-6 h-6 fill-current" />
          </button>
          <button 
            onClick={(e) => e.stopPropagation()}
            className="p-2 text-outline hover:text-on-surface transition-colors"
          >
            <ListMusic className="w-5 h-5" />
          </button>
        </div>
      </div>
    </motion.div>
  );
}
