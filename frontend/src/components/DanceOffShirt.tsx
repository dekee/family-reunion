import { useEffect, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { fetchFees, createDanceOffShirtCheckout } from '../api';
import { setFees, danceOffShirtCents, danceOffShirtsEnabled } from '../constants/ageGroups';
import type { TshirtSize } from '../types';
import SizeSelect from './SizeSelect';
import './DanceOffShirt.css';

const DESIGN_SRC = '/designs/sister-dance-off.jpg';
const DESIGN_ALT =
  'Sister Dance Off shirt design: Priscilla, Cheryl, Gail, Kendra, Myra, Penny, Tonya and Ingrid ' +
  'dancing on a stage under the words "Sister Dance Off — Who Won?"';

/** Mirrors the server's caps (PaymentService.MAX_DANCE_OFF_*), so the form never offers a refusal. */
const MAX_PER_LINE = 10;
const MAX_SHIRTS = 20;

interface ShirtRow {
  id: number;
  size: TshirtSize | '';
  quantity: number;
  wearer: string;
}

let nextRowId = 1;
const newRow = (): ShirtRow => ({ id: nextRowId++, size: '', quantity: 1, wearer: '' });

/** Cents to "$26.50". Prices are cents end to end so a total never drifts by a penny. */
const money = (cents: number) => `$${(cents / 100).toFixed(2)}`;

export default function DanceOffShirt() {
  const [searchParams] = useSearchParams();
  const paymentStatus = searchParams.get('payment');

  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState('');
  const [buyerName, setBuyerName] = useState('');
  const [rows, setRows] = useState<ShirtRow[]>(() => [newRow()]);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');
  const [zoomed, setZoomed] = useState(false);

  useEffect(() => {
    fetchFees()
      .then(fees => setFees(fees))
      .catch(() => setLoadError('Could not load the shirt price. Please refresh the page.'))
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => {
    if (!zoomed) return;
    const handler = (e: KeyboardEvent) => {
      if (e.key === 'Escape') setZoomed(false);
    };
    document.addEventListener('keydown', handler);
    return () => document.removeEventListener('keydown', handler);
  }, [zoomed]);

  const priceCents = danceOffShirtCents();
  const open = danceOffShirtsEnabled();
  const shirtCount = rows.reduce((n, r) => n + r.quantity, 0);
  const totalCents = shirtCount * priceCents;
  const missingSize = rows.some(r => !r.size);
  const tooMany = shirtCount > MAX_SHIRTS;
  const missingName = buyerName.trim() === '';
  const canPay = !submitting && !missingName && !missingSize && shirtCount > 0 && !tooMany;
  const hint =
    missingName && missingSize ? 'Add your name and pick a size for every shirt to continue.'
    : missingName ? 'Add your name to continue.'
    : missingSize ? 'Pick a size for every shirt to continue.'
    : '';

  const updateRow = (id: number, patch: Partial<ShirtRow>) =>
    setRows(rs => rs.map(r => (r.id === id ? { ...r, ...patch } : r)));
  const removeRow = (id: number) => setRows(rs => (rs.length > 1 ? rs.filter(r => r.id !== id) : rs));

  const pay = async () => {
    setError('');
    setSubmitting(true);
    try {
      const { url } = await createDanceOffShirtCheckout({
        buyerName: buyerName.trim(),
        totalCents,
        items: rows.map(r => ({
          tshirtSize: r.size as TshirtSize,
          quantity: r.quantity,
          ...(r.wearer.trim() ? { wearerName: r.wearer.trim() } : {}),
        })),
      });
      window.location.href = url;
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Something went wrong. Please try again.');
      setSubmitting(false);
    }
  };

  return (
    <div className="dance-page">
      <h2 className="dance-title">Sister Dance Off T-Shirt</h2>
      <p className="dance-subtitle">
        Eight sisters, one stage. Priscilla, Cheryl, Gail, Kendra, Myra, Penny, Tonya and Ingrid,
        and the question nobody has settled: who won?
      </p>

      {paymentStatus === 'success' && (
        <div className="dance-banner dance-banner-success" role="status">
          Your order is in. Pick up your shirts at the fish fry at Derrick&rsquo;s.
        </div>
      )}
      {paymentStatus === 'cancelled' && (
        <div className="dance-banner dance-banner-cancelled" role="status">
          Checkout was cancelled and nothing was charged. Your order is still here if you want it.
        </div>
      )}

      <div className="dance-layout">
        <button
          className="dance-design"
          onClick={() => setZoomed(true)}
          aria-label="View the Sister Dance Off design full size"
        >
          <img src={DESIGN_SRC} alt={DESIGN_ALT} />
          <span className="dance-design-hint">Tap to see it full size</span>
        </button>

        <section className="dance-order" aria-labelledby="dance-order-heading">
          <h3 id="dance-order-heading">Order yours</h3>

          {loading ? (
            <p className="dance-muted">Loading&hellip;</p>
          ) : loadError ? (
            <p className="dance-error">{loadError}</p>
          ) : !open ? (
            <div className="dance-closed">
              <strong>Orders are closed right now.</strong>
              <p>Check back soon, or ask Derrick if you missed the order window.</p>
            </div>
          ) : (
            <>
              <p className="dance-price">
                <span className="dance-price-amount">{money(priceCents)}</span> per shirt
              </p>
              <p className="dance-pickup">Pick up at the fish fry at Derrick&rsquo;s. No shipping.</p>

              <label className="dance-field">
                <span>Your name</span>
                <input
                  type="text"
                  value={buyerName}
                  maxLength={80}
                  autoComplete="name"
                  placeholder="So we know whose shirts these are"
                  onChange={e => setBuyerName(e.target.value)}
                />
              </label>

              <div className="dance-rows">
                {rows.map((row, i) => (
                  <div className="dance-row" key={row.id}>
                    <SizeSelect
                      ageGroup="ADULT"
                      value={row.size}
                      onChange={size => updateRow(row.id, { size })}
                      placeholder="Size"
                      ariaLabel={`Size for shirt ${i + 1}`}
                      className="dance-size"
                    />
                    <select
                      className="dance-qty"
                      value={row.quantity}
                      onChange={e => updateRow(row.id, { quantity: Number(e.target.value) })}
                      aria-label={`How many of shirt ${i + 1}`}
                    >
                      {Array.from({ length: MAX_PER_LINE }, (_, n) => n + 1).map(n => (
                        <option key={n} value={n}>{n}</option>
                      ))}
                    </select>
                    <input
                      className="dance-wearer"
                      type="text"
                      value={row.wearer}
                      maxLength={80}
                      placeholder="Who it's for (optional)"
                      aria-label={`Who shirt ${i + 1} is for`}
                      onChange={e => updateRow(row.id, { wearer: e.target.value })}
                    />
                    {rows.length > 1 && (
                      <button
                        className="dance-remove"
                        onClick={() => removeRow(row.id)}
                        aria-label={`Remove shirt ${i + 1}`}
                      >
                        &times;
                      </button>
                    )}
                  </div>
                ))}
              </div>

              <button
                className="dance-add"
                onClick={() => setRows(rs => [...rs, newRow()])}
                disabled={shirtCount >= MAX_SHIRTS}
              >
                + Add another size
              </button>

              <div className="dance-total">
                <span>
                  {shirtCount} {shirtCount === 1 ? 'shirt' : 'shirts'}
                </span>
                <strong>{money(totalCents)}</strong>
              </div>

              {tooMany && (
                <p className="dance-error">
                  Online orders are capped at {MAX_SHIRTS} shirts. Ask Derrick about a bigger order.
                </p>
              )}
              {error && <p className="dance-error" role="alert">{error}</p>}

              <button className="dance-pay" onClick={pay} disabled={!canPay}>
                {submitting ? 'Opening checkout…' : `Pay ${money(totalCents)}`}
              </button>
              {hint && <p className="dance-hint">{hint}</p>}
              <p className="dance-hint">Secure checkout by Stripe.</p>
            </>
          )}
        </section>
      </div>

      {zoomed && (
        <div className="dance-lightbox" onClick={() => setZoomed(false)}>
          <button className="dance-lightbox-close" aria-label="Close">&times;</button>
          <img src={DESIGN_SRC} alt={DESIGN_ALT} />
        </div>
      )}
    </div>
  );
}
