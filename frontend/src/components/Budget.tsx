import { useEffect, useState } from 'react';
import { fetchFamilyTree } from '../api';
import type { FamilyTreeNode } from '../types';
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

interface BranchSummary {
  name: string;
  adults: number;
  children: number;
  infants: number;
  total: number;
  owed: number;
  paid: number;
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

function buildBranches(roots: FamilyTreeNode[]): { totals: MemberCounts; branches: BranchSummary[] } {
  const totals: MemberCounts = { totalMembers: 0, adultCount: 0, childCount: 0, infantCount: 0 };
  const branches: BranchSummary[] = [];

  for (const root of roots) {
    for (const branch of root.children) {
      const c = countSubtree(branch);
      const name = branch.name.replace(/ - Done$/, '');
      const owed = c.adultCount * ADULT_FEE + c.childCount * CHILD_FEE;
      branches.push({
        name,
        adults: c.adultCount,
        children: c.childCount,
        infants: c.infantCount,
        total: c.totalMembers,
        owed,
        paid: 0,
      });
      totals.totalMembers += c.totalMembers;
      totals.adultCount += c.adultCount;
      totals.childCount += c.childCount;
      totals.infantCount += c.infantCount;
    }
  }

  branches.sort((a, b) => a.name.localeCompare(b.name));
  return { totals, branches };
}

export default function Budget() {
  const [memberCounts, setMemberCounts] = useState<MemberCounts | null>(null);
  const [branches, setBranches] = useState<BranchSummary[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    setLoading(true);
    fetchFamilyTree()
      .then((tree) => {
        const { totals, branches } = buildBranches(tree.roots);
        setMemberCounts(totals);
        setBranches(branches);
      })
      .catch(console.error)
      .finally(() => setLoading(false));
  }, []);

  if (loading) return (
    <div className="budget-page">
      <div className="page-header"><h2>Reunion Budget</h2><p>Cost estimates based on family members</p></div>
      <div className="budget-summary-grid">
        {Array.from({ length: 4 }).map((_, i) => <SkeletonCard key={i} lines={1} />)}
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
      <div className="payment-table-wrapper">
        <table className="payment-table">
          <thead>
            <tr>
              <th>Family Branch</th>
              <th>Members</th>
              <th>Owed</th>
              <th>Paid</th>
              <th>Balance</th>
              <th>Status</th>
            </tr>
          </thead>
          <tbody>
            {branches.map((b) => {
              const balance = b.owed - b.paid;
              const status = b.paid === 0 ? 'Unpaid' : b.paid >= b.owed ? 'Paid' : 'Partial';
              return (
                <tr key={b.name}>
                  <td className="payment-family">{b.name}</td>
                  <td>{b.total}</td>
                  <td>{dollars(b.owed)}</td>
                  <td>{dollars(b.paid)}</td>
                  <td className={balance > 0 ? 'payment-balance-due' : ''}>
                    {dollars(balance)}
                  </td>
                  <td>
                    <span className={`payment-status-badge status-${status.toLowerCase()}`}>
                      {status}
                    </span>
                  </td>
                </tr>
              );
            })}
            <tr className="payment-total-row">
              <td className="payment-family"><strong>Total</strong></td>
              <td><strong>{branches.reduce((s, b) => s + b.total, 0)}</strong></td>
              <td><strong>{dollars(branches.reduce((s, b) => s + b.owed, 0))}</strong></td>
              <td><strong>{dollars(branches.reduce((s, b) => s + b.paid, 0))}</strong></td>
              <td className="payment-balance-due">
                <strong>{dollars(branches.reduce((s, b) => s + b.owed - b.paid, 0))}</strong>
              </td>
              <td></td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  );
}
