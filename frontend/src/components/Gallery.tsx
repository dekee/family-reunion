import { useEffect, useState, useCallback, useRef } from 'react';
import { fetchGalleryPhotos } from '../api';
import type { GalleryPhoto } from '../types';
import { SkeletonCard } from './Skeleton';
import './Gallery.css';

export default function Gallery() {
  const [photos, setPhotos] = useState<GalleryPhoto[]>([]);
  const [loading, setLoading] = useState(true);
  const [loadingMore, setLoadingMore] = useState(false);
  const [nextPageToken, setNextPageToken] = useState<string | null>(null);
  const [totalCount, setTotalCount] = useState(0);
  const [lightboxIndex, setLightboxIndex] = useState<number | null>(null);
  const [error, setError] = useState<string | null>(null);
  const sentinelRef = useRef<HTMLDivElement>(null);

  const loadPhotos = useCallback(async (pageToken?: string) => {
    try {
      if (pageToken) {
        setLoadingMore(true);
      } else {
        setLoading(true);
      }
      const data = await fetchGalleryPhotos(pageToken);
      setPhotos(prev => pageToken ? [...prev, ...data.photos] : data.photos);
      setNextPageToken(data.nextPageToken);
      setTotalCount(data.totalCount);
      setError(null);
    } catch (err) {
      if (!pageToken) {
        setError(err instanceof Error ? err.message : 'Failed to load photos');
      }
    } finally {
      setLoading(false);
      setLoadingMore(false);
    }
  }, []);

  useEffect(() => {
    loadPhotos();
  }, [loadPhotos]);

  // Infinite scroll
  useEffect(() => {
    if (!sentinelRef.current || !nextPageToken) return;
    const observer = new IntersectionObserver(
      (entries) => {
        if (entries[0].isIntersecting && nextPageToken && !loadingMore) {
          loadPhotos(nextPageToken);
        }
      },
      { rootMargin: '200px' }
    );
    observer.observe(sentinelRef.current);
    return () => observer.disconnect();
  }, [nextPageToken, loadingMore, loadPhotos]);

  // Lightbox keyboard navigation
  useEffect(() => {
    if (lightboxIndex === null) return;
    const handleKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') setLightboxIndex(null);
      if (e.key === 'ArrowRight' && lightboxIndex < photos.length - 1) setLightboxIndex(lightboxIndex + 1);
      if (e.key === 'ArrowLeft' && lightboxIndex > 0) setLightboxIndex(lightboxIndex - 1);
    };
    document.body.style.overflow = 'hidden';
    window.addEventListener('keydown', handleKey);
    return () => {
      document.body.style.overflow = '';
      window.removeEventListener('keydown', handleKey);
    };
  }, [lightboxIndex, photos.length]);

  if (error) {
    return (
      <div className="gallery-page">
        <div className="page-header">
          <h2>Photo Gallery</h2>
          <p>Family memories shared together</p>
        </div>
        <div className="gallery-empty">
          <p>Gallery is not available right now.</p>
          <p className="gallery-empty-sub">{error}</p>
        </div>
      </div>
    );
  }

  return (
    <div className="gallery-page">
      <div className="page-header">
        <h2>Photo Gallery</h2>
        <p>{totalCount > 0 ? `${totalCount} photos` : 'Family memories shared together'}</p>
      </div>

      {loading ? (
        <div className="gallery-grid">
          {Array.from({ length: 12 }).map((_, i) => (
            <SkeletonCard key={i} />
          ))}
        </div>
      ) : photos.length === 0 ? (
        <div className="gallery-empty">
          <p>No photos yet.</p>
          <p className="gallery-empty-sub">Photos added to the shared Google Drive folder will appear here.</p>
        </div>
      ) : (
        <>
          <div className="gallery-grid">
            {photos.map((photo, index) => (
              <div
                key={photo.id}
                className="gallery-item"
                onClick={() => setLightboxIndex(index)}
                style={{
                  aspectRatio: photo.width && photo.height
                    ? `${photo.width}/${photo.height}`
                    : '4/3',
                }}
              >
                <img
                  src={photo.thumbnailUrl}
                  alt={photo.name}
                  loading="lazy"
                />
              </div>
            ))}
          </div>

          {loadingMore && (
            <div className="gallery-loading-more">Loading more photos...</div>
          )}
          <div ref={sentinelRef} />
        </>
      )}

      {/* Lightbox */}
      {lightboxIndex !== null && photos[lightboxIndex] && (
        <div className="gallery-lightbox" onClick={() => setLightboxIndex(null)}>
          <button
            className="gallery-lightbox-close"
            onClick={() => setLightboxIndex(null)}
            aria-label="Close"
          >
            &times;
          </button>

          {lightboxIndex > 0 && (
            <button
              className="gallery-lightbox-nav gallery-lightbox-prev"
              onClick={(e) => { e.stopPropagation(); setLightboxIndex(lightboxIndex - 1); }}
              aria-label="Previous photo"
            >
              &#8249;
            </button>
          )}

          <img
            src={photos[lightboxIndex].fullUrl}
            alt={photos[lightboxIndex].name}
            className="gallery-lightbox-img"
            onClick={(e) => e.stopPropagation()}
          />

          {lightboxIndex < photos.length - 1 && (
            <button
              className="gallery-lightbox-nav gallery-lightbox-next"
              onClick={(e) => { e.stopPropagation(); setLightboxIndex(lightboxIndex + 1); }}
              aria-label="Next photo"
            >
              &#8250;
            </button>
          )}

          <div className="gallery-lightbox-caption" onClick={(e) => e.stopPropagation()}>
            {photos[lightboxIndex].name.replace(/\.[^.]+$/, '')}
            <span className="gallery-lightbox-counter">
              {lightboxIndex + 1} / {photos.length}
            </span>
          </div>
        </div>
      )}
    </div>
  );
}
