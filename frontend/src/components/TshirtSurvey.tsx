import { useEffect, useState } from 'react';
import { fetchSlogans, voteForSlogan, fetchMyVote } from '../api';
import { useToast } from './Toast';
import type { SloganResponse } from '../types';
import './TshirtSurvey.css';

export default function TshirtSurvey() {
  const [slogans, setSlogans] = useState<SloganResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [voterName, setVoterName] = useState(() => localStorage.getItem('slogan_voter') || '');
  const [selectedId, setSelectedId] = useState<number | null>(null);
  const [hasVoted, setHasVoted] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [showResults, setShowResults] = useState(false);
  const { showToast } = useToast();

  const load = () => {
    setLoading(true);
    fetchSlogans()
      .then(setSlogans)
      .catch(() => showToast('Failed to load slogans', 'error'))
      .finally(() => setLoading(false));
  };

  useEffect(() => { load(); }, []);

  // If returning voter, restore their vote
  useEffect(() => {
    const saved = localStorage.getItem('slogan_voter');
    if (saved) {
      fetchMyVote(saved).then((result) => {
        if (result.sloganId) {
          setSelectedId(result.sloganId);
          setHasVoted(true);
          setShowResults(true);
        }
      }).catch(() => {});
    }
  }, []);

  const totalVotes = slogans.reduce((sum, s) => sum + s.voteCount, 0);
  const maxVotes = Math.max(...slogans.map((s) => s.voteCount), 1);

  const handleVote = async () => {
    if (!voterName.trim()) {
      showToast('Please enter your name', 'error');
      return;
    }
    if (selectedId === null) {
      showToast('Please select a slogan', 'error');
      return;
    }

    setSubmitting(true);
    try {
      await voteForSlogan({ sloganId: selectedId, voterName: voterName.trim() });
      localStorage.setItem('slogan_voter', voterName.trim());
      setHasVoted(true);
      setShowResults(true);
      showToast('Vote submitted! Thank you');
      load(); // Refresh vote counts
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

  return (
    <div className="survey-page">
      <div className="page-header">
        <h2>T-Shirt Slogan Survey</h2>
        <p>Help us pick the perfect slogan for our family reunion t-shirts!</p>
      </div>

      {!hasVoted && (
        <div className="survey-name-section">
          <label className="survey-name-label">
            Your Name
            <input
              type="text"
              className="survey-name-input"
              value={voterName}
              onChange={(e) => setVoterName(e.target.value)}
              placeholder="Enter your first and last name"
            />
          </label>
        </div>
      )}

      {loading ? (
        <div className="survey-loading">Loading slogans...</div>
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
                const isMyVote = slogan.id === selectedId;
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
                className={`survey-slogan-card ${selectedId === slogan.id ? 'selected' : ''}`}
                onClick={() => setSelectedId(slogan.id)}
              >
                <span className="slogan-radio">
                  {selectedId === slogan.id ? (
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
              disabled={submitting || !voterName.trim() || selectedId === null}
            >
              {submitting ? 'Submitting...' : 'Submit My Vote'}
            </button>
          </div>
        </>
      )}
    </div>
  );
}
