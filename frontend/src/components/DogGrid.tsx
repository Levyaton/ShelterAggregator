import React, { useEffect, useState } from 'react';
import axios from 'axios';
import './DogGrid.css';
import LogoAdoptujMe from './LogoAdoptujMe.png';

// Constants
const GRID_SIZE = 42; // 6 rows x 7 columns
const ROWS = 6;
const COLUMNS = 7;

// TypeScript interfaces
interface DogImage {
  id: string | number;
  url: string;
  imageUrls?: string[]; // All image URLs for carousel
  originalUrl?: string; // Store original URL if we receive a converted data URI
  name?: string;
  description?: string;
  breedGuess?: string;
  sex?: string;
  estimatedAgeInYears?: number;
  currentWeight?: number;
  estimatedFinalWeightMin?: number;
  estimatedFinalWeightMax?: number;
  dogAddress?: string;
  shelterUrl?: string;
}

interface HoverState {
  imageId: string | number | null;
  url: string | null;
}

interface OverlayState {
  isVisible: boolean;
  dog: DogImage | null;
  selectedImageIndex: number; // Track which image is currently displayed
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
    dog: null,
    selectedImageIndex: 0
  });
  const [previewAnchor, setPreviewAnchor] = useState<{ x: number; y: number }>({ x: 0, y: 0 });
  const [loadStatus, setLoadStatus] = useState<Record<string, { loaded: boolean; attempts: number; failed: boolean; isPlaceholder: boolean; originalUrl?: string; lastError?: string; errorType?: string }>>({});

  const sanitizeMeta = (text?: string): string => {
    if (!text) return '';
    return text.replace(/^\s*,\s*/, '');
  };

  // Placeholder for missing/broken images (white bg with gray X)
  const PLACEHOLDER_IMAGE =
    'data:image/svg+xml;utf8,' +
    encodeURIComponent(
      `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 200 200">
         <rect width="100%" height="100%" fill="white"/>
         <line x1="10" y1="10" x2="190" y2="190" stroke="#bbb" stroke-width="8"/>
         <line x1="190" y1="10" x2="10" y2="190" stroke="#bbb" stroke-width="8"/>
       </svg>`
    );

  // Validate if a URL is a valid image URL
  const isValidImageUrl = (url: string): boolean => {
    if (!url || typeof url !== 'string') return false;
    // Accept regular HTTP/HTTPS URLs (images are now passed through directly)
    if (url.startsWith('http://') || url.startsWith('https://')) return true;
    // Accept data URIs that are images (in case we still have some)
    if (url.startsWith('data:image/')) return true;
    // Reject data URIs that are HTML pages (shouldn't happen now, but keep for safety)
    if (url.startsWith('data:text/html')) return false;
    // Reject anything else
    return false;
  };

  // Load images from backend
  const loadImages = async (): Promise<void> => {
    try {
      setIsLoading(true);
      const { data } = await axios.get(`/api/dogs?size=${GRID_SIZE}`);
      
      // Preload images (or placeholders) - skip HTML error pages
      await Promise.all(
        (Array.isArray(data) ? data : [])
          .map((raw: any, index: number) => {
            const url = typeof raw === 'string' ? raw : raw?.url;
            return url ? url : PLACEHOLDER_IMAGE;
          })
          .filter((u: string | null): u is string => Boolean(u))
          .filter((url: string) => isValidImageUrl(url)) // Filter out HTML error pages
          .map((url: string) => new Promise<void>((resolve) => {
            const img = new window.Image();
            img.onload = () => resolve();
            img.onerror = () => resolve(); // Continue even if image fails to load
            img.src = url;
          }))
      );

      // Normalize server payload to DogImage[]
      let newImages: DogImage[] = (Array.isArray(data) ? data : [])
        .map((raw: any, index: number): DogImage | null => {
          let imageUrl: string;
          
          if (typeof raw === 'string') {
            imageUrl = raw;
          } else if (raw && typeof raw.url === 'string') {
            imageUrl = raw.url;
          } else {
            // No usable data; still return a placeholder item to represent the dog entity
            return {
              id: `dog-${Date.now()}-${index}`,
              url: PLACEHOLDER_IMAGE,
            } as DogImage;
          }

          // Validate and sanitize the URL
          if (!imageUrl || !isValidImageUrl(imageUrl)) {
            // If it's a data URI HTML page, we can't recover the original URL, but log it clearly
            const isHtmlDataUri = imageUrl?.startsWith('data:text/html');
            const logMessage = isHtmlDataUri 
              ? `[Image Load] Received HTML error page as data URI for dog ${raw?.id ?? index} (original URL lost - check proxy/backend logs)`
              : `[Image Load] Invalid image URL for dog ${raw?.id ?? index}: ${imageUrl?.substring(0, 100)}...`;
            
            console.warn(logMessage, {
              dogId: raw?.id ?? index,
              receivedUrl: imageUrl?.substring(0, 200),
              isHtmlDataUri: isHtmlDataUri,
              note: isHtmlDataUri ? 'This suggests the proxy or backend converted an HTML error page to a data URI. Check server logs for the original URL.' : 'Invalid URL format'
            });
            imageUrl = PLACEHOLDER_IMAGE;
          }

          if (typeof raw === 'string') {
            return { id: `dog-${Date.now()}-${index}`, url: imageUrl };
          }
          
          return {
            id: raw.id ?? `dog-${Date.now()}-${index}`,
            url: imageUrl,
            imageUrls: raw.imageUrls || [imageUrl], // Include all image URLs if available
            name: raw.name,
            description: raw.description,
            breedGuess: raw.breedGuess,
            sex: raw.sex,
            estimatedAgeInYears: raw.estimatedAgeInYears,
            currentWeight: raw.currentWeight,
            estimatedFinalWeightMin: raw.estimatedFinalWeightMin,
            estimatedFinalWeightMax: raw.estimatedFinalWeightMax,
            dogAddress: raw.dogAddress,
            shelterUrl: raw.shelterUrl,
          };
        })
        .filter((d: DogImage | null): d is DogImage => Boolean(d));

      // Dedupe by id if present, otherwise by url
      const seen = new Set<string>();
      newImages = newImages.filter((img) => {
        const key = (img.id !== undefined && img.id !== null) ? `id:${String(img.id)}` : `url:${img.url}`;
        if (seen.has(key)) return false;
        seen.add(key);
        return true;
      });

      setImages(newImages);
      // initialize per-image load status
      const initial: Record<string, { loaded: boolean; attempts: number; failed: boolean; isPlaceholder: boolean; originalUrl?: string; lastError?: string; errorType?: string }> = {};
      newImages.forEach(img => {
        const isPh = img.url === PLACEHOLDER_IMAGE;
        initial[String(img.id)] = { 
          loaded: isPh, 
          attempts: 0, 
          failed: false, 
          isPlaceholder: isPh,
          originalUrl: img.url // Store original URL for error reporting
        };
      });
      setLoadStatus(initial);
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

  // Per-image retry loader and 90% readiness logic
  useEffect(() => {
    const maxAttempts = 10;
    const timers: number[] = [];

    images.forEach((img) => {
      const key = String(img.id);
      const st = loadStatus[key];
      if (!st || st.isPlaceholder || st.loaded || st.failed) return;

      const attemptLoad = () => {
        const i = new window.Image();
        const originalUrl = img.url; // Capture original URL before any potential conversion
        i.onload = () => setLoadStatus(prev => ({ ...prev, [key]: { ...prev[key], loaded: true } }));
        i.onerror = () => {
          const errorType = 'image-load-error';
          const urlToLog = originalUrl; // Always use the original URL from the API, not the converted one
          const errorMessage = `Failed to load image: ${urlToLog.substring(0, 100)}${urlToLog.length > 100 ? '...' : ''}`;
          setLoadStatus(prev => {
            const cur = prev[key];
            const nextAttempts = (cur?.attempts ?? 0) + 1;
            const failed = nextAttempts >= maxAttempts;
            
            // Log the failure with original URL
            if (failed) {
              console.error(`[Image Load] Failed after ${maxAttempts} attempts for dog ${img.id}: ${urlToLog}`, {
                attempts: nextAttempts,
                errorType,
                errorMessage,
                dogId: img.id,
                originalImageUrl: urlToLog
              });
            } else {
              console.warn(`[Image Load] Attempt ${nextAttempts}/${maxAttempts} failed for dog ${img.id}: ${urlToLog}`, {
                errorType,
                errorMessage,
                dogId: img.id,
                originalImageUrl: urlToLog
              });
            }
            
            return { 
              ...prev, 
              [key]: { 
                ...cur, 
                attempts: nextAttempts, 
                failed,
                originalUrl: urlToLog, // Ensure original URL is stored
                lastError: errorMessage,
                errorType
              } 
            };
          });
        };
        i.src = img.url;
      };

      // first attempt or retry with backoff
      const delay = st.attempts === 0 ? 0 : 500 + Math.min(1500, st.attempts * 300);
      const t = window.setTimeout(attemptLoad, delay);
      timers.push(t);
    });

    // readiness threshold
    if (images.length > 0) {
      const loadedCount = images.filter(im => {
        const st = loadStatus[String(im.id)];
        return st && (st.loaded || st.isPlaceholder || st.failed);
      }).length;
      if (loadedCount / images.length >= 0.9) setIsReady(true);
    }

    return () => timers.forEach(t => window.clearTimeout(t));
  }, [images, loadStatus]);

  // Handle shuffle button click
  const handleShuffle = (): void => {
    loadImages();
  };

  // Handle image hover
  const handleImageHover = (e: React.MouseEvent, imageId: string | number, url: string): void => {
    setHovered({ imageId, url });
    const rect = (e.currentTarget as HTMLElement).getBoundingClientRect();
    setPreviewAnchor({ x: rect.left + rect.width / 2, y: rect.top + rect.height / 2 });
  };

  // Handle mouse leave
  const handleImageUnhover = (): void => {
    setHovered({ imageId: null, url: null });
  };

  // Handle image click to show overlay
  const handleImageClick = (dog: DogImage): void => {
    setOverlay({ isVisible: true, dog, selectedImageIndex: 0 });
  };

  // Handle overlay close
  const handleOverlayClose = (): void => {
    setOverlay({ isVisible: false, dog: null, selectedImageIndex: 0 });
  };

  // Handle thumbnail click to change main image
  const handleThumbnailClick = (index: number): void => {
    if (overlay.dog) {
      setOverlay({ ...overlay, selectedImageIndex: index });
    }
  };

  // Keyboard event handler for arrow keys to navigate images
  useEffect(() => {
    if (!overlay.isVisible || !overlay.dog) return;

    const handleKeyDown = (event: KeyboardEvent): void => {
      // Only handle if overlay is visible and not typing in an input
      if (event.target instanceof HTMLInputElement || event.target instanceof HTMLTextAreaElement) {
        return;
      }

      const allImageUrls = overlay.dog.imageUrls || [overlay.dog.url];
      if (allImageUrls.length <= 1) return; // No navigation needed if only one image

      if (event.key === 'ArrowLeft') {
        event.preventDefault();
        // Navigate to previous image (wrap to end if at start)
        const newIndex = overlay.selectedImageIndex === 0 
          ? allImageUrls.length - 1 
          : overlay.selectedImageIndex - 1;
        setOverlay({ ...overlay, selectedImageIndex: newIndex });
      } else if (event.key === 'ArrowRight') {
        event.preventDefault();
        // Navigate to next image (wrap to start if at end)
        const newIndex = (overlay.selectedImageIndex + 1) % allImageUrls.length;
        setOverlay({ ...overlay, selectedImageIndex: newIndex });
      }
    };

    window.addEventListener('keydown', handleKeyDown);
    return () => {
      window.removeEventListener('keydown', handleKeyDown);
    };
  }, [overlay.isVisible, overlay.dog, overlay.selectedImageIndex]);

  // Handle navigation to previous dog
  const handlePreviousDog = (): void => {
    if (!overlay.dog || images.length === 0) return;
    const currentIndex = images.findIndex(img => String(img.id) === String(overlay.dog!.id));
    if (currentIndex === -1) return;
    const previousIndex = currentIndex === 0 ? images.length - 1 : currentIndex - 1;
    setOverlay({ isVisible: true, dog: images[previousIndex], selectedImageIndex: 0 });
  };

  // Handle navigation to next dog
  const handleNextDog = (): void => {
    if (!overlay.dog || images.length === 0) return;
    const currentIndex = images.findIndex(img => String(img.id) === String(overlay.dog!.id));
    if (currentIndex === -1) return;
    const nextIndex = currentIndex === images.length - 1 ? 0 : currentIndex + 1;
    setOverlay({ isVisible: true, dog: images[nextIndex], selectedImageIndex: 0 });
  };

  // Handle overlay background click
  const handleOverlayBackgroundClick = (e: React.MouseEvent<HTMLDivElement>) => {
    if (e.target === e.currentTarget) {
      handleOverlayClose();
    }
  };

  // Temporary animal labeling (future-proof for cats)
  const animalAccusative = 'Pejska'; // e.g., swap to 'Kočičky' for cats
  const animalAdjective = 'Psí'; // e.g., swap to 'Kočičí' for cats

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
      
      <div className={`header ${overlay.isVisible ? 'header-blurred' : ''}`}>
        <img src={LogoAdoptujMe} alt="Adoptuj mě" className="logo" draggable={false} />
        <h1 className="title">Adoptuj mě, prosím!</h1>
        <div className="header-spacer" />
      </div>
      
      {/* Navigation arrows - positioned above overlay */}
      {overlay.isVisible && overlay.dog && (
        <>
          <button 
            className="nav-arrow nav-arrow-left nav-arrow-fixed"
            onClick={handlePreviousDog}
            aria-label="Previous dog"
          >
            &lt;
          </button>
          <button 
            className="nav-arrow nav-arrow-right nav-arrow-fixed"
            onClick={handleNextDog}
            aria-label="Next dog"
          >
            &gt;
          </button>
        </>
      )}
      
      {/* Loading indicator above grid */}
      {isLoading && (
        <div className="loading-indicator">
          <div className="loading-text">Načítám nové pejsky</div>
          <AnimatedDots />
        </div>
      )}
      
      <div className="container-fluid dog-grid-container">
        <div className="row g-3">
          {images.map((image) => (
            <div
              key={String(image.id)}
              className="col-xl-2 col-lg-3 col-md-4 col-sm-6 col-12"
            >
              <div
                className={`dog-grid-item ${
                  hovered.imageId === image.id ? 'dog-grid-item-hovered' : ''
                }`}
                onMouseEnter={(e) => handleImageHover(e, image.id, image.url)}
                // preview position is based on card center; no need to track mouse move
                onMouseLeave={handleImageUnhover}
                onClick={() => handleImageClick(image)}
              >
                <img
                  src={(() => { 
                const st = loadStatus[String(image.id)]; 
                if (st && (st.failed || st.isPlaceholder)) {
                  if (st.failed && !st.isPlaceholder) {
                    // Use original URL from loadStatus if available, otherwise fall back to image.url
                    const originalUrl = st.originalUrl || image.url;
                    console.warn(`[Image Load] Showing placeholder for dog ${image.id} after failure: ${originalUrl}`, {
                      attempts: st.attempts,
                      errorType: st.errorType,
                      lastError: st.lastError,
                      dogId: image.id,
                      originalImageUrl: originalUrl
                    });
                  }
                  return PLACEHOLDER_IMAGE;
                }
                return image.url;
                  })()}
                  alt="Dog"
                  className={`dog-image ${(() => { const st = loadStatus[String(image.id)]; return st && !st.loaded && !st.isPlaceholder && !st.failed ? 'is-loading' : '' })()}`}
                  draggable={false}
                  onError={(event) => {
                const key = String(image.id);
                const errorEvent = event as React.SyntheticEvent<HTMLImageElement, Event>;
                const imgElement = errorEvent.currentTarget;
                const errorType = errorEvent.type || 'unknown';
                // Always use the original URL from image.url, not imgElement.src which might be converted to data URI
                const originalUrl = image.url;
                const errorMessage = `Failed to load image: ${originalUrl.substring(0, 100)}${originalUrl.length > 100 ? '...' : ''}`;
                
                setLoadStatus(prev => {
                  const s = prev[key];
                  if (!s || s.isPlaceholder) return prev;
                  const nextAttempts = (s.attempts ?? 0) + 1;
                  const failed = nextAttempts >= 10;
                  
                  // Log the failure with original URL
                  if (failed) {
                    console.error(`[Image Load] Failed after 10 attempts for dog ${image.id}: ${originalUrl}`, {
                      attempts: nextAttempts,
                      errorType,
                      errorMessage,
                      dogId: image.id,
                      originalImageUrl: originalUrl
                    });
                  } else {
                    console.warn(`[Image Load] Attempt ${nextAttempts}/10 failed for dog ${image.id}: ${originalUrl}`, {
                      errorType,
                      errorMessage,
                      dogId: image.id,
                      originalImageUrl: originalUrl
                    });
                  }
                  
                  return { 
                    ...prev, 
                    [key]: { 
                      ...s, 
                      attempts: nextAttempts, 
                      failed,
                      originalUrl: originalUrl, // Store original URL
                      lastError: errorMessage,
                      errorType
                    } 
                  };
                });
              }}
                />
                {(() => { const st = loadStatus[String(image.id)]; return st && !st.loaded && !st.isPlaceholder && !st.failed; })() && (
                  <div className="card-skeleton" />
                )}
                <div className="dog-card-overlay">
                  <div className="dog-card-line title-line">
                    <span className="dog-card-name">{image.name || 'Neznámé jméno'}</span>
                    {image.sex && (
                      <span className="sex-icon small" aria-label={image.sex} title={image.sex}>
                        {image.sex === 'MALE' ? (
                          <svg width="12" height="12" viewBox="0 0 24 24" fill="#2d7ff9" xmlns="http://www.w3.org/2000/svg">
                            <path d="M14 2h8v8h-2V6.414l-4.293 4.293-1.414-1.414L18.586 5H14V2z"/>
                            <path d="M10 6a6 6 0 1 1 0 12 6 6 0 0 1 0-12zm0 2a4 4 0 1 0 0 8 4 4 0 0 0 0-8z"/>
                          </svg>
                        ) : (
                          <svg width="12" height="12" viewBox="0 0 24 24" fill="#ff61ad" xmlns="http://www.w3.org/2000/svg">
                            <path d="M12 2a6 6 0 1 1 0 12A6 6 0 0 1 12 2zm0 2a4 4 0 1 0 .001 8.001A4 4 0 0 0 12 4z"/>
                            <path d="M11 14h2v3h3v2h-3v3h-2v-3H8v-2h3v-3z"/>
                          </svg>
                        )}
                      </span>
                    )}
                    {typeof image.estimatedAgeInYears === 'number' && (
                      <span className="dog-card-age">{image.estimatedAgeInYears} r.</span>
                    )}
                  </div>
                  <div className="dog-card-line meta-line">
                    {sanitizeMeta(image.breedGuess) && (
                      <span>{sanitizeMeta(image.breedGuess)}</span>
                    )}
                    <span>
                      {(() => {
                        if (image.shelterUrl) {
                          try { return new URL(image.shelterUrl).hostname.replace(/^www\./,''); } catch { return 'útulek'; }
                        }
                        return 'útulek';
                      })()}
                    </span>
                  </div>
                </div>
              </div>
            </div>
          ))}
        </div>
      </div>
      
      {/* Hover preview */}
      {hovered.url && (
        (() => {
          const vv = typeof window !== 'undefined' && (window as any).visualViewport ? (window as any).visualViewport : null;
          const vw = vv ? vv.width : (typeof window !== 'undefined' ? window.innerWidth : 1920);
          const vh = vv ? vv.height : (typeof window !== 'undefined' ? window.innerHeight : 1080);
          const safe = 20;
          const leftSpace = previewAnchor.x - safe;
          const rightSpace = vw - previewAnchor.x - safe;
          const topSpace = previewAnchor.y - safe;
          const bottomSpace = vh - previewAnchor.y - safe;

          const placeLeft = leftSpace > rightSpace;
          const placeAbove = topSpace > bottomSpace;

          const baseLeft = placeLeft ? previewAnchor.x - 24 : previewAnchor.x + 24;
          const baseTop = placeAbove ? previewAnchor.y - 24 : previewAnchor.y + 24;

          // Clamp within viewport safe area
          const left = Math.min(vw - safe, Math.max(safe, baseLeft));
          const top = Math.min(vh - safe, Math.max(safe, baseTop));

          let transform = 'translate(0, 0)';
          if (placeLeft && placeAbove) transform = 'translate(-100%, -100%)';
          else if (!placeLeft && placeAbove) transform = 'translate(0, -100%)';
          else if (placeLeft && !placeAbove) transform = 'translate(-100%, 0)';
          else transform = 'translate(0, 0)';

          return (
            <div className="dog-hover-preview" style={{ top, left, transform }}>
              <img src={hovered.url} alt="Preview" className="dog-hover-image" onError={(e) => { (e.currentTarget as HTMLImageElement).src = PLACEHOLDER_IMAGE; }} />
            </div>
          );
        })()
      )}
      
      {/* Full overlay */}
      {overlay.isVisible && overlay.dog && (
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
            <h1 className="overlay-title">Adoptuj mě, prosím!</h1>
            {(() => {
              // Get all available image URLs for this dog
              const allImageUrls = overlay.dog!.imageUrls || [overlay.dog!.url];
              const currentImageUrl = allImageUrls[overlay.selectedImageIndex] || overlay.dog!.url;
              
              return (
                <>
                  <img 
                    src={(() => { 
                      const st = loadStatus[String(overlay.dog!.id)]; 
                      if (st && (st.failed || st.isPlaceholder)) {
                        if (st.failed && !st.isPlaceholder) {
                          // Use original URL from loadStatus if available, otherwise fall back to current image URL
                          const originalUrl = st.originalUrl || currentImageUrl;
                          console.warn(`[Image Load] Showing placeholder in overlay for dog ${overlay.dog!.id} after failure: ${originalUrl}`, {
                            attempts: st.attempts,
                            errorType: st.errorType,
                            lastError: st.lastError,
                            dogId: overlay.dog!.id,
                            originalImageUrl: originalUrl
                          });
                        }
                        return PLACEHOLDER_IMAGE;
                      }
                      return currentImageUrl;
                    })()} 
                    alt={overlay.dog.name || 'Dog'} 
                    className={`dog-overlay-image ${(() => { const st = loadStatus[String(overlay.dog!.id)]; return st && !st.loaded && !st.isPlaceholder && !st.failed ? 'is-loading' : '' })()}`} 
                    onError={(event) => {
                      const key = String(overlay.dog!.id);
                      const errorEvent = event as React.SyntheticEvent<HTMLImageElement, Event>;
                      const imgElement = errorEvent.currentTarget;
                      const errorType = errorEvent.type || 'unknown';
                      // Always use the current image URL, not imgElement.src which might be converted to data URI
                      const originalUrl = currentImageUrl;
                      const errorMessage = `Failed to load image: ${originalUrl.substring(0, 100)}${originalUrl.length > 100 ? '...' : ''}`;
                      
                      setLoadStatus(prev => {
                        const s = prev[key];
                        if (!s || s.isPlaceholder) return prev;
                        const nextAttempts = (s.attempts ?? 0) + 1;
                        const failed = nextAttempts >= 10;
                        
                        // Log the failure with original URL
                        if (failed) {
                          console.error(`[Image Load] Failed after 10 attempts in overlay for dog ${overlay.dog!.id}: ${originalUrl}`, {
                            attempts: nextAttempts,
                            errorType,
                            errorMessage,
                            dogId: overlay.dog!.id,
                            originalImageUrl: originalUrl
                          });
                        } else {
                          console.warn(`[Image Load] Attempt ${nextAttempts}/10 failed in overlay for dog ${overlay.dog!.id}: ${originalUrl}`, {
                            errorType,
                            errorMessage,
                            dogId: overlay.dog!.id,
                            originalImageUrl: originalUrl
                          });
                        }
                        
                        return { 
                          ...prev, 
                          [key]: { 
                            ...s, 
                            attempts: nextAttempts, 
                            failed,
                            originalUrl: originalUrl, // Store original URL
                            lastError: errorMessage,
                            errorType
                          } 
                        };
                      });
                    }}
                  />
                  {/* Image carousel/thumbnails */}
                  {allImageUrls.length > 1 && (
                    <div className="dog-image-carousel">
                      {allImageUrls.map((url, index) => (
                        <button
                          key={index}
                          className={`dog-thumbnail ${overlay.selectedImageIndex === index ? 'active' : ''}`}
                          onClick={(e) => {
                            e.stopPropagation();
                            handleThumbnailClick(index);
                          }}
                          aria-label={`View image ${index + 1} of ${allImageUrls.length}`}
                        >
                          <img
                            src={url}
                            alt={`${overlay.dog!.name || 'Dog'} - Image ${index + 1}`}
                            onError={(e) => { (e.currentTarget as HTMLImageElement).src = PLACEHOLDER_IMAGE; }}
                          />
                        </button>
                      ))}
                    </div>
                  )}
                </>
              );
            })()}
            <div className="dog-info-box">
              <div className="dog-info-title">{overlay.dog.name || 'Neznámé jméno'}</div>
              <div className="dog-info-meta">
                {sanitizeMeta(overlay.dog.breedGuess) && <span>{sanitizeMeta(overlay.dog.breedGuess)}</span>}
                {overlay.dog.sex && (
                  <span className="sex-icon" aria-label={overlay.dog.sex} title={overlay.dog.sex}>
                    {overlay.dog.sex === 'MALE' ? (
                      <svg width="16" height="16" viewBox="0 0 24 24" fill="#2d7ff9" xmlns="http://www.w3.org/2000/svg">
                        <path d="M14 2h8v8h-2V6.414l-4.293 4.293-1.414-1.414L18.586 5H14V2z"/>
                        <path d="M10 6a6 6 0 1 1 0 12 6 6 0 0 1 0-12zm0 2a4 4 0 1 0 0 8 4 4 0 0 0 0-8z"/>
                      </svg>
                    ) : (
                      <svg width="16" height="16" viewBox="0 0 24 24" fill="#ff61ad" xmlns="http://www.w3.org/2000/svg">
                        <path d="M12 2a6 6 0 1 1 0 12A6 6 0 0 1 12 2zm0 2a4 4 0 1 0 .001 8.001A4 4 0 0 0 12 4z"/>
                        <path d="M11 14h2v3h3v2h-3v3h-2v-3H8v-2h3v-3z"/>
                      </svg>
                    )}
                  </span>
                )}
                {typeof overlay.dog.estimatedAgeInYears === 'number' && (
                  <span> • {overlay.dog.estimatedAgeInYears} roků</span>
                )}
              </div>
              {/* Description intentionally omitted in overlay */}
              <div className="dog-info-linkline">
                Více informací najdete <a className="link" href={overlay.dog.shelterUrl || '#'} target="_blank" rel="noreferrer">ZDE</a>
              </div>
              <div className="dog-info-footer">
                {overlay.dog.shelterUrl && (() => {
                  try {
                    const u = new URL(overlay.dog.shelterUrl);
                    const shelterHome = u.origin;
                    return (
                      <>
                        <a className="btn" href={shelterHome} target="_blank" rel="noreferrer">Útulek {animalAccusative}</a>
                        {overlay.dog.currentWeight && (
                          <span className="pill">{overlay.dog.currentWeight} kg</span>
                        )}
                        {(overlay.dog.estimatedFinalWeightMin || overlay.dog.estimatedFinalWeightMax) && (
                          <span className="pill">
                            finální váha {overlay.dog.estimatedFinalWeightMin || '?'}–{overlay.dog.estimatedFinalWeightMax || '?'} kg
                          </span>
                        )}
                        <a className="btn primary" href={overlay.dog.shelterUrl} target="_blank" rel="noreferrer">{animalAdjective} profil</a>
                      </>
                    );
                  } catch {
                    return (
                      <>
                        {overlay.dog.currentWeight && (
                          <span className="pill">{overlay.dog.currentWeight} kg</span>
                        )}
                        {(overlay.dog.estimatedFinalWeightMin || overlay.dog.estimatedFinalWeightMax) && (
                          <span className="pill">
                            finální váha {overlay.dog.estimatedFinalWeightMin || '?'}–{overlay.dog.estimatedFinalWeightMax || '?'} kg
                          </span>
                        )}
                        <a className="btn primary" href={overlay.dog.shelterUrl!} target="_blank" rel="noreferrer">{animalAdjective} profil</a>
                      </>
                    );
                  }
                })()}
              </div>
            </div>
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