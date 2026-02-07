import { useEffect, useState } from 'react';
import { fetchSummary } from '../api';
import type { RsvpSummaryResponse } from '../types';
import './Budget.css';

const ADULT_MIN = 75;
const ADULT_MAX = 125;
const CHILD_MIN = 50;
const CHILD_MAX = 75;

function dollars(n: number): string {
  return '$' + n.toLocaleString();
}

export default function Budget() {
  const [summary, setSummary] = useState<RsvpSummaryResponse | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    setLoading(true);
    fetchSummary()
      .then(setSummary)
      .catch(console.error)
      .finally(() => setLoading(false));
  }, []);

  if (loading) return <p>Loading budget...</p>;
  if (!summary) return <p>Unable to load summary data.</p>;

  const adultLow = summary.adultCount * ADULT_MIN;
  const adultHigh = summary.adultCount * ADULT_MAX;
  const childLow = summary.childCount * CHILD_MIN;
  const childHigh = summary.childCount * CHILD_MAX;
  const totalLow = adultLow + childLow;
  const totalHigh = adultHigh + childHigh;

  return (
    <div className="budget-page">
      <div className="page-header">
        <h2>Reunion Budget</h2>
        <p>Cost estimates based on current RSVPs</p>
      </div>

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
            <span className="budget-row-title">Adults</span>
            <span className="budget-row-detail">
              {summary.adultCount} x {dollars(ADULT_MIN)}–{dollars(ADULT_MAX)} per person
            </span>
          </div>
          <span className="budget-row-range">
            {dollars(adultLow)} – {dollars(adultHigh)}
          </span>
        </div>

        <div className="budget-row">
          <div className="budget-row-label">
            <span className="budget-row-title">Children</span>
            <span className="budget-row-detail">
              {summary.childCount} x {dollars(CHILD_MIN)}–{dollars(CHILD_MAX)} per person
            </span>
          </div>
          <span className="budget-row-range">
            {dollars(childLow)} – {dollars(childHigh)}
          </span>
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
            <span className="budget-row-title">Estimated Total</span>
            <span className="budget-row-detail">
              {summary.totalHeadcount} guests across {summary.totalFamilies} families
            </span>
          </div>
          <span className="budget-row-range">
            {dollars(totalLow)} – {dollars(totalHigh)}
          </span>
        </div>
      </div>
    </div>
  );
}
