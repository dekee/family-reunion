import { useEffect, useState } from 'react';
import { fetchMeetings, createMeeting, updateMeeting, deleteMeeting } from '../api';
import { useAuth } from '../AuthContext';
import type { MeetingRequest, MeetingResponse } from '../types';
import { useToast } from './Toast';
import { SkeletonCard } from './Skeleton';
import './Meetings.css';

const emptyForm: MeetingRequest = {
  title: '',
  meetingDateTime: '',
  zoomLink: '',
  phoneNumber: '',
  meetingId: '',
  passcode: '',
  notes: '',
};

function formatDateTime(iso: string): string {
  const d = new Date(iso);
  return d.toLocaleString('en-US', {
    weekday: 'short',
    month: 'short',
    day: 'numeric',
    year: 'numeric',
    hour: 'numeric',
    minute: '2-digit',
  });
}

export default function Meetings() {
  const [meetings, setMeetings] = useState<MeetingResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [form, setForm] = useState<MeetingRequest>(emptyForm);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [error, setError] = useState('');
  const { showToast } = useToast();
  const { isAdmin } = useAuth();

  const copyToClipboard = (text: string, label: string) => {
    navigator.clipboard.writeText(text).then(() => {
      showToast(`${label} copied!`);
    }).catch(() => {
      showToast('Failed to copy', 'error');
    });
  };

  const load = () => {
    setLoading(true);
    fetchMeetings()
      .then(setMeetings)
      .catch(console.error)
      .finally(() => setLoading(false));
  };

  useEffect(() => { load(); }, []);

  const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) => {
    setForm((f) => ({ ...f, [e.target.name]: e.target.value }));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    try {
      if (editingId !== null) {
        await updateMeeting(editingId, form);
        showToast('Meeting updated');
      } else {
        await createMeeting(form);
        showToast('Meeting created');
      }
      setForm(emptyForm);
      setEditingId(null);
      load();
    } catch (err: any) {
      setError(err.message || 'Failed to save meeting');
      showToast('Failed to save meeting', 'error');
    }
  };

  const handleEdit = (m: MeetingResponse) => {
    setEditingId(m.id);
    setForm({
      title: m.title,
      meetingDateTime: m.meetingDateTime,
      zoomLink: m.zoomLink,
      phoneNumber: m.phoneNumber || '',
      meetingId: m.meetingId || '',
      passcode: m.passcode || '',
      notes: m.notes || '',
    });
  };

  const handleCancelEdit = () => {
    setEditingId(null);
    setForm(emptyForm);
    setError('');
  };

  const handleDelete = async (id: number, title: string) => {
    if (!window.confirm(`Delete the meeting "${title}"?`)) return;
    try {
      await deleteMeeting(id);
      showToast('Meeting deleted');
      load();
    } catch (err) {
      showToast('Failed to delete meeting', 'error');
    }
  };

  return (
    <div className="meetings-page">
      <div className="page-header">
        <h2>Zoom Meetings</h2>
        <p>Upcoming planning calls for the reunion</p>
      </div>

      {loading ? (
        <div className="meeting-cards">
          <SkeletonCard lines={4} />
          <SkeletonCard lines={4} />
        </div>
      ) : meetings.length === 0 ? (
        <p className="meetings-empty">No meetings scheduled yet.</p>
      ) : (
        <div className="meeting-cards">
          {meetings.map((m) => (
            <div key={m.id} className="meeting-card">
              <div className="meeting-card-header">
                <h3>{m.title}</h3>
                {isAdmin && (
                  <div className="meeting-card-actions">
                    <button className="btn-edit" onClick={() => handleEdit(m)}>Edit</button>
                    <button className="btn-delete" onClick={() => handleDelete(m.id, m.title)}>Delete</button>
                  </div>
                )}
              </div>
              <p className="meeting-date">{formatDateTime(m.meetingDateTime)}</p>
              <div className="meeting-zoom">
                <a href={m.zoomLink} target="_blank" rel="noopener noreferrer">
                  Join Zoom Meeting
                </a>
                <button className="btn-copy" onClick={() => copyToClipboard(m.zoomLink, 'Zoom link')} title="Copy Zoom link">
                  Copy
                </button>
              </div>
              {(m.phoneNumber || m.meetingId || m.passcode) && (
                <div className="meeting-phone">
                  <strong>Phone Dial-In</strong>
                  <div className="meeting-phone-detail">
                    {m.phoneNumber && (
                      <div className="meeting-phone-row">
                        Phone: {m.phoneNumber}
                        <button className="btn-copy" onClick={() => copyToClipboard(m.phoneNumber!, 'Phone number')} title="Copy phone number">Copy</button>
                      </div>
                    )}
                    {m.meetingId && (
                      <div className="meeting-phone-row">
                        Meeting ID: {m.meetingId}
                        <button className="btn-copy" onClick={() => copyToClipboard(m.meetingId!, 'Meeting ID')} title="Copy meeting ID">Copy</button>
                      </div>
                    )}
                    {m.passcode && (
                      <div className="meeting-phone-row">
                        Passcode: {m.passcode}
                        <button className="btn-copy" onClick={() => copyToClipboard(m.passcode!, 'Passcode')} title="Copy passcode">Copy</button>
                      </div>
                    )}
                  </div>
                </div>
              )}
              {m.notes && <p className="meeting-notes">{m.notes}</p>}
            </div>
          ))}
        </div>
      )}

      {isAdmin && (
        <div className="meeting-form">
          <h3>{editingId !== null ? 'Edit Meeting' : 'Add Meeting'}</h3>
          <form onSubmit={handleSubmit}>
            <div className="meeting-form-grid">
              <label className="full-width">
                Title
                <input name="title" value={form.title} onChange={handleChange} required />
              </label>
              <label>
                Date & Time
                <input
                  type="datetime-local"
                  name="meetingDateTime"
                  value={form.meetingDateTime}
                  onChange={handleChange}
                  required
                />
              </label>
              <label>
                Zoom Link
                <input name="zoomLink" value={form.zoomLink} onChange={handleChange} required />
              </label>
              <label>
                Phone Number
                <input name="phoneNumber" value={form.phoneNumber} onChange={handleChange} placeholder="Optional" />
              </label>
              <label>
                Meeting ID
                <input name="meetingId" value={form.meetingId} onChange={handleChange} placeholder="Optional" />
              </label>
              <label>
                Passcode
                <input name="passcode" value={form.passcode} onChange={handleChange} placeholder="Optional" />
              </label>
              <label className="full-width">
                Notes
                <textarea name="notes" value={form.notes} onChange={handleChange} placeholder="Optional" />
              </label>
            </div>
            {error && <p className="meeting-form-error">{error}</p>}
            <div className="meeting-form-actions">
              <button type="submit" className="btn-meeting-submit">
                {editingId !== null ? 'Update Meeting' : 'Add Meeting'}
              </button>
              {editingId !== null && (
                <button type="button" className="btn-meeting-cancel" onClick={handleCancelEdit}>
                  Cancel
                </button>
              )}
            </div>
          </form>
        </div>
      )}
    </div>
  );
}
