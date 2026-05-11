import { ChevronDown, MoreVertical, Shuffle, SkipBack, Play, Pause, SkipForward, Repeat, Video } from 'lucide-react';
import { motion, AnimatePresence } from 'motion/react';
import { Track } from '../types';
import { cn } from '../lib/utils';
import { useState } from 'react';

interface PlayerViewProps {
  track: Track;
  isPlaying: boolean;
  onClose: () => void;
  onTogglePlay: () => void;
}

export default function PlayerView({ track, isPlaying, onClose, onTogglePlay }: PlayerViewProps) {
  const [progress, setProgress] = useState(35);

  return (
    <motion.div 
      initial={{ y: '100%' }}
      animate={{ y: 0 }}
      exit={{ y: '100%' }}
      transition={{ type: 'spring', damping: 25, stiffness: 200 }}
      className="fixed inset-0 z-50 bg-surface flex flex-col"
    >
      <header className="px-6 pt-12 pb-6 flex justify-between items-center">
        <button onClick={onClose} className="p-2 text-outline hover:bg-surface-container rounded-full transition-colors">
          <ChevronDown className="w-6 h-6" />
        </button>
        <div className="text-center">
          <p className="text-[10px] uppercase tracking-widest font-bold text-outline">正在播放</p>
          <p className="text-xs font-semibold text-on-surface">视频合集</p>
        </div>
        <button className="p-2 text-outline hover:bg-surface-container rounded-full transition-colors">
          <MoreVertical className="w-6 h-6" />
        </button>
      </header>

      <main className="flex-1 flex flex-col items-center justify-center px-6 gap-12">
        {/* Disc Wrapper */}
        <div className="relative">
          <motion.div 
            animate={{ rotate: isPlaying ? 360 : 0 }}
            transition={{ duration: 20, repeat: Infinity, ease: 'linear' }}
            className={cn(
              "w-64 h-64 md:w-72 md:h-72 rounded-full shadow-2xl overflow-hidden border-8 border-white",
              !isPlaying && "animate-none"
            )}
          >
            <img 
              src={track.thumbnail} 
              alt={track.title} 
              className="w-full h-full object-cover"
              referrerPolicy="no-referrer"
            />
          </motion.div>
          {/* Center Hole */}
          <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-10 h-10 bg-surface rounded-full shadow-inner border border-outline/20" />
        </div>

        <div className="text-center w-full px-4">
          <h1 className="text-2xl font-bold text-on-surface truncate">{track.title}</h1>
          <p className="text-outline font-medium mt-1">{track.artist}</p>
        </div>

        <div className="w-full space-y-3">
          <div className="relative group px-1">
            <input 
              type="range" 
              value={progress}
              onChange={(e) => setProgress(Number(e.target.value))}
              className="w-full h-1.5 bg-surface-container rounded-full appearance-none cursor-pointer accent-brand-primary"
            />
          </div>
          <div className="flex justify-between text-[10px] font-bold text-outline tabular-nums">
            <span>01:14</span>
            <span>{track.duration}</span>
          </div>
        </div>

        <div className="w-full flex items-center justify-between px-2">
          <button className="text-outline hover:text-brand-primary transition-colors">
            <Shuffle className="w-5 h-5" />
          </button>
          
          <div className="flex items-center gap-8">
            <button className="text-on-surface hover:text-brand-primary transition-all active:scale-90">
              <SkipBack className="w-8 h-8 fill-current" />
            </button>
            <button 
              onClick={onTogglePlay}
              className="w-16 h-16 bg-brand-primary-container text-white rounded-full shadow-xl shadow-brand-primary/30 flex items-center justify-center transition-all active:scale-95 hover:bg-brand-primary"
            >
              {isPlaying ? <Pause className="w-8 h-8 fill-current" /> : <Play className="w-8 h-8 translate-x-0.5 fill-current" />}
            </button>
            <button className="text-on-surface hover:text-brand-primary transition-all active:scale-90">
              <SkipForward className="w-8 h-8 fill-current" />
            </button>
          </div>

          <button className="text-brand-primary">
            <Repeat className="w-5 h-5" />
          </button>
        </div>
      </main>

      <footer className="pt-6 pb-12 flex justify-center">
        <button className="flex items-center gap-2 px-8 py-3 bg-white border border-surface-container rounded-full shadow-sm hover:shadow-md transition-all text-on-surface-variant font-semibold group active:scale-95">
          <Video className="w-5 h-5 text-brand-primary group-hover:scale-110 transition-transform" />
          <span>切换到视频模式</span>
        </button>
      </footer>
    </motion.div>
  );
}
