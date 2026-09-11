import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { fetchPaymentHistory } from '../api';
import { ageLabel } from '../constants/ageGroups';
import { ANGEL_LINE_ITEM_NAME, SIZES_BY_CATEGORY, SIZE_CATEGORY_LABELS, SIZE_LABELS, sizeLabel } from '../constants/tshirtSizes';
import type { SizeCategory } from '../constants/tshirtSizes';
import { dollars } from '../utils/formatting';
import type { PaymentDetailResponse, PaymentLineItemResponse } from '../types';
import { SkeletonCard } from './Skeleton';
import './PaymentHistory.css';

interface SizeRow extends PaymentLineItemResponse {
  familyName: string;
}

const SIZE_CATEGORIES: SizeCategory[] = ['UNISEX', 'YOUTH', 'ONESIE'];

function formatDate(iso: string): string {
  const d = new Date(iso);
  return d.toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric', hour: 'numeric', minute: '2-digit' });
}

type StatusFilter = 'ALL' | 'COMPLETED' | 'PENDING';

export default function PaymentHistory() {
  const [payments, setPayments] = useState<PaymentDetailResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [filter, setFilter] = useState<StatusFilter>('ALL');
  const [search, setSearch] = useState('');
  const [showSizes, setShowSizes] = useState(true);
  const [showSizeList, setShowSizeList] = useState(false);

  useEffect(() => {
    fetchPaymentHistory()
      .then(setPayments)
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false));
  }, []);

  const filtered = payments.filter(p => {
    if (filter !== 'ALL' && p.status !== filter) return false;
    if (search) {
      const q = search.toLowerCase();
      return (
        p.familyName.toLowerCase().includes(q) ||
        (p.payerName?.toLowerCase().includes(q) ?? false) ||
        (p.payerEmail?.toLowerCase().includes(q) ?? false) ||
        p.lineItems.some(li => li.name.toLowerCase().includes(q))
      );
    }
    return true;
  });

  const completedPayments = payments.filter(p => p.status === 'COMPLETED');
  const totalCollected = completedPayments.reduce((s, p) => s + p.amount, 0);
  const totalPending = payments.filter(p => p.status === 'PENDING').reduce((s, p) => s + p.amount, 0);

  // T-shirt tally: one shirt per completed, non-angel line item
  const sizeRows: SizeRow[] = completedPayments.flatMap(p =>
    p.lineItems
      .filter(li => !(li.isGuest && li.name === ANGEL_LINE_ITEM_NAME))
      .map(li => ({ ...li, familyName: p.familyName }))
  );
  const sizeCounts = new Map<string, number>();
  for (const row of sizeRows) {
    if (row.tshirtSize) sizeCounts.set(row.tshirtSize, (sizeCounts.get(row.tshirtSize) ?? 0) + 1);
  }
  const missingSizeRows = sizeRows.filter(r => !r.tshirtSize);
  const sortedSizeRows = [...sizeRows].sort((a, b) => {
    // Missing sizes first so they're easy to chase, then by family, then name
    if (!a.tshirtSize !== !b.tshirtSize) return a.tshirtSize ? 1 : -1;
    return a.familyName.localeCompare(b.familyName) || a.name.localeCompare(b.name);
  });

  if (loading) return (
    <div className="ph-page">
      <div className="page-header"><h2>Payment History</h2><p>All payment transactions</p></div>
      <div className="ph-stats-grid">
        {Array.from({ length: 3 }).map((_, i) => <SkeletonCard key={i} lines={1} />)}
      </div>
    </div>
  );

  if (error) return (
    <div className="ph-page">
      <div className="page-header"><h2>Payment History</h2></div>
      <p className="ph-error">{error}</p>
    </div>
  );

  return (
    <div className="ph-page">
      <div className="page-header">
        <h2>Payment History</h2>
        <p>All payment transactions with payer details</p>
      </div>

      <div className="ph-stats-grid">
        <div className="ph-stat-card">
          <span className="ph-stat-number">{payments.length}</span>
          <span className="ph-stat-label">Total Transactions</span>
        </div>
        <div className="ph-stat-card">
          <span className="ph-stat-number ph-stat-green">{dollars(totalCollected)}</span>
          <span className="ph-stat-label">Collected</span>
        </div>
        <div className="ph-stat-card">
          <span className="ph-stat-number ph-stat-amber">{dollars(totalPending)}</span>
          <span className="ph-stat-label">Pending</span>
        </div>
      </div>

      <section className="ph-sizes-section">
        <button
          type="button"
          className="ph-sizes-toggle"
          onClick={() => setShowSizes(s => !s)}
          aria-expanded={showSizes}
        >
          <span className="ph-sizes-title">T-Shirt Sizes</span>
          <span className="ph-sizes-summary">
            {sizeRows.length} shirt{sizeRows.length === 1 ? '' : 's'}
            {missingSizeRows.length > 0 && (
              <span className="ph-size-missing-badge">{missingSizeRows.length} missing</span>
            )}
          </span>
          <span className="ph-sizes-chevron" aria-hidden="true">{showSizes ? '▾' : '▸'}</span>
        </button>

        {showSizes && (
          <div className="ph-sizes-body">
            {sizeRows.length === 0 ? (
              <p className="ph-sizes-empty">No completed payments yet.</p>
            ) : (
              <>
                <div className="ph-sizes-groups">
                  {SIZE_CATEGORIES.map(cat => (
                    <div key={cat} className="ph-sizes-group">
                      <span className="ph-sizes-group-label">{SIZE_CATEGORY_LABELS[cat]}</span>
                      <div className="ph-size-chips">
                        {SIZES_BY_CATEGORY[cat].map(size => {
                          const count = sizeCounts.get(size) ?? 0;
                          return (
                            <span key={size} className={`ph-size-chip ${count === 0 ? 'ph-size-chip-zero' : ''}`}>
                              <span className="ph-size-chip-label">{SIZE_LABELS[size]}</span>
                              <span className="ph-size-chip-count">{count}</span>
                            </span>
                          );
                        })}
                      </div>
                    </div>
                  ))}
                </div>

                <button
                  type="button"
                  className="ph-sizes-list-toggle"
                  onClick={() => setShowSizeList(s => !s)}
                  aria-expanded={showSizeList}
                >
                  {showSizeList ? 'Hide' : 'Show'} per-person list ({sizeRows.length})
                </button>

                {showSizeList && (
                  <div className="ph-sizes-table-wrap">
                    <table className="ph-sizes-table">
                      <thead>
                        <tr>
                          <th>Name</th>
                          <th>Family</th>
                          <th>Age</th>
                          <th>Size</th>
                        </tr>
                      </thead>
                      <tbody>
                        {sortedSizeRows.map(row => (
                          <tr key={row.lineItemId} className={row.tshirtSize ? '' : 'ph-sizes-row-missing'}>
                            <td>
                              {row.name}
                              {row.isGuest && <span className="ph-guest-tag">Guest</span>}
                            </td>
                            <td>{row.familyName}</td>
                            <td><span className={`ph-li-age age-${row.ageGroup.toLowerCase()}`}>{ageLabel(row.ageGroup)}</span></td>
                            <td>
                              {row.tshirtSize
                                ? sizeLabel(row.tshirtSize)
                                : <span className="ph-size-missing-badge">Missing</span>}
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                )}
              </>
            )}
          </div>
        )}
      </section>

      <div className="ph-controls">
        <input
          type="text"
          placeholder="Search by name, email, or family..."
          value={search}
          onChange={e => setSearch(e.target.value)}
          className="ph-search"
        />
        <div className="ph-filters">
          {(['ALL', 'COMPLETED', 'PENDING'] as StatusFilter[]).map(f => (
            <button
              key={f}
              className={`ph-filter-btn ${filter === f ? 'active' : ''}`}
              onClick={() => setFilter(f)}
            >
              {f === 'ALL' ? 'All' : f === 'COMPLETED' ? 'Completed' : 'Pending'}
              {f === 'ALL' && ` (${payments.length})`}
              {f === 'COMPLETED' && ` (${completedPayments.length})`}
              {f === 'PENDING' && ` (${payments.filter(p => p.status === 'PENDING').length})`}
            </button>
          ))}
        </div>
      </div>

      {filtered.length === 0 ? (
        <p className="ph-empty">No payments match your filters.</p>
      ) : (
        <div className="ph-list">
          {filtered.map(p => (
            <div key={p.id} className={`ph-card ph-card-${p.status.toLowerCase()}`}>
              <div className="ph-card-header">
                <div className="ph-card-title">
                  <span className="ph-family-name">{p.familyName}</span>
                  <span className={`ph-status-badge ph-status-${p.status.toLowerCase()}`}>{p.status}</span>
                </div>
                <span className="ph-amount">{dollars(p.amount)}</span>
              </div>

              <div className="ph-card-details">
                <div className="ph-detail-row">
                  <span className="ph-detail-label">Payer</span>
                  <span className="ph-detail-value">{p.payerName && p.payerName !== 'null' ? p.payerName : '—'}</span>
                </div>
                {p.payerEmail && (
                  <div className="ph-detail-row">
                    <span className="ph-detail-label">Email</span>
                    <span className="ph-detail-value">{p.payerEmail}</span>
                  </div>
                )}
                <div className="ph-detail-row">
                  <span className="ph-detail-label">Date</span>
                  <span className="ph-detail-value">{formatDate(p.createdAt)}</span>
                </div>
                {p.checkedIn && (
                  <div className="ph-detail-row">
                    <span className="ph-detail-label">Checked In</span>
                    <span className="ph-detail-value ph-checked-in">{p.checkedInAt ? formatDate(p.checkedInAt) : 'Yes'}</span>
                  </div>
                )}
              </div>

              {p.lineItems.length > 0 && (
                <div className="ph-line-items">
                  <span className="ph-line-items-title">Paid for ({p.lineItems.length})</span>
                  <div className="ph-line-items-list">
                    {p.lineItems.map((li, i) => {
                      const isAngel = li.isGuest && li.name === ANGEL_LINE_ITEM_NAME;
                      return (
                        <div key={li.lineItemId ?? i} className="ph-line-item">
                          <span className="ph-li-name">
                            {isAngel ? (
                              <span className="ph-angel-tag">Angel</span>
                            ) : li.isGuest ? (
                              <span className="ph-guest-tag">Guest</span>
                            ) : null}
                            {isAngel ? ANGEL_LINE_ITEM_NAME : li.name}
                          </span>
                          {!isAngel && <span className={`ph-li-age age-${li.ageGroup.toLowerCase()}`}>{ageLabel(li.ageGroup)}</span>}
                          {!isAngel && (
                            li.tshirtSize
                              ? <span className="ph-li-size">{sizeLabel(li.tshirtSize)}</span>
                              : <span className="ph-li-size ph-li-size-missing">No size</span>
                          )}
                          <span className="ph-li-amount">{dollars(li.amount)}</span>
                        </div>
                      );
                    })}
                  </div>
                </div>
              )}

              {p.checkinToken && (
                <div className="ph-card-footer">
                  <Link to={`/ticket/${p.checkinToken}`} className="ph-ticket-link">View Ticket</Link>
                </div>
              )}
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
