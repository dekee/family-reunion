import { useState } from 'react';
import { createDonationCheckout } from '../api';
import { ANGEL_MIN_DOLLARS, ANGEL_MAX_DOLLARS } from '../constants/angelFund';
import { dollars } from '../utils/formatting';
import './AngelGiveForm.css';

interface Props {
  /** Styling only — 'promo' sits on the navy card on the pay page. */
  variant?: 'standalone' | 'promo';
  onCancel?: () => void;
}

/**
 * Standalone Angel Fund gift. Used inline on both /thank-you and the /pay promo card, and
 * deliberately independent of paying fees for anyone: no RSVP, no member selection, no T-shirt size.
 */
export default function AngelGiveForm({ variant = 'standalone', onCancel }: Props) {
  const [amount, setAmount] = useState('');
  const [donorName, setDonorName] = useState('');
  const [familyLabel, setFamilyLabel] = useState('');
  const [anonymous, setAnonymous] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');

  const giftDollars = parseFloat(amount) || 0;
  const amountValid = giftDollars >= ANGEL_MIN_DOLLARS && giftDollars <= ANGEL_MAX_DOLLARS;
  const nameValid = anonymous || donorName.trim().length > 0;
  const canSubmit = amountValid && nameValid && !submitting;

  const handleSubmit = async () => {
    // Gate here rather than leaning on the server: its validation errors are plain strings, but a
    // donor should not be meeting them for something the form can see.
    if (!amountValid) {
      setError(`Please enter an amount between ${dollars(ANGEL_MIN_DOLLARS)} and ${dollars(ANGEL_MAX_DOLLARS)}.`);
      return;
    }
    if (!nameValid) {
      setError('Add the name to credit, or choose to give anonymously.');
      return;
    }
    // Stays true through the redirect, so a double-click can't open two Stripe sessions.
    setSubmitting(true);
    setError('');
    try {
      const { url } = await createDonationCheckout({
        amountCents: Math.round(giftDollars * 100),
        donorName: anonymous ? undefined : donorName.trim(),
        familyLabel: anonymous || !familyLabel.trim() ? undefined : familyLabel.trim(),
        anonymous,
      });
      window.location.href = url;
    } catch (err: any) {
      setError(err.message || 'Could not start checkout. Please try again.');
      setSubmitting(false);
    }
  };

  return (
    <div className={`angel-give angel-give-${variant}`}>
      <div className="angel-give-intro">
        <span className="angel-give-title">Give to the Angel Fund</span>
        <p>
          Your gift helps cover reunion fees for family members who can't manage the cost.
          You don't need to be paying for anyone else to give.
        </p>
      </div>

      <label className="angel-give-label" htmlFor="angel-give-amount">Amount</label>
      <div className="angel-give-amount-row">
        <span className="angel-give-dollar-sign">$</span>
        <input
          id="angel-give-amount"
          type="number"
          min="1"
          step="1"
          placeholder="0"
          value={amount}
          onChange={e => setAmount(e.target.value)}
          className="angel-give-amount-input"
          onKeyDown={e => { if (e.key === 'Escape' && onCancel) onCancel(); }}
        />
      </div>

      <label className="angel-give-label" htmlFor="angel-give-name">Name to credit</label>
      <input
        id="angel-give-name"
        type="text"
        maxLength={80}
        placeholder="Your name"
        value={donorName}
        disabled={anonymous}
        onChange={e => setDonorName(e.target.value)}
        className="angel-give-text-input"
      />

      <label className="angel-give-label" htmlFor="angel-give-family">Family branch (optional)</label>
      <input
        id="angel-give-family"
        type="text"
        maxLength={80}
        placeholder="e.g. Norris"
        value={familyLabel}
        disabled={anonymous}
        onChange={e => setFamilyLabel(e.target.value)}
        className="angel-give-text-input"
      />

      <label className="angel-give-anon">
        <input
          type="checkbox"
          checked={anonymous}
          onChange={e => setAnonymous(e.target.checked)}
        />
        <span>Give anonymously</span>
      </label>
      <p className="angel-give-anon-note">
        Reunion admins can still see who gave — only the public Thank You page hides your name.
      </p>

      {error && <p className="angel-give-error">{error}</p>}

      <div className="angel-give-actions">
        <button className="angel-give-submit" onClick={handleSubmit} disabled={!canSubmit}>
          {submitting
            ? 'Redirecting...'
            : giftDollars > 0 ? `Give ${dollars(giftDollars)}` : 'Give'}
        </button>
        {onCancel && (
          <button className="angel-give-cancel" onClick={onCancel} disabled={submitting}>
            Cancel
          </button>
        )}
      </div>
    </div>
  );
}
