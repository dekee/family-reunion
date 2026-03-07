import { useEffect, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { fetchFamilyTree, fetchPaymentSummaries, createCheckoutSession } from '../api';
import type { FamilyTreeNode, PaymentSummaryResponse } from '../types';
import { SkeletonCard, SkeletonTable } from './Skeleton';
import './Budget.css';

const ADULT_FEE = 100; // includes spouses
const CHILD_FEE = 50;

function dollars(n: number): string {
  return '$' + n.toLocaleString();
}

interface MemberCounts {
  totalMembers: number;
  adultCount: number;
  childCount: number;
  infantCount: number;
}

function countMembers(nodes: FamilyTreeNode[]): MemberCounts {
  const counts: MemberCounts = { totalMembers: 0, adultCount: 0, childCount: 0, infantCount: 0 };
  function walk(node: FamilyTreeNode) {
    counts.totalMembers++;
    if (node.ageGroup === 'ADULT' || node.ageGroup === 'SPOUSE') counts.adultCount++;
    else if (node.ageGroup === 'CHILD') counts.childCount++;
    else if (node.ageGroup === 'INFANT') counts.infantCount++;
    node.children.forEach(walk);
  }
  nodes.forEach(walk);
  return counts;
}

export default function Budget() {
  const [memberCounts, setMemberCounts] = useState<MemberCounts | null>(null);
  const [payments, setPayments] = useState<PaymentSummaryResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [payingRsvpId, setPayingRsvpId] = useState<number | null>(null);
  const [payAmount, setPayAmount] = useState('');
  const [error, setError] = useState('');
  const [searchParams] = useSearchParams();

  const paymentStatus = searchParams.get('payment');

  const load = () => {
    setLoading(true);
    Promise.all([fetchFamilyTree(), fetchPaymentSummaries()])
      .then(([tree, p]) => {
        setMemberCounts(countMembers(tree.roots));
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
  if (!memberCounts) return <p>Unable to load member data.</p>;

  const adultTotal = memberCounts.adultCount * ADULT_FEE;
  const childTotal = memberCounts.childCount * CHILD_FEE;
  const grandTotal = adultTotal + childTotal;

  return (
    <div className="budget-page">
      <div className="page-header">
        <h2>Reunion Budget</h2>
        <p>Cost estimates based on family members</p>
      </div>

      {paymentStatus === 'success' && (
        <div className="payment-banner payment-success">Payment successful! Thank you.</div>
      )}
      {paymentStatus === 'cancelled' && (
        <div className="payment-banner payment-cancelled">Payment was cancelled.</div>
      )}

      <div className="budget-summary-grid">
        <div className="budget-summary-card">
          <span className="budget-summary-number">{memberCounts.totalMembers}</span>
          <span className="budget-summary-label">Total Members</span>
        </div>
        <div className="budget-summary-card">
          <span className="budget-summary-number">{memberCounts.adultCount}</span>
          <span className="budget-summary-label">Adults / Spouses</span>
        </div>
        <div className="budget-summary-card">
          <span className="budget-summary-number">{memberCounts.childCount}</span>
          <span className="budget-summary-label">Children</span>
        </div>
        <div className="budget-summary-card">
          <span className="budget-summary-number">{memberCounts.infantCount}</span>
          <span className="budget-summary-label">Infants (free)</span>
        </div>
      </div>

      <h2>Cost Breakdown</h2>
      <div className="budget-breakdown">
        <div className="budget-row">
          <div className="budget-row-label">
            <span className="budget-row-title">Adults / Spouses</span>
            <span className="budget-row-detail">
              {memberCounts.adultCount} x {dollars(ADULT_FEE)} per person
            </span>
          </div>
          <span className="budget-row-range">{dollars(adultTotal)}</span>
        </div>

        <div className="budget-row">
          <div className="budget-row-label">
            <span className="budget-row-title">Children</span>
            <span className="budget-row-detail">
              {memberCounts.childCount} x {dollars(CHILD_FEE)} per person
            </span>
          </div>
          <span className="budget-row-range">{dollars(childTotal)}</span>
        </div>

        <div className="budget-row">
          <div className="budget-row-label">
            <span className="budget-row-title">Infants</span>
            <span className="budget-row-detail">{memberCounts.infantCount} — no cost</span>
          </div>
          <span className="budget-row-range">$0</span>
        </div>

        <div className="budget-total">
          <div className="budget-row-label">
            <span className="budget-row-title">Total</span>
            <span className="budget-row-detail">
              {memberCounts.totalMembers} family members
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
