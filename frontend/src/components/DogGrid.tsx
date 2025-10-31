import React, { useEffect, useState } from 'react';
import axios from 'axios';
import './DogGrid.css';

// Constants
const GRID_SIZE = 42; // 6 rows x 7 columns
const ROWS = 6;
const COLUMNS = 7;

// TypeScript interfaces
interface DogImage {
  url: string;
  id: string;
}

interface HoverState {
  imageId: string | null;
  url: string | null;
}

interface OverlayState {
  isVisible: boolean;
  imageUrl: string | null;
}

// Animated dots component for loading
const AnimatedDots = () => (
  <span className="dots">
    <span className="dot dot-1">.</span>
    <span className="dot dot-2">.</span>
    <span className="dot dot-3">.</span>
  </span>
);

// Main component
const DogGrid = () => {
  const [isReady, setIsReady] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [images, setImages] = useState<DogImage[]>([]);
  const [hovered, setHovered] = useState<HoverState>({ 
    imageId: null, 
    url: null 
  });
  const [overlay, setOverlay] = useState<OverlayState>({
    isVisible: false,
    imageUrl: null
  });
  const [mousePos, setMousePos] = useState<{ x: number; y: number }>({ x: 0, y: 0 });

  // Load images from backend
  const loadImages = async (): Promise<void> => {
    try {
      setIsLoading(true);
      const { data: urls } = await axios.get(`/api/dogs?size=${GRID_SIZE}`);
      
      // Preload images
      await Promise.all(
        urls.map((url: string) => new Promise<void>((resolve) => {
          const img = new window.Image();
          img.onload = () => resolve();
          img.onerror = () => resolve(); // Continue even if image fails to load
          img.src = url;
        }))
      );

      // Create image objects with unique IDs
      const newImages: DogImage[] = urls.map((url: string, index: number) => ({
        url,
        id: `dog-${Date.now()}-${index}`
      }));

      setImages(newImages);
      setIsReady(true);
    } catch (error) {
      console.error('Failed to load images:', error);
      setIsReady(true); // Show component even if loading fails
    } finally {
      setIsLoading(false);
    }
  };

  // Load initial images
  useEffect(() => {
    loadImages();
  }, []);

  // Handle shuffle button click
  const handleShuffle = (): void => {
    loadImages();
  };

  // Handle image hover
  const handleImageHover = (e: React.MouseEvent, imageId: string, url: string): void => {
    setHovered({ imageId, url });
    setMousePos({ x: e.clientX, y: e.clientY });
  };

  // Handle mouse leave
  const handleImageUnhover = (): void => {
    setHovered({ imageId: null, url: null });
  };

  // Handle image click to show overlay
  const handleImageClick = (url: string): void => {
    setOverlay({ isVisible: true, imageUrl: url });
  };

  // Handle overlay close
  const handleOverlayClose = (): void => {
    setOverlay({ isVisible: false, imageUrl: null });
  };

  // Handle overlay background click
  const handleOverlayBackgroundClick = (e: React.MouseEvent<HTMLDivElement>) => {
    if (e.target === e.currentTarget) {
      handleOverlayClose();
    }
  };

  return (
    <div className="dog-grid-wrapper">
      {(!isReady || isLoading) && (
        <div className="loading-overlay">
          <div>
            <div style={{ fontWeight: 'bold', fontSize: '1.7rem' }}>
              {isLoading ? 'Načítám nové pejsky' : 'Připravuji pejsky'}
              <AnimatedDots />
            </div>
            <div style={{ marginTop: '1.2rem', color: '#666', fontSize: '1.3rem' }}>
              Chvilku strpení, prosím
            </div>
          </div>
        </div>
      )}
      
      <div className="header">
        <h1 className="title">Adoptuj mě, prosím!</h1>
      </div>
      
      {/* Loading indicator above grid */}
      {isLoading && (
        <div className="loading-indicator">
          <div className="loading-text">Načítám nové pejsky</div>
          <AnimatedDots />
        </div>
      )}
      
      <div className="dog-grid">
        {images.map((image) => (
          <div
            key={image.id}
            className={`dog-grid-item ${
              hovered.imageId === image.id ? 'dog-grid-item-hovered' : ''
            }`}
            onMouseEnter={(e) => handleImageHover(e, image.id, image.url)}
            onMouseMove={(e) => setMousePos({ x: e.clientX, y: e.clientY })}
            onMouseLeave={handleImageUnhover}
            onClick={() => handleImageClick(image.url)}
          >
            <img
              src={image.url}
              alt="Dog"
              className="dog-image"
              draggable={false}
            />
          </div>
        ))}
      </div>
      
      {/* Hover preview */}
      {hovered.url && (
        <div
          className="dog-hover-preview"
          style={{
            top: mousePos.y,
            // offset preview slightly to the right of the cursor and clamp within viewport
            left: Math.min(
              (typeof window !== 'undefined' ? window.innerWidth : 1920) - 20,
              mousePos.x + 24
            ),
            transform: 'translate(0, -50%)'
          }}
        >
          <img src={hovered.url} alt="Preview" className="dog-hover-image" />
        </div>
      )}
      
      {/* Full overlay */}
      {overlay.isVisible && overlay.imageUrl && (
        <div 
          className="dog-overlay" 
          onClick={handleOverlayBackgroundClick}
        >
          <div className="dog-overlay-content">
            <button 
              className="close-button" 
              onClick={handleOverlayClose}
              aria-label="Close"
            >
              ×
            </button>
            <img 
              src={overlay.imageUrl} 
              alt="Dog" 
              className="dog-overlay-image" 
            />
          </div>
        </div>
      )}
      
      {/* Floating shuffle button */}
      <button 
        className="floating-shuffle-button" 
        onClick={handleShuffle}
        disabled={isLoading}
        title="Zamíchat pejsky"
      >
        <svg width="24" height="24" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
          <path d="M10.59 9.17L5.41 4L4 5.41L9.17 10.58L10.59 9.17ZM14.5 4L16.54 6.04L4 18.59L5.41 20L17.96 7.46L20 9.5V4H14.5ZM14.83 13.41L13.42 14.82L16.55 17.95L14.5 20H20V14.5L17.96 16.54L14.83 13.41Z" fill="currentColor"/>
        </svg>
      </button>
    </div>
  );
};

export default DogGrid;