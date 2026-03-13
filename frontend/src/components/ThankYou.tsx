import { useEffect, useState } from 'react';
import { fetchAngelContributors } from '../api';
import { dollars } from '../utils/formatting';
import type { AngelContributor } from '../types';
import { SkeletonCard } from './Skeleton';
import './ThankYou.css';

function formatDate(iso: string): string {
  const d = new Date(iso + 'T00:00:00');
  return d.toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' });
}

export default function ThankYou() {
  const [angels, setAngels] = useState<AngelContributor[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchAngelContributors()
      .then(setAngels)
      .catch(console.error)
      .finally(() => setLoading(false));
  }, []);

  const ANGEL_GOAL = 2000;
  const totalContributed = angels.reduce((s, a) => s + a.amount, 0);
  const pct = Math.min(100, Math.round((totalContributed / ANGEL_GOAL) * 100));
  const goalReached = totalContributed >= ANGEL_GOAL;

  return (
    <div className="thankyou-page">
      <div className="page-header">
        <h2>Thank You</h2>
        <p>Recognizing the generous family members whose contributions help others participate in the reunion</p>
      </div>

      <div className="thankyou-hero" style={{ backgroundImage: 'url(/angel-contributor.png)' }}>
        <div className="thankyou-hero-overlay">
          <h3 className="thankyou-hero-title">Angel Contributors</h3>
          <p className="thankyou-hero-desc">
            Angel contributors donate beyond their own fees to help enable family members
            who may not be able to cover the cost of attendance. Their generosity ensures
            that everyone in the Tumblin family can come together.
          </p>
        </div>
      </div>

      {/* Thermometer */}
      {!loading && (
        <div className="thermo-section">
          <div className="thermo-header">
            <span className="thermo-raised">{dollars(totalContributed)} raised</span>
            <span className="thermo-goal">Goal: {dollars(ANGEL_GOAL)}</span>
          </div>
          <div className="thermo-track">
            <div
              className={`thermo-fill ${goalReached ? 'thermo-fill-complete' : ''}`}
              style={{ width: `${Math.max(pct, 2)}%` }}
            />
            <div className="thermo-bulb" />
          </div>
          <div className="thermo-footer">
            <span className="thermo-pct">{pct}%</span>
            <span className="thermo-remaining">
              {goalReached ? 'Goal reached!' : `${dollars(ANGEL_GOAL - totalContributed)} to go`}
            </span>
          </div>
          {angels.length > 0 && (
            <div className="thermo-angel-count">
              {angels.length} angel contributor{angels.length !== 1 ? 's' : ''}
            </div>
          )}
        </div>
      )}

      {loading ? (
        <div className="thankyou-grid">
          {Array.from({ length: 3 }).map((_, i) => <SkeletonCard key={i} lines={2} />)}
        </div>
      ) : angels.length === 0 ? (
        <div className="thankyou-empty">
          <div className="thankyou-empty-icon">&#127873;</div>
          <h3>Be the first angel contributor</h3>
          <p>
            When paying for your family branch,
            click <strong>"Become an Angel Contributor"</strong> to add an extra
            donation that helps cover fees for family members who need support.
          </p>
        </div>
      ) : (
        <div className="thankyou-grid">
          {angels.map((a, i) => (
            <div key={i} className="thankyou-card">
              <div className="thankyou-card-badge">Angel</div>
              <span className="thankyou-card-name">{a.payerName}</span>
              <span className="thankyou-card-family">{a.familyName} Family</span>
              <span className="thankyou-card-amount">{dollars(a.amount)}</span>
              <span className="thankyou-card-date">{formatDate(a.date)}</span>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
