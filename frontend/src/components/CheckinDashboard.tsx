import { useEffect, useState, useMemo } from 'react';
import { Link } from 'react-router-dom';
import { fetchCheckinStatus, performCheckin } from '../api';
import { dollars } from '../utils/formatting';
import type { TicketResponse } from '../types';
import './CheckinDashboard.css';

type Filter = 'all' | 'checked-in' | 'not-checked-in';

export default function CheckinDashboard() {
  const [tickets, setTickets] = useState<TicketResponse[]>([]);
  const [total, setTotal] = useState(0);
  const [checkedInCount, setCheckedInCount] = useState(0);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [filter, setFilter] = useState<Filter>('all');
  const [checkingIn, setCheckingIn] = useState<string | null>(null);

  const load = () => {
    fetchCheckinStatus()
      .then((data) => {
        setTotal(data.total);
        setCheckedInCount(data.checkedIn);
        setTickets(data.tickets);
      })
      .catch(() => {})
      .finally(() => setLoading(false));
  };

  useEffect(() => { load(); }, []);

  const handleCheckin = async (token: string) => {
    setCheckingIn(token);
    try {
      const res = await performCheckin(token);
      if (res.ticket) {
        setTickets((prev) =>
          prev.map((t) => (t.checkinToken === token ? res.ticket! : t))
        );
        setCheckedInCount((c) => c + 1);
      }
    } catch {
      // silently fail — ticket page handles errors better
    } finally {
      setCheckingIn(null);
    }
  };

  const filtered = useMemo(() => {
    let list = tickets;
    if (filter === 'checked-in') list = list.filter((t) => t.checkedIn);
    if (filter === 'not-checked-in') list = list.filter((t) => !t.checkedIn);
    if (search.trim()) {
      const q = search.toLowerCase();
      list = list.filter(
        (t) =>
          t.familyName.toLowerCase().includes(q) ||
          t.payerName.toLowerCase().includes(q) ||
          t.attendees.some((a) => a.name.toLowerCase().includes(q))
      );
    }
    return list.sort((a, b) => {
      if (a.checkedIn !== b.checkedIn) return a.checkedIn ? 1 : -1;
      return a.familyName.localeCompare(b.familyName);
    });
  }, [tickets, filter, search]);

  const pct = total > 0 ? Math.round((checkedInCount / total) * 100) : 0;

  if (loading) {
    return <div className="checkin-dashboard"><div className="checkin-loading">Loading check-in data...</div></div>;
  }

  return (
    <div className="checkin-dashboard">
      <h2>Check-In</h2>
      <p className="checkin-subtitle">Manage day-of attendance for paid families</p>

      <div className="checkin-summary">
        <div className="checkin-stat">
          <div className="checkin-stat-number">{total}</div>
          <div className="checkin-stat-label">Families Paid</div>
        </div>
        <div className="checkin-stat">
          <div className="checkin-stat-number">{checkedInCount}</div>
          <div className="checkin-stat-label">Checked In</div>
        </div>
        <div className="checkin-stat">
          <div className="checkin-stat-number">{total - checkedInCount}</div>
          <div className="checkin-stat-label">Remaining</div>
        </div>
      </div>

      <div className="checkin-progress-wrap">
        <div className="checkin-progress-bar">
          <div className="checkin-progress-fill" style={{ width: `${pct}%` }} />
        </div>
        <div className="checkin-progress-text">{pct}% checked in</div>
      </div>

      <div className="checkin-search-wrap">
        <input
          type="text"
          className="checkin-search"
          placeholder="Search by family or attendee name..."
          value={search}
          onChange={(e) => setSearch(e.target.value)}
        />
        {search && (
          <button className="checkin-search-clear" onClick={() => setSearch('')}>
            &times;
          </button>
        )}
      </div>

      <div className="checkin-filters">
        <button
          className={`checkin-filter-btn ${filter === 'all' ? 'active' : ''}`}
          onClick={() => setFilter('all')}
        >
          All ({total})
        </button>
        <button
          className={`checkin-filter-btn ${filter === 'checked-in' ? 'active' : ''}`}
          onClick={() => setFilter('checked-in')}
        >
          Checked In ({checkedInCount})
        </button>
        <button
          className={`checkin-filter-btn ${filter === 'not-checked-in' ? 'active' : ''}`}
          onClick={() => setFilter('not-checked-in')}
        >
          Remaining ({total - checkedInCount})
        </button>
      </div>

      {filtered.length === 0 ? (
        <div className="checkin-empty">
          {search ? 'No families match your search' : 'No paid families yet'}
        </div>
      ) : (
        <div className="checkin-list">
          {filtered.map((t) => (
            <div
              key={t.checkinToken}
              className={`checkin-card ${t.checkedIn ? 'is-checked-in' : ''}`}
            >
              <div className="checkin-card-info">
                <div className="checkin-card-family">{t.familyName} Family</div>
                <div className="checkin-card-details">
                  <span>{t.attendees.length} attendee{t.attendees.length !== 1 ? 's' : ''}</span>
                  <span>{dollars(t.amount)}</span>
                  <span>{t.payerName}</span>
                  {t.payerEmail && <span className="checkin-card-email">{t.payerEmail}</span>}
                </div>
              </div>

              <span className={`checkin-badge ${t.checkedIn ? 'checked-in' : 'not-checked-in'}`}>
                {t.checkedIn ? 'Checked In' : 'Not Yet'}
              </span>
              {t.checkedIn && t.checkedInAt && (
                <span className="checkin-time">
                  {new Date(t.checkedInAt).toLocaleTimeString([], { hour: 'numeric', minute: '2-digit' })}
                </span>
              )}

              <div className="checkin-card-actions">
                {!t.checkedIn && (
                  <button
                    className="btn-checkin"
                    onClick={() => handleCheckin(t.checkinToken)}
                    disabled={checkingIn === t.checkinToken}
                  >
                    {checkingIn === t.checkinToken ? 'Checking in...' : 'Check In'}
                  </button>
                )}
                <Link to={`/ticket/${t.checkinToken}`} className="btn-view-ticket">
                  View Ticket
                </Link>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
