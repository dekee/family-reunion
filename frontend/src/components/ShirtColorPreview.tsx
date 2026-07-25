import { useState } from 'react';
import { PILLARS, type Pillar } from './Tributes';
import './ShirtColorPreview.css';

interface PreviewDesign {
  key: string;
  name: string;
  src: string;
  // intrinsic aspect ratio (height / width) of the transparent artwork
  aspect: number;
}

const DESIGNS: PreviewDesign[] = [
  { key: 'legacy', name: 'Legacy Tree', src: '/designs/design-legacy-tree-transparent.png', aspect: 1 },
  { key: 'heart', name: 'Heart & Branches', src: '/designs/design-heart-tree-transparent.png', aspect: 647 / 700 },
];

// Crew-neck tee silhouette, front view. viewBox 0 0 200 214.
const TEE_BODY =
  'M78,16 L40,29 L14,76 L47,91 L56,74 L56,204 L144,204 L144,74 L153,91 L186,76 L160,29 L122,16 C118,30 82,30 78,16 Z';
const COLLAR = 'M78,16 C82,30 118,30 122,16 L114,13 C111,22 89,22 86,13 Z';

function Tee({ pillar, design }: { pillar: Pillar; design: PreviewDesign }) {
  const printW = 76;
  const printH = printW * design.aspect;
  return (
    <svg viewBox="0 0 200 214" role="img" aria-label={`${pillar.colorName} shirt with ${design.name} design`}>
      <path d={TEE_BODY} fill={pillar.hex} stroke={pillar.ink} strokeWidth="1.5" strokeLinejoin="round" />
      <path d="M14,76 L47,91 L56,74" fill="none" stroke={pillar.ink} strokeWidth="1.2" opacity="0.55" />
      <path d="M186,76 L153,91 L144,74" fill="none" stroke={pillar.ink} strokeWidth="1.2" opacity="0.55" />
      <path d={COLLAR} fill={pillar.ink} opacity="0.85" />
      <path d="M56,74 L56,204 L64,204 L64,90 Z" fill="#000" opacity="0.07" />
      <path d="M144,74 L144,204 L136,204 L136,90 Z" fill="#fff" opacity="0.05" />
      <image
        href={design.src}
        x={100 - printW / 2}
        y={52}
        width={printW}
        height={printH}
        preserveAspectRatio="xMidYMid meet"
      />
    </svg>
  );
}

export default function ShirtColorPreview() {
  const [designKey, setDesignKey] = useState('legacy');
  const design = DESIGNS.find((d) => d.key === designKey) ?? DESIGNS[0];

  return (
    <section className="shirt-preview">
      <div className="shirt-preview-header">
        <h3>See Them on the Shirt Colors</h3>
        <p>
          Each pillar picked a shirt color (Gildan 5000). Switch designs to compare how each one
          looks on all eleven colors. Screen colors are approximate.
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
      <div className="shirt-preview-grid">
        {PILLARS.map((p) => (
          <div key={p.firstName} className="shirt-preview-card">
            <Tee pillar={p} design={design} />
            <div className="shirt-preview-caption">
              <span className="shirt-preview-chip" style={{ background: p.hex }} />
              <span className="shirt-preview-who">{p.displayName}</span>
              <span className="shirt-preview-colorname">{p.colorName}</span>
            </div>
          </div>
        ))}
      </div>
    </section>
  );
}
