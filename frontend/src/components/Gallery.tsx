import { useEffect, useState, useCallback, useMemo, useRef } from 'react';
import { fetchGalleryPhotos, uploadGalleryPhotos } from '../api';
import type { GalleryPhoto } from '../types';
import { SkeletonCard } from './Skeleton';
import './Gallery.css';

type GalleryView = 'masonry' | 'byDate';

interface GallerySection {
  key: string;
  label: string;
  items: { photo: GalleryPhoto; flatIndex: number }[];
}

export default function Gallery() {
  const [photos, setPhotos] = useState<GalleryPhoto[]>([]);
  const [loading, setLoading] = useState(true);
  const [loadingMore, setLoadingMore] = useState(false);
  const [nextPageToken, setNextPageToken] = useState<string | null>(null);
  const [totalCount, setTotalCount] = useState(0);
  const [lightboxIndex, setLightboxIndex] = useState<number | null>(null);
  const [error, setError] = useState<string | null>(null);
  const sentinelRef = useRef<HTMLDivElement>(null);

  const [view, setView] = useState<GalleryView>(() =>
    localStorage.getItem('gallery-view') === 'byDate' ? 'byDate' : 'masonry'
  );
  const changeView = (v: GalleryView) => {
    setView(v);
    localStorage.setItem('gallery-view', v);
  };

  // Group photos by month of date-taken (EXIF), falling back to upload time.
  // Items keep their index into the flat `photos` array so the lightbox works unchanged.
  const sections = useMemo<GallerySection[]>(() => {
    if (view !== 'byDate') return [];
    const dated = photos.map((photo, flatIndex) => ({
      photo,
      flatIndex,
      date: photo.dateTaken ?? photo.createdTime,
    }));
    dated.sort((a, b) => (b.date ?? '').localeCompare(a.date ?? ''));

    const byKey = new Map<string, GallerySection>();
    for (const { photo, flatIndex, date } of dated) {
      const d = date ? new Date(date) : null;
      const valid = d !== null && !isNaN(d.getTime());
      const key = valid ? `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}` : 'undated';
      let section = byKey.get(key);
      if (!section) {
        section = {
          key,
          label: valid ? d.toLocaleDateString('en-US', { month: 'long', year: 'numeric' }) : 'Undated',
          items: [],
        };
        byKey.set(key, section);
      }
      section.items.push({ photo, flatIndex });
    }
    return [...byKey.values()];
  }, [photos, view]);

  const [showUpload, setShowUpload] = useState(false);
  const [uploadPassword, setUploadPassword] = useState('');
  const [selectedFiles, setSelectedFiles] = useState<File[]>([]);
  const [uploading, setUploading] = useState(false);
  const [uploadStatus, setUploadStatus] = useState<string | null>(null);
  const [uploadError, setUploadError] = useState<string | null>(null);
  const [uploadSuccess, setUploadSuccess] = useState<string | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);

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

  const isHeic = (f: File) =>
    /\.(heic|heif)$/i.test(f.name) || f.type === 'image/heic' || f.type === 'image/heif';

  // iPhone photos are often HEIC, which most browsers can't display. Convert to
  // JPEG client-side so the file stored in Drive renders everywhere.
  const convertIfHeic = async (file: File): Promise<File> => {
    if (!isHeic(file)) return file;
    const { default: heic2any } = await import('heic2any');
    const converted = await heic2any({ blob: file, toType: 'image/jpeg', quality: 0.9 });
    const blob = Array.isArray(converted) ? converted[0] : converted;
    return new File([blob], file.name.replace(/\.(heic|heif)$/i, '.jpg'), { type: 'image/jpeg' });
  };

  const handleUpload = async () => {
    if (!uploadPassword || selectedFiles.length === 0) return;
    setUploading(true);
    setUploadError(null);
    setUploadSuccess(null);
    try {
      let files = selectedFiles;
      if (selectedFiles.some(isHeic)) {
        setUploadStatus('Converting iPhone photos…');
        files = await Promise.all(selectedFiles.map(convertIfHeic));
      }
      setUploadStatus('Uploading…');
      const result = await uploadGalleryPhotos(uploadPassword, files);
      setUploadSuccess(`${result.uploaded} photo${result.uploaded === 1 ? '' : 's'} uploaded!`);
      setSelectedFiles([]);
      if (fileInputRef.current) fileInputRef.current.value = '';
      await loadPhotos();
    } catch (err) {
      setUploadError(err instanceof Error ? err.message : 'Upload failed');
    } finally {
      setUploading(false);
      setUploadStatus(null);
    }
  };

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
        <button
          className="gallery-upload-toggle"
          onClick={() => { setShowUpload(!showUpload); setUploadError(null); setUploadSuccess(null); }}
        >
          {showUpload ? 'Close' : '📷 Share Your Photos'}
        </button>
        <div className="gallery-view-toggle" role="group" aria-label="Gallery layout">
          <button
            className={view === 'masonry' ? 'active' : ''}
            onClick={() => changeView('masonry')}
          >
            All photos
          </button>
          <button
            className={view === 'byDate' ? 'active' : ''}
            onClick={() => changeView('byDate')}
          >
            By date
          </button>
        </div>
      </div>

      {showUpload && (
        <div className="gallery-upload-panel">
          <h3>Upload Photos</h3>
          <p className="gallery-upload-hint">
            Enter the family password and pick your photos — they'll be added to the shared album.
          </p>
          <input
            type="password"
            className="gallery-upload-password"
            placeholder="Family password"
            value={uploadPassword}
            onChange={(e) => setUploadPassword(e.target.value)}
            disabled={uploading}
          />
          <input
            ref={fileInputRef}
            type="file"
            className="gallery-upload-file"
            accept="image/*,.heic,.heif"
            multiple
            onChange={(e) => setSelectedFiles(Array.from(e.target.files ?? []))}
            disabled={uploading}
          />
          {selectedFiles.length > 0 && (
            <p className="gallery-upload-count">
              {selectedFiles.length} photo{selectedFiles.length === 1 ? '' : 's'} selected
            </p>
          )}
          {uploadError && <p className="gallery-upload-error">{uploadError}</p>}
          {uploadSuccess && <p className="gallery-upload-success">{uploadSuccess}</p>}
          <button
            className="gallery-upload-submit"
            onClick={handleUpload}
            disabled={uploading || !uploadPassword || selectedFiles.length === 0}
          >
            {uploading ? (uploadStatus ?? 'Uploading…') : 'Upload'}
          </button>
        </div>
      )}

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
          {view === 'masonry' ? (
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
          ) : (
            <div className="gallery-sections">
              {sections.map((section) => (
                <section key={section.key} className="gallery-section">
                  <h3 className="gallery-section-header">
                    {section.label}
                    <span className="gallery-section-count">{section.items.length}</span>
                  </h3>
                  <div className="gallery-rows">
                    {section.items.map(({ photo, flatIndex }) => {
                      const ratio = photo.width && photo.height ? photo.width / photo.height : 4 / 3;
                      return (
                        <div
                          key={photo.id}
                          className="gallery-row-item"
                          style={{ flexGrow: ratio, flexBasis: `calc(${ratio} * var(--gallery-row-height))` }}
                          onClick={() => setLightboxIndex(flatIndex)}
                        >
                          <img src={photo.thumbnailUrl} alt={photo.name} loading="lazy" />
                        </div>
                      );
                    })}
                  </div>
                </section>
              ))}
            </div>
          )}

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
