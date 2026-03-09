import { useEffect, useState } from 'react';
import { useSearchParams, Link } from 'react-router-dom';
import { fetchFamilyTree, fetchPaymentSummaries } from '../api';
import { ADULT_FEE, CHILD_FEE } from '../constants/ageGroups';
import { dollars } from '../utils/formatting';
import type { FamilyTreeNode, PaymentSummaryResponse } from '../types';
import { SkeletonCard } from './Skeleton';
import './Budget.css';

interface MemberCounts {
  totalMembers: number;
  adultCount: number;
  childCount: number;
  infantCount: number;
}

function countSubtree(node: FamilyTreeNode): MemberCounts {
  const counts: MemberCounts = { totalMembers: 0, adultCount: 0, childCount: 0, infantCount: 0 };
  function walk(n: FamilyTreeNode) {
    counts.totalMembers++;
    if (n.ageGroup === 'ADULT' || n.ageGroup === 'SPOUSE') counts.adultCount++;
    else if (n.ageGroup === 'CHILD') counts.childCount++;
    else if (n.ageGroup === 'INFANT') counts.infantCount++;
    n.children.forEach(walk);
  }
  walk(node);
  return counts;
}

function buildTotals(roots: FamilyTreeNode[]): MemberCounts {
  const totals: MemberCounts = { totalMembers: 0, adultCount: 0, childCount: 0, infantCount: 0 };
  for (const root of roots) {
    for (const branch of root.children) {
      const c = countSubtree(branch);
      totals.totalMembers += c.totalMembers;
      totals.adultCount += c.adultCount;
      totals.childCount += c.childCount;
      totals.infantCount += c.infantCount;
    }
  }
  return totals;
}

export default function Budget() {
  const [memberCounts, setMemberCounts] = useState<MemberCounts | null>(null);
  const [payments, setPayments] = useState<PaymentSummaryResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchParams] = useSearchParams();

  const paymentStatus = searchParams.get('payment');

  const load = () => {
    setLoading(true);
    Promise.all([fetchFamilyTree(), fetchPaymentSummaries()])
      .then(([tree, p]) => {
        setMemberCounts(buildTotals(tree.roots));
        setPayments(p);
      })
      .catch(console.error)
      .finally(() => setLoading(false));
  };

  useEffect(() => { load(); }, []);

  if (loading) return (
    <div className="budget-page">
      <div className="page-header"><h2>Reunion Budget</h2><p>Cost estimates based on family members</p></div>
      <div className="budget-summary-grid">
        {Array.from({ length: 4 }).map((_, i) => <SkeletonCard key={i} lines={1} />)}
      </div>
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
          <span className="budget-summary-label">Adults / Spouses (18+)</span>
        </div>
        <div className="budget-summary-card">
          <span className="budget-summary-number">{memberCounts.childCount}</span>
          <span className="budget-summary-label">Children (6–17)</span>
        </div>
        <div className="budget-summary-card">
          <span className="budget-summary-number">{memberCounts.infantCount}</span>
          <span className="budget-summary-label">Under 5 (free)</span>
        </div>
      </div>

      <h2>Cost Breakdown</h2>
      <div className="budget-breakdown">
        <div className="budget-row">
          <div className="budget-row-label">
            <span className="budget-row-title">Adults / Spouses (18+)</span>
            <span className="budget-row-detail">
              {memberCounts.adultCount} x {dollars(ADULT_FEE)} per person
            </span>
          </div>
          <span className="budget-row-range">{dollars(adultTotal)}</span>
        </div>

        <div className="budget-row">
          <div className="budget-row-label">
            <span className="budget-row-title">Children (6–17)</span>
            <span className="budget-row-detail">
              {memberCounts.childCount} x {dollars(CHILD_FEE)} per person
            </span>
          </div>
          <span className="budget-row-range">{dollars(childTotal)}</span>
        </div>

        <div className="budget-row">
          <div className="budget-row-label">
            <span className="budget-row-title">Under 5 (0–5)</span>
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

      <h2>Payment Summary</h2>
      {payments.length === 0 ? (
        <p className="payment-empty">No families to track payments for yet.</p>
      ) : (
        <>
          <div className="payment-summary-overview">
            <div className="budget-summary-grid">
              <div className="budget-summary-card">
                <span className="budget-summary-number">{dollars(payments.reduce((s, p) => s + p.totalPaid, 0))}</span>
                <span className="budget-summary-label">Total Collected</span>
              </div>
              <div className="budget-summary-card">
                <span className="budget-summary-number">{dollars(payments.reduce((s, p) => s + p.balance, 0))}</span>
                <span className="budget-summary-label">Remaining Balance</span>
              </div>
              <div className="budget-summary-card">
                <span className="budget-summary-number">{payments.filter(p => p.status === 'PAID').length} / {payments.length}</span>
                <span className="budget-summary-label">Families Paid</span>
              </div>
            </div>
          </div>
          <div className="budget-pay-link-section">
            <Link to="/pay" className="budget-pay-link-btn">
              Go to Pay &amp; RSVP Page &rarr;
            </Link>
            <p className="budget-pay-link-note">Select your family branch and pay for attending members</p>
          </div>
        </>
      )}
    </div>
  );
}
