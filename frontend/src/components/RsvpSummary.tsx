import { useEffect, useState } from 'react';
import { fetchSummary } from '../api';
import type { RsvpSummaryResponse } from '../types';
import './RsvpSummary.css';

interface Props {
  refreshKey: number;
}

export default function RsvpSummary({ refreshKey }: Props) {
  const [summary, setSummary] = useState<RsvpSummaryResponse | null>(null);

  useEffect(() => {
    fetchSummary().then(setSummary).catch(console.error);
  }, [refreshKey]);

  if (!summary) return null;

  return (
    <div className="summary">
      <h2>Reunion Summary</h2>
      <div className="summary-grid">
        <div className="summary-card">
          <span className="summary-number">{summary.totalFamilies}</span>
          <span className="summary-label">Families</span>
        </div>
        <div className="summary-card">
          <span className="summary-number">{summary.totalHeadcount}</span>
          <span className="summary-label">Total Guests</span>
        </div>
        <div className="summary-card">
          <span className="summary-number">{summary.adultCount}</span>
          <span className="summary-label">Adults</span>
        </div>
        <div className="summary-card">
          <span className="summary-number">{summary.childCount}</span>
          <span className="summary-label">Children</span>
        </div>
        <div className="summary-card">
          <span className="summary-number">{summary.infantCount}</span>
          <span className="summary-label">Infants</span>
        </div>
        <div className="summary-card">
          <span className="summary-number">{summary.lodgingCount}</span>
          <span className="summary-label">Need Lodging</span>
        </div>
      </div>
    </div>
  );
}
