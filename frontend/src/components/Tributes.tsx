import { useEffect, useState, useRef } from 'react';
import { fetchTributes, submitTribute, deleteTribute, fetchFamilyTree } from '../api';
import { useAuth } from '../AuthContext';
import { useToast } from './Toast';
import type { TributeResponse, FamilyTreeNode } from '../types';
import './Tributes.css';

// The 11 pillars and the shirt colors each picked to represent their family
// (Gildan 5000 color chart). Branch heads in the DB use full names — some with
// married surnames (e.g. "Cheryl Johnson", "Gail Creecy") — so pillars are
// matched by first name, which is unique among the 11 siblings.
export interface Pillar {
  firstName: string;
  displayName: string;
  colorName: string;
  hex: string;
  ink: string; // darker variant for legible text on cream
}

export const PILLARS: Pillar[] = [
  { firstName: 'Cheryl', displayName: 'Cheryl', colorName: 'Red', hex: '#C8102E', ink: '#A50D26' },
  { firstName: 'Kendra', displayName: 'Kendra', colorName: 'Military Green', hex: '#5E6737', ink: '#4C5329' },
  { firstName: 'Stephen', displayName: 'Stephen', colorName: 'Sapphire Blue', hex: '#1D6FC9', ink: '#1859A3' },
  { firstName: 'Myra', displayName: 'Myra', colorName: 'Daisy Yellow', hex: '#F4C223', ink: '#B08A08' },
  { firstName: 'Wendell', displayName: 'Wendell', colorName: 'Old Gold', hex: '#C08A2D', ink: '#966C20' },
  { firstName: 'Gail', displayName: 'Gail', colorName: 'Mint Green', hex: '#A9CBB7', ink: '#5F8A72' },
  { firstName: 'Chantell', displayName: 'Chantel', colorName: 'Purple', hex: '#5C2D91', ink: '#4C2478' },
  { firstName: 'Wesley', displayName: 'Wesley', colorName: 'Tennessee Orange', hex: '#F77F00', ink: '#C56500' },
  { firstName: 'Donald', displayName: 'Donald', colorName: 'Orange', hex: '#F4633A', ink: '#CE4519' },
  { firstName: 'Michael', displayName: 'Michael', colorName: 'Turf Green', hex: '#4A7729', ink: '#3B6021' },
  { firstName: 'Norris', displayName: 'Norris', colorName: 'Sand', hex: '#CABF9F', ink: '#8B7E58' },
];

// "Cheryl Johnson" → pillar with firstName "Cheryl"
const pillarByName = (name: string) =>
  PILLARS.find((p) => p.firstName === name.split(' ')[0]);

function Leaf({ fill, size = 84 }: { fill: string; size?: number }) {
  return (
    <svg
      className="pillar-leaf"
      width={size}
      height={size * 1.3}
      viewBox="0 0 100 130"
      role="img"
      aria-hidden="true"
    >
      <path
        d="M50 3 C28 22 10 50 12 82 C14 106 30 122 50 127 C70 122 86 106 88 82 C90 50 72 22 50 3 Z"
        fill={fill}
        stroke="rgba(0,0,0,0.18)"
        strokeWidth="1.5"
      />
      <path d="M50 12 L50 120" stroke="rgba(0,0,0,0.22)" strokeWidth="1.8" fill="none" />
      <g stroke="rgba(0,0,0,0.16)" strokeWidth="1.2" fill="none">
        <path d="M50 32 C42 34 32 40 26 48" />
        <path d="M50 32 C58 34 68 40 74 48" />
        <path d="M50 52 C41 55 30 62 24 71" />
        <path d="M50 52 C59 55 70 62 76 71" />
        <path d="M50 72 C42 76 33 82 28 91" />
        <path d="M50 72 C58 76 67 82 72 91" />
        <path d="M50 92 C44 96 37 101 33 108" />
        <path d="M50 92 C56 96 63 101 67 108" />
      </g>
    </svg>
  );
}

interface FlatMember {
  id: number;
  name: string;
  branchName: string;
}

function flattenTree(nodes: FamilyTreeNode[]): FlatMember[] {
  const result: FlatMember[] = [];
  function walk(n: FamilyTreeNode, branch: string) {
    if (!n.excludeFromRsvp) {
      result.push({ id: n.id, name: n.name, branchName: branch });
    }
    n.children.forEach((c) => walk(c, branch));
  }
  for (const root of nodes) {
    if (root.children.length > 0) {
      for (const child of root.children) {
        walk(child, child.name);
      }
    }
  }
  return result.sort((a, b) => a.name.localeCompare(b.name));
}

export default function Tributes() {
  const [tributes, setTributes] = useState<TributeResponse[]>([]);
  const [members, setMembers] = useState<FlatMember[]>([]);
  const [siblingIds, setSiblingIds] = useState<Map<string, number>>(new Map());
  const [loading, setLoading] = useState(true);
  const [selectedPillar, setSelectedPillar] = useState<Pillar | null>(null);
  const [selectedMemberId, setSelectedMemberId] = useState<number | null>(() => {
    const saved = localStorage.getItem('tribute_member_id');
    return saved ? Number(saved) : null;
  });
  const [search, setSearch] = useState('');
  const [dropdownOpen, setDropdownOpen] = useState(false);
  const [story, setStory] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [writing, setWriting] = useState(false);
  const dropdownRef = useRef<HTMLDivElement>(null);
  const detailRef = useRef<HTMLDivElement>(null);
  const { isAdmin } = useAuth();
  const { showToast } = useToast();

  const load = () => {
    Promise.all([fetchTributes(), fetchFamilyTree()])
      .then(([t, tree]) => {
        setTributes(t);
        setMembers(flattenTree(tree.roots));
        // The 11 pillars are the root founders' children; map pillar first name → member id
        const ids = new Map<string, number>();
        for (const root of tree.roots) {
          for (const child of root.children) {
            const pillar = pillarByName(child.name);
            if (pillar && !ids.has(pillar.firstName)) {
              ids.set(pillar.firstName, child.id);
            }
          }
        }
        setSiblingIds(ids);
      })
      .catch(() => showToast('Failed to load tributes', 'error'))
      .finally(() => setLoading(false));
  };

  useEffect(() => { load(); }, []);

  // Close dropdown on outside click
  useEffect(() => {
    const handler = (e: MouseEvent) => {
      if (dropdownRef.current && !dropdownRef.current.contains(e.target as Node)) {
        setDropdownOpen(false);
      }
    };
    document.addEventListener('mousedown', handler);
    return () => document.removeEventListener('mousedown', handler);
  }, []);

  const selectedMember = members.find((m) => m.id === selectedMemberId);
  const selectedSiblingId = selectedPillar ? siblingIds.get(selectedPillar.firstName) : undefined;
  const pillarTributes = selectedPillar
    ? tributes.filter((t) => t.siblingId === selectedSiblingId)
    : [];
  const myExisting = pillarTributes.find((t) => t.authorId === selectedMemberId);

  const tributeCount = (p: Pillar) => {
    const id = siblingIds.get(p.firstName);
    return tributes.filter((t) => t.siblingId === id).length;
  };

  const filtered = search.trim()
    ? members.filter((m) => m.name.toLowerCase().includes(search.toLowerCase()))
    : members;

  const grouped = new Map<string, FlatMember[]>();
  for (const m of filtered) {
    const list = grouped.get(m.branchName) || [];
    list.push(m);
    grouped.set(m.branchName, list);
  }
  const sortedBranches = [...grouped.entries()].sort((a, b) => a[0].localeCompare(b[0]));

  const handleSelectPillar = (p: Pillar) => {
    setSelectedPillar(p);
    setWriting(false);
    setStory('');
    setTimeout(() => detailRef.current?.scrollIntoView({ behavior: 'smooth', block: 'start' }), 50);
  };

  const handleStartWriting = () => {
    setWriting(true);
    setStory(myExisting ? myExisting.story : '');
  };

  const handleSelectMember = (member: FlatMember) => {
    setSelectedMemberId(member.id);
    localStorage.setItem('tribute_member_id', String(member.id));
    setSearch('');
    setDropdownOpen(false);
    if (selectedSiblingId) {
      const existing = tributes.find(
        (t) => t.siblingId === selectedSiblingId && t.authorId === member.id
      );
      setStory(existing ? existing.story : '');
    }
  };

  const handleChangeMember = () => {
    setSelectedMemberId(null);
    localStorage.removeItem('tribute_member_id');
    setStory('');
  };

  const handleSubmit = async () => {
    if (!selectedSiblingId || !selectedMemberId || !story.trim()) return;
    setSubmitting(true);
    try {
      await submitTribute({ siblingId: selectedSiblingId, authorId: selectedMemberId, story: story.trim() });
      showToast(myExisting ? 'Tribute updated!' : 'Tribute shared! Thank you');
      setWriting(false);
      setStory('');
      load();
    } catch (err: any) {
      showToast(err.message || 'Failed to share tribute', 'error');
    } finally {
      setSubmitting(false);
    }
  };

  const handleDelete = async (id: number) => {
    if (!window.confirm('Delete this tribute?')) return;
    try {
      await deleteTribute(id);
      showToast('Tribute deleted');
      load();
    } catch (err: any) {
      showToast(err.message || 'Failed to delete tribute', 'error');
    }
  };

  const formatDate = (iso: string) =>
    new Date(iso).toLocaleDateString(undefined, { month: 'long', day: 'numeric', year: 'numeric' });

  return (
    <div className="tributes-page">
      <div className="tributes-chart">
        <header className="tributes-header">
          <h2>Pillars of Our Family</h2>
          <div className="tributes-ornament">
            <span className="ornament-line" />
            <span className="ornament-heart">&hearts;</span>
            <span className="ornament-line" />
          </div>
          <p className="tributes-caption">The Eleven Children of Wesley &amp; Esther Tumblin &middot; Est. 1948</p>
          <p className="tributes-sub">
            Each pillar chose a color to represent their family. Select a pillar to read
            tributes or share a story of your own.
          </p>
        </header>

        {loading ? (
          <div className="tributes-loading">Loading tributes...</div>
        ) : (
          <div className="pillar-grid">
            {PILLARS.map((p, i) => {
              const count = tributeCount(p);
              const selected = selectedPillar?.firstName === p.firstName;
              return (
                <button
                  key={p.firstName}
                  className={`pillar-card ${selected ? 'selected' : ''}`}
                  style={{ '--pillar-hex': p.hex, '--pillar-ink': p.ink, ...(selected ? { outlineColor: p.hex } : {}) } as React.CSSProperties}
                  onClick={() => handleSelectPillar(p)}
                >
                  <span className="pillar-number">{i + 1}.</span>
                  <Leaf fill={p.hex} />
                  <span className="pillar-name">{p.displayName}</span>
                  <span className="pillar-color-name">{p.colorName}</span>
                  <span className="pillar-divider">
                    <span className="pillar-divider-line" style={{ background: p.hex }} />
                    <span className="pillar-divider-heart" style={{ color: p.hex }}>&hearts;</span>
                    <span className="pillar-divider-line" style={{ background: p.hex }} />
                  </span>
                  <span className="pillar-count">
                    {count === 0 ? 'No tributes yet' : `${count} tribute${count !== 1 ? 's' : ''}`}
                  </span>
                </button>
              );
            })}
          </div>
        )}
        <p className="tributes-footnote">* Colors are based on the Gildan 5000 color chart.</p>
      </div>

      {selectedPillar && (
        <div
          className="tribute-detail"
          ref={detailRef}
          style={{ '--pillar-hex': selectedPillar.hex, '--pillar-ink': selectedPillar.ink } as React.CSSProperties}
        >
          <div className="tribute-detail-header">
            <Leaf fill={selectedPillar.hex} size={40} />
            <div>
              <h3>Tributes to {selectedPillar.displayName}</h3>
              <span className="tribute-detail-colorname">{selectedPillar.colorName}</span>
            </div>
          </div>

          {pillarTributes.length === 0 ? (
            <p className="tribute-empty">
              No tributes yet &mdash; be the first to share a story about {selectedPillar.displayName}.
            </p>
          ) : (
            <div className="tribute-cards">
              {pillarTributes.map((t) => {
                const author = members.find((m) => m.id === t.authorId);
                return (
                  <div key={t.id} className="tribute-card" style={{ borderLeftColor: selectedPillar.hex }}>
                    <p className="tribute-story">{t.story}</p>
                    <div className="tribute-byline">
                      <span className="tribute-author">
                        &mdash; {t.authorName}
                        {author && author.branchName !== t.authorName && (
                          <span className="tribute-author-branch">
                            {' '}&middot; {pillarByName(author.branchName)?.displayName ?? author.branchName} branch
                          </span>
                        )}
                      </span>
                      <span className="tribute-date">{formatDate(t.createdAt)}</span>
                      {isAdmin && (
                        <button className="tribute-delete" onClick={() => handleDelete(t.id)}>
                          Delete
                        </button>
                      )}
                    </div>
                  </div>
                );
              })}
            </div>
          )}

          {!writing ? (
            <div className="tribute-share-cta">
              <button
                className="btn-share-tribute"
                onClick={handleStartWriting}
              >
                {myExisting ? 'Edit Your Tribute' : `Share a Story About ${selectedPillar.displayName}`}
              </button>
            </div>
          ) : (
            <div className="tribute-form">
              {!selectedMember ? (
                <div className="tribute-member-picker" ref={dropdownRef}>
                  <label className="tribute-member-label">First, find your name</label>
                  <input
                    type="text"
                    className="tribute-member-search"
                    value={search}
                    onChange={(e) => { setSearch(e.target.value); setDropdownOpen(true); }}
                    onFocus={() => setDropdownOpen(true)}
                    placeholder="Search by name..."
                    autoComplete="off"
                  />
                  {dropdownOpen && (
                    <div className="tribute-member-dropdown">
                      {filtered.length === 0 ? (
                        <div className="tribute-dropdown-empty">No members found</div>
                      ) : (
                        sortedBranches.map(([branch, branchMembers]) => (
                          <div key={branch}>
                            <div
                              className="tribute-dropdown-branch"
                              style={{ borderLeftColor: pillarByName(branch)?.hex ?? 'var(--color-border)' }}
                            >
                              {pillarByName(branch)?.displayName ?? branch}
                            </div>
                            {branchMembers.map((m) => (
                              <button
                                key={m.id}
                                className="tribute-dropdown-item"
                                onClick={() => handleSelectMember(m)}
                              >
                                {m.name}
                              </button>
                            ))}
                          </div>
                        ))
                      )}
                    </div>
                  )}
                </div>
              ) : (
                <>
                  <div className="tribute-writing-as">
                    <span className="tribute-writing-as-label">Sharing as</span>
                    <span
                      className="tribute-writing-as-name"
                      style={{ borderLeftColor: pillarByName(selectedMember.branchName)?.hex ?? 'var(--color-border)' }}
                    >
                      {selectedMember.name}
                    </span>
                    <button className="btn-not-you" onClick={handleChangeMember}>Not you?</button>
                  </div>
                  <textarea
                    className="tribute-textarea"
                    value={story}
                    onChange={(e) => setStory(e.target.value.slice(0, 4000))}
                    placeholder={`Share a story, memory, or words of appreciation for ${selectedPillar.displayName}...`}
                    rows={6}
                  />
                  <div className="tribute-form-footer">
                    <span className="tribute-char-count">{story.length} / 4000</span>
                    <div className="tribute-form-actions">
                      <button className="btn-cancel-tribute" onClick={() => setWriting(false)}>
                        Cancel
                      </button>
                      <button
                        className="btn-share-tribute"
                        onClick={handleSubmit}
                        disabled={submitting || !story.trim()}
                      >
                        {submitting ? 'Sharing...' : myExisting ? 'Update Tribute' : 'Share Tribute'}
                      </button>
                    </div>
                  </div>
                </>
              )}
            </div>
          )}
        </div>
      )}
    </div>
  );
}
