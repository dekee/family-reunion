import { useEffect, useState } from 'react';
import './ShirtColorPreview.css';

interface PreviewDesign {
  key: string;
  name: string;
  src: string;
}

const DESIGNS: PreviewDesign[] = [
  { key: 'legacy', name: 'Legacy Tree', src: '/designs/mockup-legacy-tree.jpg' },
  { key: 'heart', name: 'Heart & Branches', src: '/designs/mockup-heart-tree.jpg' },
];

export default function ShirtColorPreview() {
  const [designKey, setDesignKey] = useState('legacy');
  const [zoomed, setZoomed] = useState(false);
  const design = DESIGNS.find((d) => d.key === designKey) ?? DESIGNS[0];

  useEffect(() => {
    if (!zoomed) return;
    const handler = (e: KeyboardEvent) => {
      if (e.key === 'Escape') setZoomed(false);
    };
    document.addEventListener('keydown', handler);
    return () => document.removeEventListener('keydown', handler);
  }, [zoomed]);

  return (
    <section className="shirt-preview">
      <div className="shirt-preview-header">
        <h3>See Them on the Shirt Colors</h3>
        <p>
          Each pillar picked a shirt color (Gildan 5000). Switch designs to compare how each one
          looks on all the colors. Tap the picture to see it full size.
        </p>
        <div className="shirt-preview-toggle" role="group" aria-label="Choose design to preview">
          {DESIGNS.map((d) => (
            <button
              key={d.key}
              aria-pressed={designKey === d.key}
              onClick={() => setDesignKey(d.key)}
            >
              {d.name}
            </button>
          ))}
        </div>
      </div>
      <button
        className="shirt-preview-image"
        onClick={() => setZoomed(true)}
        aria-label={`View ${design.name} shirt colors full size`}
      >
        <img src={design.src} alt={`${design.name} design on every pillar shirt color`} />
      </button>

      {zoomed && (
        <div className="shirt-preview-lightbox" onClick={() => setZoomed(false)}>
          <button className="shirt-preview-lightbox-close" aria-label="Close">&times;</button>
          <img src={design.src} alt={`${design.name} design on every pillar shirt color`} />
        </div>
      )}
    </section>
  );
}
