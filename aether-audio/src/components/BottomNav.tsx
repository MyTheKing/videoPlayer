import { Library, ListMusic, Settings } from 'lucide-react';
import { AppView } from '../types';
import { cn } from '../lib/utils';

interface BottomNavProps {
  currentView: AppView;
  onViewChange: (view: AppView) => void;
}

export default function BottomNav({ currentView, onViewChange }: BottomNavProps) {
  const tabs = [
    { id: 'library' as AppView, label: '本地库', icon: Library },
    { id: 'playlists' as AppView, label: '歌单', icon: ListMusic },
    { id: 'settings' as AppView, label: '设置', icon: Settings },
  ];

  return (
    <nav className="fixed bottom-0 left-0 right-0 z-50 glass border-t border-white/20 pb-[env(safe-area-inset-bottom,16px)]">
      <div className="flex justify-around items-center py-3 px-6">
        {tabs.map(({ id, label, icon: Icon }) => {
          const isActive = currentView === id;
          return (
            <button 
              key={id}
              onClick={() => onViewChange(id)}
              className={cn(
                "flex flex-col items-center gap-1 transition-all active:scale-90 px-4 py-1 rounded-xl",
                isActive ? "text-brand-primary" : "text-outline hover:text-on-surface-variant"
              )}
            >
              <div className={cn(
                "relative p-1 transition-all",
                isActive && "scale-110"
              )}>
                <Icon className={cn("w-6 h-6", isActive && "stroke-[2.5px]")} />
                {isActive && (
                  <div className="absolute inset-0 bg-brand-primary/10 blur-xl rounded-full" />
                )}
              </div>
              <span className={cn(
                "text-[10px] font-bold uppercase tracking-wider",
                isActive ? "opacity-100" : "opacity-70"
              )}>
                {label}
              </span>
            </button>
          );
        })}
      </div>
    </nav>
  );
}
