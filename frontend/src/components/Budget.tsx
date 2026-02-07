import { useEffect, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { fetchSummary, fetchPaymentSummaries, createCheckoutSession } from '../api';
import type { RsvpSummaryResponse, PaymentSummaryResponse } from '../types';
import { SkeletonCard, SkeletonTable } from './Skeleton';
import './Budget.css';

const ADULT_FEE = 100; // includes spouses
const CHILD_FEE = 50;

function dollars(n: number): string {
  return '$' + n.toLocaleString();
}

export default function Budget() {
  const [summary, setSummary] = useState<RsvpSummaryResponse | null>(null);
  const [payments, setPayments] = useState<PaymentSummaryResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [payingRsvpId, setPayingRsvpId] = useState<number | null>(null);
  const [payAmount, setPayAmount] = useState('');
  const [error, setError] = useState('');
  const [searchParams] = useSearchParams();

  const paymentStatus = searchParams.get('payment');

  const load = () => {
    setLoading(true);
    Promise.all([fetchSummary(), fetchPaymentSummaries()])
      .then(([s, p]) => {
        setSummary(s);
        setPayments(p);
      })
      .catch(console.error)
      .finally(() => setLoading(false));
  };

  useEffect(() => { load(); }, []);

  const handlePay = async (rsvpId: number) => {
    setError('');
    const cents = Math.round(parseFloat(payAmount) * 100);
    if (isNaN(cents) || cents < 100) {
      setError('Please enter a valid amount (minimum $1.00)');
      return;
    }
    try {
      const { url } = await createCheckoutSession({ rsvpId, amount: cents });
      window.location.href = url;
    } catch (err: any) {
      setError(err.message || 'Failed to start checkout');
    }
  };

  if (loading) return (
    <div className="budget-page">
      <div className="page-header"><h2>Reunion Budget</h2><p>Cost estimates and payment tracking</p></div>
      <div className="budget-summary-grid">
        {Array.from({ length: 6 }).map((_, i) => <SkeletonCard key={i} lines={1} />)}
      </div>
      <SkeletonTable rows={4} cols={6} />
    </div>
  );
  if (!summary) return <p>Unable to load summary data.</p>;

  const adultTotal = summary.adultCount * ADULT_FEE;
  const childTotal = summary.childCount * CHILD_FEE;
  const grandTotal = adultTotal + childTotal;

  return (
    <div className="budget-page">
      <div className="page-header">
        <h2>Reunion Budget</h2>
        <p>Cost estimates and payment tracking</p>
      </div>

      {paymentStatus === 'success' && (
        <div className="payment-banner payment-success">Payment successful! Thank you.</div>
      )}
      {paymentStatus === 'cancelled' && (
        <div className="payment-banner payment-cancelled">Payment was cancelled.</div>
      )}

      <div className="budget-summary-grid">
        <div className="budget-summary-card">
          <span className="budget-summary-number">{summary.totalFamilies}</span>
          <span className="budget-summary-label">Families</span>
        </div>
        <div className="budget-summary-card">
          <span className="budget-summary-number">{summary.totalHeadcount}</span>
          <span className="budget-summary-label">Total Guests</span>
        </div>
        <div className="budget-summary-card">
          <span className="budget-summary-number">{summary.adultCount}</span>
          <span className="budget-summary-label">Adults</span>
        </div>
        <div className="budget-summary-card">
          <span className="budget-summary-number">{summary.childCount}</span>
          <span className="budget-summary-label">Children</span>
        </div>
        <div className="budget-summary-card">
          <span className="budget-summary-number">{summary.infantCount}</span>
          <span className="budget-summary-label">Infants</span>
        </div>
        <div className="budget-summary-card">
          <span className="budget-summary-number">{summary.lodgingCount}</span>
          <span className="budget-summary-label">Need Lodging</span>
        </div>
      </div>

      <h2>Cost Breakdown</h2>
      <div className="budget-breakdown">
        <div className="budget-row">
          <div className="budget-row-label">
            <span className="budget-row-title">Adults / Spouses</span>
            <span className="budget-row-detail">
              {summary.adultCount} x {dollars(ADULT_FEE)} per person
            </span>
          </div>
          <span className="budget-row-range">{dollars(adultTotal)}</span>
        </div>

        <div className="budget-row">
          <div className="budget-row-label">
            <span className="budget-row-title">Children</span>
            <span className="budget-row-detail">
              {summary.childCount} x {dollars(CHILD_FEE)} per person
            </span>
          </div>
          <span className="budget-row-range">{dollars(childTotal)}</span>
        </div>

        <div className="budget-row">
          <div className="budget-row-label">
            <span className="budget-row-title">Infants</span>
            <span className="budget-row-detail">{summary.infantCount} — no cost</span>
          </div>
          <span className="budget-row-range">$0</span>
        </div>

        <div className="budget-total">
          <div className="budget-row-label">
            <span className="budget-row-title">Total</span>
            <span className="budget-row-detail">
              {summary.totalHeadcount} guests across {summary.totalFamilies} families
            </span>
          </div>
          <span className="budget-row-range">{dollars(grandTotal)}</span>
        </div>
      </div>

      <h2>Payment Tracker</h2>
      {payments.length === 0 ? (
        <p className="payment-empty">No RSVPs to track payments for yet.</p>
      ) : (
        <div className="payment-table-wrapper">
          <table className="payment-table">
            <thead>
              <tr>
                <th>Family</th>
                <th>Owed</th>
                <th>Paid</th>
                <th>Balance</th>
                <th>Status</th>
                <th>Action</th>
              </tr>
            </thead>
            <tbody>
              {payments.map((p) => (
                <tr key={p.rsvpId}>
                  <td className="payment-family">{p.familyName}</td>
                  <td>{dollars(p.totalOwed)}</td>
                  <td>{dollars(p.totalPaid)}</td>
                  <td className={p.balance > 0 ? 'payment-balance-due' : p.balance < 0 ? 'payment-balance-credit' : ''}>
                    {p.balance < 0 ? `($${Math.abs(p.balance).toLocaleString()} credit)` : dollars(p.balance)}
                  </td>
                  <td>
                    <span className={`payment-status-badge ${p.balance < 0 ? 'status-overpaid' : `status-${p.status.toLowerCase()}`}`}>
                      {p.balance < 0 ? 'Overpaid' : p.status === 'PAID' ? 'Paid' : p.status === 'PARTIAL' ? 'Partial' : p.status === 'PENDING' ? 'Pending' : 'Unpaid'}
                    </span>
                  </td>
                  <td>
                    {p.balance > 0 && (
                      payingRsvpId === p.rsvpId ? (
                        <div className="pay-inline">
                          <input
                            type="number"
                            step="0.01"
                            min="1"
                            value={payAmount}
                            onChange={(e) => setPayAmount(e.target.value)}
                            placeholder={p.balance.toFixed(2)}
                            className="pay-amount-input"
                          />
                          <button className="btn-pay-confirm" onClick={() => handlePay(p.rsvpId)}>
                            Pay
                          </button>
                          <button className="btn-pay-cancel" onClick={() => { setPayingRsvpId(null); setError(''); }}>
                            Cancel
                          </button>
                        </div>
                      ) : (
                        <button
                          className="btn-pay"
                          onClick={() => {
                            setPayingRsvpId(p.rsvpId);
                            setPayAmount(p.balance.toFixed(2));
                            setError('');
                          }}
                        >
                          Pay Now
                        </button>
                      )
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          {error && <p className="payment-error">{error}</p>}
        </div>
      )}
    </div>
  );
}
