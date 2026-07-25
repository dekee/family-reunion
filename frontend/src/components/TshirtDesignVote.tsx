import { useEffect, useState, useRef } from 'react';
import { fetchDesigns, voteForDesign, fetchMyDesignVote, fetchFamilyTree } from '../api';
import { useToast } from './Toast';
import { getBranchColor } from '../branchColors';
import type { DesignResponse, FamilyTreeNode } from '../types';
import './TshirtSurvey.css';
import './TshirtDesignVote.css';

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

type View = 'pick-member' | 'pick-design' | 'results';

export default function TshirtDesignVote() {
  const [designs, setDesigns] = useState<DesignResponse[]>([]);
  const [members, setMembers] = useState<FlatMember[]>([]);
  const [loading, setLoading] = useState(true);
  const [selectedMemberId, setSelectedMemberId] = useState<number | null>(() => {
    const saved = localStorage.getItem('slogan_member_id');
    return saved ? Number(saved) : null;
  });
  const [search, setSearch] = useState('');
  const [dropdownOpen, setDropdownOpen] = useState(false);
  const [selectedDesignId, setSelectedDesignId] = useState<number | null>(null);
  const [hasVoted, setHasVoted] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [view, setView] = useState<View>('pick-member');
  const [zoomedDesign, setZoomedDesign] = useState<DesignResponse | null>(null);
  const dropdownRef = useRef<HTMLDivElement>(null);
  const { showToast } = useToast();

  const load = () => {
    setLoading(true);
    Promise.all([fetchDesigns(), fetchFamilyTree()])
      .then(([d, tree]) => {
        setDesigns(d);
        setMembers(flattenTree(tree.roots));
      })
      .catch(() => showToast('Failed to load data', 'error'))
      .finally(() => setLoading(false));
  };

  useEffect(() => { load(); }, []);

  // Restore previous vote once members are loaded
  useEffect(() => {
    if (members.length === 0) return;
    const savedId = localStorage.getItem('slogan_member_id');
    if (savedId) {
      fetchMyDesignVote(Number(savedId)).then((result) => {
        if (result.designId) {
          setSelectedDesignId(result.designId);
          setHasVoted(true);
          setView('results');
        } else {
          setView('pick-design');
        }
      }).catch(() => {});
    }
  }, [members]);

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

  // Close zoom on Escape
  useEffect(() => {
    if (!zoomedDesign) return;
    const handler = (e: KeyboardEvent) => {
      if (e.key === 'Escape') setZoomedDesign(null);
    };
    document.addEventListener('keydown', handler);
    return () => document.removeEventListener('keydown', handler);
  }, [zoomedDesign]);

  const selectedMember = members.find((m) => m.id === selectedMemberId);

  const filtered = search.trim()
    ? members.filter((m) => m.name.toLowerCase().includes(search.toLowerCase()))
    : members;

  // Group filtered members by branch
  const grouped = new Map<string, FlatMember[]>();
  for (const m of filtered) {
    const list = grouped.get(m.branchName) || [];
    list.push(m);
    grouped.set(m.branchName, list);
  }
  const sortedBranches = [...grouped.entries()].sort((a, b) => a[0].localeCompare(b[0]));

  const totalVotes = designs.reduce((sum, d) => sum + d.voteCount, 0);
  const maxVotes = Math.max(...designs.map((d) => d.voteCount), 1);

  const handleSelectMember = (member: FlatMember) => {
    setSelectedMemberId(member.id);
    setSearch('');
    setDropdownOpen(false);
    fetchMyDesignVote(member.id).then((result) => {
      if (result.designId) {
        setSelectedDesignId(result.designId);
        setHasVoted(true);
        setView('results');
      } else {
        setSelectedDesignId(null);
        setHasVoted(false);
        setView('pick-design');
      }
    }).catch(() => {});
  };

  const handleVote = async () => {
    if (selectedMemberId === null || selectedDesignId === null) return;

    setSubmitting(true);
    try {
      await voteForDesign({ designId: selectedDesignId, familyMemberId: selectedMemberId });
      localStorage.setItem('slogan_member_id', String(selectedMemberId));
      setHasVoted(true);
      setView('results');
      showToast('Vote submitted! Thank you');
      load();
    } catch (err: any) {
      showToast(err.message || 'Failed to submit vote', 'error');
    } finally {
      setSubmitting(false);
    }
  };

  const handleChangeMember = () => {
    setSelectedMemberId(null);
    setSelectedDesignId(null);
    setHasVoted(false);
    setView('pick-member');
    localStorage.removeItem('slogan_member_id');
  };

  const resultsBlock = (
    <div className="survey-results">
      <div className="survey-results-header">
        <h3>Results ({totalVotes} vote{totalVotes !== 1 ? 's' : ''})</h3>
      </div>
      <div className="design-result-cards">
        {[...designs]
          .sort((a, b) => b.voteCount - a.voteCount)
          .map((design, index) => {
            const pct = totalVotes > 0 ? (design.voteCount / totalVotes) * 100 : 0;
            const isMyVote = hasVoted && design.id === selectedDesignId;
            return (
              <div
                key={design.id}
                className={`design-result-card ${isMyVote ? 'my-vote' : ''} ${index === 0 && design.voteCount > 0 ? 'leading' : ''}`}
              >
                <button
                  className="design-result-thumb"
                  onClick={() => setZoomedDesign(design)}
                  aria-label={`View ${design.name} full size`}
                >
                  <img src={design.imageUrl} alt={design.name} />
                </button>
                <div className="design-result-body">
                  <div className="result-slogan">
                    {isMyVote && <span className="my-vote-badge">Your vote</span>}
                    <span className="result-text">{design.name}</span>
                  </div>
                  <div className="design-result-bar-track">
                    <div className="design-result-bar" style={{ width: `${(design.voteCount / maxVotes) * 100}%` }} />
                  </div>
                  <div className="result-stats">
                    <span className="result-count">{design.voteCount}</span>
                    <span className="result-pct">{pct.toFixed(0)}%</span>
                  </div>
                </div>
              </div>
            );
          })}
      </div>
    </div>
  );

  return (
    <div className="survey-page">
      <div className="page-header">
        <h2>T-Shirt Design Vote</h2>
        <p>The slogan is chosen — now help us pick the winning t-shirt design! Tap a design to see it full size.</p>
      </div>

      {loading ? (
        <div className="survey-loading">Loading designs...</div>
      ) : view === 'results' ? (
        <>
          {/* Show who voted if they're identified */}
          {selectedMember && (
            <div className="survey-member-section">
              <div className="survey-selected-member">
                <span className="selected-member-label">Voting as</span>
                <span
                  className="selected-member-name"
                  style={{ borderLeftColor: getBranchColor(selectedMember.branchName) }}
                >
                  {selectedMember.name}
                  <span className="selected-member-branch">{selectedMember.branchName} branch</span>
                </span>
              </div>
            </div>
          )}

          {resultsBlock}

          <div className="survey-results-actions">
            {hasVoted ? (
              <button className="btn-change-vote" onClick={() => setView('pick-design')}>
                Change My Vote
              </button>
            ) : (
              <button className="btn-change-vote" onClick={() => setView('pick-member')}>
                Cast Your Vote
              </button>
            )}
            {selectedMember && (
              <button className="btn-change-member" onClick={handleChangeMember}>Not you?</button>
            )}
          </div>
        </>
      ) : (
        <>
          {/* Member picker */}
          <div className="survey-member-section">
            {selectedMember ? (
              <div className="survey-selected-member">
                <span className="selected-member-label">Voting as</span>
                <span
                  className="selected-member-name"
                  style={{ borderLeftColor: getBranchColor(selectedMember.branchName) }}
                >
                  {selectedMember.name}
                  <span className="selected-member-branch">{selectedMember.branchName} branch</span>
                </span>
                <button className="btn-change-member" onClick={handleChangeMember}>Not you?</button>
              </div>
            ) : (
              <div className="survey-member-picker" ref={dropdownRef}>
                <label className="survey-member-label">Find your name to vote</label>
                <input
                  type="text"
                  className="survey-member-search"
                  value={search}
                  onChange={(e) => { setSearch(e.target.value); setDropdownOpen(true); }}
                  onFocus={() => setDropdownOpen(true)}
                  placeholder="Search by name..."
                  autoComplete="off"
                />
                {dropdownOpen && (
                  <div className="survey-member-dropdown">
                    {filtered.length === 0 ? (
                      <div className="member-dropdown-empty">No members found</div>
                    ) : (
                      sortedBranches.map(([branch, branchMembers]) => (
                        <div key={branch} className="member-dropdown-branch">
                          <div
                            className="member-dropdown-branch-label"
                            style={{ borderLeftColor: getBranchColor(branch) }}
                          >
                            {branch}
                          </div>
                          {branchMembers.map((m) => (
                            <button
                              key={m.id}
                              className="member-dropdown-item"
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
            )}
          </div>

          {!selectedMember ? (
            <>
              <div className="survey-prompt">Select your name above to start voting</div>
              <div className="design-preview-grid">
                {designs.map((design) => (
                  <button
                    key={design.id}
                    className="design-preview-card"
                    onClick={() => setZoomedDesign(design)}
                  >
                    <img src={design.imageUrl} alt={design.name} />
                    <span className="design-card-name">{design.name}</span>
                  </button>
                ))}
              </div>
              {totalVotes > 0 && (
                <div className="survey-view-results-section">
                  <button className="btn-view-results" onClick={() => setView('results')}>
                    View Current Results
                  </button>
                </div>
              )}
            </>
          ) : (
            <>
              <div className="design-vote-grid">
                {designs.map((design) => (
                  <div
                    key={design.id}
                    className={`design-vote-card ${selectedDesignId === design.id ? 'selected' : ''}`}
                  >
                    <button
                      className="design-vote-image"
                      onClick={() => setZoomedDesign(design)}
                      aria-label={`View ${design.name} full size`}
                    >
                      <img src={design.imageUrl} alt={design.name} />
                      <span className="design-zoom-hint">🔍 Tap to enlarge</span>
                    </button>
                    <button
                      className="design-vote-select"
                      onClick={() => setSelectedDesignId(design.id)}
                    >
                      <span className="slogan-radio">
                        {selectedDesignId === design.id ? (
                          <span className="radio-checked" />
                        ) : (
                          <span className="radio-unchecked" />
                        )}
                      </span>
                      <span className="slogan-text">{design.name}</span>
                    </button>
                  </div>
                ))}
              </div>

              <div className="survey-submit-section">
                <button
                  className="btn-submit-vote"
                  onClick={handleVote}
                  disabled={submitting || selectedDesignId === null}
                >
                  {submitting ? 'Submitting...' : hasVoted ? 'Update My Vote' : 'Submit My Vote'}
                </button>
                <button className="btn-view-results-link" onClick={() => { load(); setView('results'); }}>
                  View Results
                </button>
              </div>
            </>
          )}
        </>
      )}

      {zoomedDesign && (
        <div className="design-lightbox" onClick={() => setZoomedDesign(null)}>
          <button className="design-lightbox-close" aria-label="Close">&times;</button>
          <img src={zoomedDesign.imageUrl} alt={zoomedDesign.name} onClick={(e) => e.stopPropagation()} />
          <div className="design-lightbox-caption">{zoomedDesign.name}</div>
        </div>
      )}
    </div>
  );
}
