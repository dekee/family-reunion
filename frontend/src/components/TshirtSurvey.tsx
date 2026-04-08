import { useEffect, useState, useRef } from 'react';
import { fetchSlogans, voteForSlogan, fetchMyVote, fetchFamilyTree } from '../api';
import { useToast } from './Toast';
import { getBranchColor } from '../branchColors';
import type { SloganResponse, FamilyTreeNode } from '../types';
import './TshirtSurvey.css';

interface FlatMember {
  id: number;
  name: string;
  branchName: string;
}

function flattenTree(nodes: FamilyTreeNode[]): FlatMember[] {
  const result: FlatMember[] = [];
  function walk(n: FamilyTreeNode, branch: string) {
    result.push({ id: n.id, name: n.name, branchName: branch });
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

export default function TshirtSurvey() {
  const [slogans, setSlogans] = useState<SloganResponse[]>([]);
  const [members, setMembers] = useState<FlatMember[]>([]);
  const [loading, setLoading] = useState(true);
  const [selectedMemberId, setSelectedMemberId] = useState<number | null>(() => {
    const saved = localStorage.getItem('slogan_member_id');
    return saved ? Number(saved) : null;
  });
  const [search, setSearch] = useState('');
  const [dropdownOpen, setDropdownOpen] = useState(false);
  const [selectedSloganId, setSelectedSloganId] = useState<number | null>(null);
  const [hasVoted, setHasVoted] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [showResults, setShowResults] = useState(false);
  const dropdownRef = useRef<HTMLDivElement>(null);
  const { showToast } = useToast();

  const load = () => {
    setLoading(true);
    Promise.all([fetchSlogans(), fetchFamilyTree()])
      .then(([s, tree]) => {
        setSlogans(s);
        setMembers(flattenTree(tree.roots));
      })
      .catch(() => showToast('Failed to load data', 'error'))
      .finally(() => setLoading(false));
  };

  useEffect(() => { load(); }, []);

  // Restore previous vote
  useEffect(() => {
    const savedId = localStorage.getItem('slogan_member_id');
    if (savedId) {
      fetchMyVote(Number(savedId)).then((result) => {
        if (result.sloganId) {
          setSelectedSloganId(result.sloganId);
          setHasVoted(true);
          setShowResults(true);
        }
      }).catch(() => {});
    }
  }, []);

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

  const totalVotes = slogans.reduce((sum, s) => sum + s.voteCount, 0);
  const maxVotes = Math.max(...slogans.map((s) => s.voteCount), 1);

  const handleSelectMember = (member: FlatMember) => {
    setSelectedMemberId(member.id);
    setSearch('');
    setDropdownOpen(false);
    // Check if this member already voted
    fetchMyVote(member.id).then((result) => {
      if (result.sloganId) {
        setSelectedSloganId(result.sloganId);
        setHasVoted(true);
        setShowResults(true);
      } else {
        setSelectedSloganId(null);
        setHasVoted(false);
        setShowResults(false);
      }
    }).catch(() => {});
  };

  const handleVote = async () => {
    if (selectedMemberId === null) {
      showToast('Please select your name', 'error');
      return;
    }
    if (selectedSloganId === null) {
      showToast('Please select a slogan', 'error');
      return;
    }

    setSubmitting(true);
    try {
      await voteForSlogan({ sloganId: selectedSloganId, familyMemberId: selectedMemberId });
      localStorage.setItem('slogan_member_id', String(selectedMemberId));
      setHasVoted(true);
      setShowResults(true);
      showToast('Vote submitted! Thank you');
      load();
    } catch (err: any) {
      showToast(err.message || 'Failed to submit vote', 'error');
    } finally {
      setSubmitting(false);
    }
  };

  const handleChangeVote = () => {
    setHasVoted(false);
    setShowResults(false);
  };

  const handleChangeMember = () => {
    setSelectedMemberId(null);
    setSelectedSloganId(null);
    setHasVoted(false);
    setShowResults(false);
    localStorage.removeItem('slogan_member_id');
  };

  return (
    <div className="survey-page">
      <div className="page-header">
        <h2>T-Shirt Slogan Survey</h2>
        <p>Help us pick the perfect slogan for our family reunion t-shirts!</p>
      </div>

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
            {!hasVoted && (
              <button className="btn-change-member" onClick={handleChangeMember}>Change</button>
            )}
          </div>
        ) : (
          <div className="survey-member-picker" ref={dropdownRef}>
            <label className="survey-member-label">Find your name</label>
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

      {loading ? (
        <div className="survey-loading">Loading slogans...</div>
      ) : !selectedMember ? (
        <div className="survey-prompt">Select your name above to start voting</div>
      ) : showResults ? (
        <div className="survey-results">
          <div className="survey-results-header">
            <h3>Results ({totalVotes} vote{totalVotes !== 1 ? 's' : ''})</h3>
            <button className="btn-change-vote" onClick={handleChangeVote}>
              Change My Vote
            </button>
          </div>
          <div className="survey-result-cards">
            {[...slogans]
              .sort((a, b) => b.voteCount - a.voteCount)
              .map((slogan, index) => {
                const pct = totalVotes > 0 ? (slogan.voteCount / totalVotes) * 100 : 0;
                const isMyVote = slogan.id === selectedSloganId;
                return (
                  <div
                    key={slogan.id}
                    className={`survey-result-card ${isMyVote ? 'my-vote' : ''} ${index === 0 && slogan.voteCount > 0 ? 'leading' : ''}`}
                  >
                    <div className="result-bar" style={{ width: `${(slogan.voteCount / maxVotes) * 100}%` }} />
                    <div className="result-content">
                      <div className="result-slogan">
                        {isMyVote && <span className="my-vote-badge">Your vote</span>}
                        <span className="result-text">{slogan.slogan}</span>
                      </div>
                      <div className="result-stats">
                        <span className="result-count">{slogan.voteCount}</span>
                        <span className="result-pct">{pct.toFixed(0)}%</span>
                      </div>
                    </div>
                  </div>
                );
              })}
          </div>
        </div>
      ) : (
        <>
          <div className="survey-slogan-grid">
            {slogans.map((slogan) => (
              <button
                key={slogan.id}
                className={`survey-slogan-card ${selectedSloganId === slogan.id ? 'selected' : ''}`}
                onClick={() => setSelectedSloganId(slogan.id)}
              >
                <span className="slogan-radio">
                  {selectedSloganId === slogan.id ? (
                    <span className="radio-checked" />
                  ) : (
                    <span className="radio-unchecked" />
                  )}
                </span>
                <span className="slogan-text">{slogan.slogan}</span>
              </button>
            ))}
          </div>

          <div className="survey-submit-section">
            <button
              className="btn-submit-vote"
              onClick={handleVote}
              disabled={submitting || selectedMemberId === null || selectedSloganId === null}
            >
              {submitting ? 'Submitting...' : 'Submit My Vote'}
            </button>
          </div>
        </>
      )}
    </div>
  );
}
