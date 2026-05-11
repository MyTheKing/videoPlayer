/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import { useState } from 'react';
import { AnimatePresence, motion } from 'motion/react';
import { AppView, Track } from './types';
import { TRACKS } from './constants';
import PlaylistsView from './components/PlaylistsView';
import LocalLibraryView from './components/LocalLibraryView';
import PlayerView from './components/PlayerView';
import BottomNav from './components/BottomNav';
import MiniPlayer from './components/MiniPlayer';
import BottomSheet from './components/BottomSheet';

export default function App() {
  const [currentView, setCurrentView] = useState<AppView>('playlists');
  const [currentTrack, setCurrentTrack] = useState<Track | null>(TRACKS[0]);
  const [isPlaying, setIsPlaying] = useState(false);
  const [isPlayerOpen, setIsPlayerOpen] = useState(false);
  const [activeActionTrack, setActiveActionTrack] = useState<Track | null>(null);

  const togglePlay = () => setIsPlaying(!isPlaying);

  const renderView = () => {
    switch (currentView) {
      case 'playlists':
        return <PlaylistsView key="playlists" />;
      case 'library':
        return <LocalLibraryView key="library" onTrackAction={setActiveActionTrack} />;
      case 'settings':
        return (
          <div key="settings" className="px-6 pt-12">
            <h1 className="text-3xl font-bold mb-8">设置</h1>
            <div className="space-y-6">
              <div className="p-4 bg-surface-container rounded-2xl">
                <p className="text-sm font-semibold opacity-60">版本</p>
                <p className="text-lg font-bold">1.0.42 (Beta)</p>
              </div>
              <div className="p-4 bg-surface-container rounded-2xl">
                <p className="text-sm font-semibold opacity-60">存储占用</p>
                <p className="text-lg font-bold">1.2 GB / 256 GB</p>
              </div>
              <button className="w-full py-4 text-brand-primary font-bold">清空搜索记录</button>
            </div>
          </div>
        );
      default:
        return null;
    }
  };

  return (
    <div className="min-h-screen bg-surface flex flex-col max-w-md mx-auto relative shadow-2xl overflow-hidden">
      {/* Simulation of a notch area */}
      <div className="h-10 shrink-0" />
      
      <main className="flex-1 overflow-y-auto no-scrollbar">
        <AnimatePresence mode="wait">
          {renderView()}
        </AnimatePresence>
      </main>

      {currentTrack && !isPlayerOpen && (
        <MiniPlayer 
          track={currentTrack} 
          isPlaying={isPlaying} 
          onTogglePlay={togglePlay}
          onClick={() => setIsPlayerOpen(true)}
        />
      )}

      <BottomNav 
        currentView={currentView} 
        onViewChange={(view) => {
          setCurrentView(view);
          setIsPlayerOpen(false);
        }} 
      />

      <AnimatePresence>
        {isPlayerOpen && currentTrack && (
          <PlayerView 
            track={currentTrack} 
            isPlaying={isPlaying} 
            onClose={() => setIsPlayerOpen(false)} 
            onTogglePlay={togglePlay}
          />
        )}
      </AnimatePresence>

      <BottomSheet 
        track={activeActionTrack} 
        onClose={() => setActiveActionTrack(null)} 
      />
    </div>
  );
}
