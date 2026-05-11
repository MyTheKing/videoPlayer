import { Search, Plus } from 'lucide-react';
import { PLAYLISTS } from '../constants';
import { motion } from 'motion/react';

export default function PlaylistsView() {
  return (
    <div className="px-5 pb-32">
      <header className="py-8 flex justify-between items-center">
        <h1 className="text-3xl font-bold tracking-tight">我的歌单</h1>
        <button className="p-2 text-outline hover:text-brand-primary transition-colors">
          <Search className="w-6 h-6" />
        </button>
      </header>

      <div className="grid grid-cols-2 gap-x-4 gap-y-8">
        {PLAYLISTS.map((playlist) => (
          <motion.article 
            key={playlist.id} 
            className="flex flex-col gap-2 group cursor-pointer"
            whileTap={{ scale: 0.98 }}
          >
            <div className="relative aspect-square rounded-2xl overflow-hidden bg-surface-container grid grid-cols-2 grid-rows-2 gap-[1px]">
              {playlist.thumbnails.map((thumb, idx) => (
                <img 
                  key={idx} 
                  src={thumb} 
                  alt={`${playlist.title} cover ${idx + 1}`} 
                  className="w-full h-full object-cover"
                  referrerPolicy="no-referrer"
                />
              ))}
            </div>
            <div>
              <h2 className="font-semibold text-base truncate group-hover:text-brand-primary transition-colors">
                {playlist.title}
              </h2>
              <p className="text-sm text-outline">
                {playlist.trackCount} 首曲目
              </p>
            </div>
          </motion.article>
        ))}
      </div>

      <button className="fixed bottom-32 right-6 w-14 h-14 bg-brand-primary-container text-white rounded-full shadow-lg shadow-brand-primary/20 flex items-center justify-center hover:bg-brand-primary active:scale-95 transition-all z-20">
        <Plus className="w-8 h-8" />
      </button>
    </div>
  );
}
