export interface Track {
  id: string;
  title: string;
  artist: string;
  duration: string;
  size: string;
  date: string;
  thumbnail: string;
}

export interface Playlist {
  id: string;
  title: string;
  trackCount: number;
  thumbnails: string[];
}

export type AppView = 'library' | 'playlists' | 'settings' | 'player';
