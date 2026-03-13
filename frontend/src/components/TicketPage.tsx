import { useEffect, useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import { QRCodeSVG } from 'qrcode.react';
import { fetchTicket, sendTicket, performCheckin, fetchCheckinCapabilities } from '../api';
import { useAuth } from '../AuthContext';
import { ageLabel } from '../constants/ageGroups';
import { dollars } from '../utils/formatting';
import type { TicketResponse } from '../types';
import './TicketPage.css';

export default function TicketPage() {
  const { token } = useParams<{ token: string }>();
  const { isAdmin } = useAuth();
  const [ticket, setTicket] = useState<TicketResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const [sendMode, setSendMode] = useState<'email' | 'sms' | null>(null);
  const [sendTo, setSendTo] = useState('');
  const [sending, setSending] = useState(false);
  const [sendResult, setSendResult] = useState('');

  const [capabilities, setCapabilities] = useState({ email: false, sms: false });
  const [checkingIn, setCheckingIn] = useState(false);

  useEffect(() => {
    if (!token) return;
    Promise.all([
      fetchTicket(token),
      fetchCheckinCapabilities(),
    ])
      .then(([t, caps]) => {
        setTicket(t);
        setCapabilities(caps);
      })
      .catch(() => setError('Ticket not found or payment not completed.'))
      .finally(() => setLoading(false));
  }, [token]);

  const handleSend = async () => {
    if (!token || !sendTo.trim()) return;
    setSending(true);
    setSendResult('');
    try {
      const data = sendMode === 'email'
        ? { checkinToken: token, email: sendTo.trim() }
        : { checkinToken: token, phone: sendTo.trim() };
      const res = await sendTicket(data);
      setSendResult(res.message);
      setSendTo('');
      setSendMode(null);
    } catch (err: any) {
      setSendResult(err.message || 'Failed to send');
    } finally {
      setSending(false);
    }
  };

  const handleCheckin = async () => {
    if (!token) return;
    setCheckingIn(true);
    try {
      const res = await performCheckin(token);
      if (res.ticket) setTicket(res.ticket);
    } catch (err: any) {
      setError(err.message || 'Check-in failed');
    } finally {
      setCheckingIn(false);
    }
  };

  if (loading) {
    return (
      <div className="ticket-page">
        <div className="ticket-loading">Loading ticket...</div>
      </div>
    );
  }

  if (error || !ticket) {
    return (
      <div className="ticket-page">
        <div className="ticket-error">{error || 'Ticket not found'}</div>
      </div>
    );
  }

  const ticketUrl = `${window.location.origin}/ticket/${token}`;

  return (
    <div className="ticket-page">
      <div className="ticket-card">
        <div className="ticket-header">
          <h2>Tumblin Family Reunion</h2>
          <p className="ticket-family">{ticket.familyName} Family</p>
          <p className="ticket-payer">Paid by {ticket.payerName}</p>
        </div>

        <div className="ticket-qr">
          <QRCodeSVG value={ticketUrl} size={200} level="M" />
        </div>

        {ticket.checkedIn && (
          <div className="ticket-checked-in-badge">
            Checked In
            {ticket.checkedInAt && (
              <span className="ticket-checked-in-time">
                {new Date(ticket.checkedInAt).toLocaleString()}
              </span>
            )}
          </div>
        )}

        <div className="ticket-attendees">
          <h3>Party ({ticket.attendees.length})</h3>
          <ul>
            {ticket.attendees.map((a, i) => (
              <li key={i} className="ticket-attendee">
                <span className="ticket-attendee-name">{a.name}</span>
                <span className={`ticket-attendee-age age-${a.ageGroup.toLowerCase()}`}>
                  {ageLabel(a.ageGroup)}
                </span>
                {a.isGuest && <span className="ticket-guest-tag">Guest</span>}
              </li>
            ))}
          </ul>
        </div>

        <div className="ticket-amount">
          <span>Amount Paid</span>
          <span>{dollars(ticket.amount)}</span>
        </div>

        {/* Admin check-in button */}
        {isAdmin && !ticket.checkedIn && (
          <button
            className="ticket-checkin-btn"
            onClick={handleCheckin}
            disabled={checkingIn}
          >
            {checkingIn ? 'Checking in...' : 'Check In'}
          </button>
        )}

        {/* Send ticket options */}
        <div className="ticket-send-section">
          <p className="ticket-send-label">Send this ticket</p>
          <p className="ticket-send-hint">Send a link to this ticket and QR code via email or text</p>
          <div className="ticket-send-buttons">
            {capabilities.email && (
              <button
                className={`ticket-send-btn ${sendMode === 'email' ? 'active' : ''}`}
                onClick={() => { setSendMode(sendMode === 'email' ? null : 'email'); setSendTo(''); setSendResult(''); }}
              >
                Email
              </button>
            )}
            {capabilities.sms && (
              <button
                className={`ticket-send-btn ${sendMode === 'sms' ? 'active' : ''}`}
                onClick={() => { setSendMode(sendMode === 'sms' ? null : 'sms'); setSendTo(''); setSendResult(''); }}
              >
                Text
              </button>
            )}
            {!capabilities.email && !capabilities.sms && (
              <p className="ticket-send-disabled">Email and SMS not configured</p>
            )}
          </div>

          {sendMode && (
            <div className="ticket-send-form">
              <input
                type={sendMode === 'email' ? 'email' : 'tel'}
                placeholder={sendMode === 'email' ? 'Email address' : 'Phone number (e.g. +15551234567)'}
                value={sendTo}
                onChange={e => setSendTo(e.target.value)}
                className="ticket-send-input"
                autoFocus
                onKeyDown={e => { if (e.key === 'Enter') handleSend(); }}
              />
              <button
                className="ticket-send-submit"
                onClick={handleSend}
                disabled={sending || !sendTo.trim()}
              >
                {sending ? 'Sending...' : 'Send'}
              </button>
            </div>
          )}

          {sendResult && (
            <p className="ticket-send-result">{sendResult}</p>
          )}
        </div>

        <div className="ticket-back-link">
          <Link to="/pay">&larr; Back to Pay &amp; RSVP</Link>
        </div>
      </div>
    </div>
  );
}
