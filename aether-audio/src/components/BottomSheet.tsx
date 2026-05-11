import { motion, AnimatePresence } from 'motion/react';
import { X, Plus, Heart, Ban } from 'lucide-react';
import { Track } from '../types';
import { PLAYLISTS } from '../constants';

interface BottomSheetProps {
  track: Track | null;
  onClose: () => void;
}

export default function BottomSheet({ track, onClose }: BottomSheetProps) {
  if (!track) return null;

  return (
    <AnimatePresence>
      <motion.div 
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        exit={{ opacity: 0 }}
        onClick={onClose}
        className="fixed inset-0 z-[60] bg-on-surface/40 backdrop-blur-[2px]"
      />
      <motion.div 
        initial={{ y: '100%' }}
        animate={{ y: 0 }}
        exit={{ y: '100%' }}
        transition={{ type: 'spring', damping: 25, stiffness: 200 }}
        className="fixed bottom-0 left-0 right-0 z-[70] bg-white rounded-t-[32px] shadow-2xl safe-p-bottom"
      >
        <div className="flex justify-center py-3">
          <div className="w-12 h-1.5 bg-surface-container-highest rounded-full" />
        </div>
        
        <div className="px-6 pb-12">
          <div className="flex justify-between items-start mb-6">
            <div>
              <h2 className="text-lg font-bold text-on-surface">更多选项</h2>
              <p className="text-xs text-outline truncate max-w-[240px]">{track.title}</p>
            </div>
            <button onClick={onClose} className="p-2 text-outline bg-surface-container rounded-full">
              <X className="w-4 h-4" />
            </button>
          </div>

          <div className="space-y-1">
            <div className="py-2">
              <div className="flex items-center gap-3 text-on-surface-variant font-semibold mb-3">
                <Plus className="w-5 h-5 text-brand-primary" />
                <span>添加到歌单</span>
              </div>
              <div className="ml-8 space-y-3 border-l-2 border-surface-container pl-4">
                {PLAYLISTS.slice(0, 2).map((pl) => (
                  <button 
                    key={pl.id}
                    className="flex items-center justify-between w-full text-sm font-medium text-outline hover:text-brand-primary transition-colors"
                  >
                    <span>{pl.title}</span>
                    <div className="w-2 h-2 rounded-full bg-surface-container-highest" />
                  </button>
                ))}
              </div>
            </div>

            <hr className="my-2 border-surface-container-low" />

            <button className="w-full flex items-center gap-3 py-4 text-on-surface-variant font-semibold hover:bg-surface-container-low rounded-xl transition-colors">
              <Heart className="w-5 h-5 text-rose-500" />
              <span>添加到我喜欢的</span>
            </button>

            <button className="w-full flex items-center gap-3 py-4 text-on-surface-variant font-semibold hover:bg-surface-container-low rounded-xl transition-colors">
              <Ban className="w-5 h-5 text-outline" />
              <span>忽略</span>
            </button>

            <button className="w-full flex items-center gap-3 py-4 text-brand-primary font-bold text-xs uppercase tracking-widest mt-2 hover:bg-brand-primary/5 rounded-xl transition-colors">
               <div className="w-5 h-5 flex items-center justify-center bg-brand-primary/10 rounded-full">
                <Plus className="w-3.5 h-3.5" />
              </div>
              <span>新建歌单</span>
            </button>
          </div>
        </div>
      </motion.div>
    </AnimatePresence>
  );
}
