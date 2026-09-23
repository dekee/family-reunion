import { useEffect, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { fetchAngelContributors } from '../api';
import { ANGEL_GOAL } from '../constants/angelFund';
import AngelGiveForm from './AngelGiveForm';
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
  const [showGive, setShowGive] = useState(false);
  const [searchParams] = useSearchParams();
  const paymentStatus = searchParams.get('payment');

  useEffect(() => {
    fetchAngelContributors()
      .then(setAngels)
      .catch(console.error)
      .finally(() => setLoading(false));
  }, []);

  const totalContributed = angels.reduce((s, a) => s + a.amount, 0);
  const pct = Math.min(100, Math.round((totalContributed / ANGEL_GOAL) * 100));
  const goalReached = totalContributed >= ANGEL_GOAL;

  return (
    <div className="thankyou-page">
      <div className="page-header">
        <h2>Thank You</h2>
        <p>Recognizing the generous family members whose contributions help others participate in the reunion</p>
      </div>

      {paymentStatus === 'success' && (
        <div className="thankyou-banner thankyou-banner-success">
          <strong>Thank you! Your Angel Fund gift is on its way.</strong>
          {/* The Stripe webhook is what completes the payment, so the leaderboard below may lag. */}
          <span>Your name will appear below within a few moments.</span>
        </div>
      )}
      {paymentStatus === 'cancelled' && (
        <div className="thankyou-banner thankyou-banner-cancelled">
          Your gift was cancelled — nothing was charged.
        </div>
      )}

      <div className="thankyou-hero" style={{ backgroundImage: 'url(/angel-contributor.png)' }}>
        <div className="thankyou-hero-overlay">
          <h3 className="thankyou-hero-title">Angel Contributors</h3>
          <p className="thankyou-hero-desc">
            Angel contributors give so that family members who can't cover the cost of
            attendance can still be there. Any amount helps, whether or not you're paying
            fees of your own — their generosity is what brings the whole Tumblin family together.
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

      <div className="thankyou-give-section">
        {showGive ? (
          <AngelGiveForm variant="standalone" onCancel={() => setShowGive(false)} />
        ) : (
          <button className="thankyou-give-btn" onClick={() => setShowGive(true)}>
            Give to the Angel Fund
          </button>
        )}
      </div>

      {loading ? (
        <div className="thankyou-grid">
          {Array.from({ length: 3 }).map((_, i) => <SkeletonCard key={i} lines={2} />)}
        </div>
      ) : angels.length === 0 ? (
        <div className="thankyou-empty">
          <div className="thankyou-empty-icon">&#127873;</div>
          <h3>Be the first angel contributor</h3>
          <p>
            Give any amount to help cover reunion fees for family members who need
            support. You don't need to be paying for anyone else &mdash; use
            <strong> Give to the Angel Fund</strong> above.
          </p>
        </div>
      ) : (
        <div className="thankyou-grid">
          {angels.map((a, i) => (
            <div key={i} className="thankyou-card">
              <div className="thankyou-card-badge">Angel</div>
              <span className="thankyou-card-name">{a.payerName}</span>
              {a.familyName && <span className="thankyou-card-family">{a.familyName} Family</span>}
              <span className="thankyou-card-amount">{dollars(a.amount)}</span>
              <span className="thankyou-card-date">{formatDate(a.date)}</span>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
