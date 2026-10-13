import { useEffect, useState } from 'react';
import { fetchEvents, createEvent, updateEvent, deleteEvent } from '../api';
import { useAuth } from '../AuthContext';
import type { EventRequest, EventResponse } from '../types';
import { useToast } from './Toast';
import { SkeletonCard } from './Skeleton';
import './Events.css';

const emptyForm: EventRequest = {
  title: '',
  description: '',
  eventDateTime: '',
  address: '',
  hostName: '',
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

export default function Events() {
  const [events, setEvents] = useState<EventResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [form, setForm] = useState<EventRequest>(emptyForm);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [error, setError] = useState('');
  const [showFormModal, setShowFormModal] = useState(false);
  const { showToast } = useToast();
  const { isAdmin } = useAuth();

  const load = () => {
    setLoading(true);
    fetchEvents()
      .then(setEvents)
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
    // Ensure seconds are included for backend LocalDateTime parsing
    const payload = {
      ...form,
      eventDateTime: form.eventDateTime.length === 16 ? form.eventDateTime + ':00' : form.eventDateTime,
    };
    try {
      if (editingId !== null) {
        await updateEvent(editingId, payload);
        showToast('Event updated');
      } else {
        await createEvent(payload);
        showToast('Event created');
      }
      setForm(emptyForm);
      setEditingId(null);
      setShowFormModal(false);
      load();
    } catch (err: any) {
      setError(err.message || 'Failed to save event');
      showToast('Failed to save event', 'error');
    }
  };

  const handleEdit = (ev: EventResponse) => {
    setEditingId(ev.id);
    const dt = ev.eventDateTime.length > 16 ? ev.eventDateTime.slice(0, 16) : ev.eventDateTime;
    setForm({
      title: ev.title,
      description: ev.description || '',
      eventDateTime: dt,
      address: ev.address,
      hostName: ev.hostName || '',
      notes: ev.notes || '',
    });
    setError('');
    setShowFormModal(true);
  };

  const handleAddNew = () => {
    setEditingId(null);
    setForm(emptyForm);
    setError('');
    setShowFormModal(true);
  };

  const closeFormModal = () => {
    setShowFormModal(false);
    setEditingId(null);
    setForm(emptyForm);
    setError('');
  };

  const handleDelete = async (id: number, title: string) => {
    if (!window.confirm(`Delete the event "${title}"?`)) return;
    try {
      await deleteEvent(id);
      showToast('Event deleted');
      load();
    } catch (err) {
      showToast('Failed to delete event', 'error');
    }
  };

  return (
    <div className="events-page">
      <div className="page-header">
        <h2>Reunion Events</h2>
        <p>Events happening during the reunion weekend</p>
        {isAdmin && (
          <button className="btn-add-event" onClick={handleAddNew}>+ Add Event</button>
        )}
      </div>

      {loading ? (
        <div className="event-cards">
          <SkeletonCard lines={4} />
          <SkeletonCard lines={4} />
        </div>
      ) : events.length === 0 ? (
        <p className="events-empty">No events scheduled yet.</p>
      ) : (
        <div className="event-cards">
          {events.map((ev) => {
            return (
              <div key={ev.id} className="event-card">
                <div className="event-card-header">
                  <div>
                    <h3>{ev.title}</h3>
                    <p className="event-date">{formatDateTime(ev.eventDateTime)}</p>
                  </div>
                  {isAdmin && (
                    <div className="event-card-actions">
                      <button className="btn-edit" onClick={() => handleEdit(ev)}>Edit</button>
                      <button className="btn-delete" onClick={() => handleDelete(ev.id, ev.title)}>Delete</button>
                    </div>
                  )}
                </div>

                <div className="event-details">
                  <div className="event-detail-row">
                    <span className="event-detail-icon">&#128205;</span>
                    <span>{ev.address}</span>
                  </div>
                  {ev.hostName && (
                    <div className="event-detail-row">
                      <span className="event-detail-icon">&#128100;</span>
                      <span>Hosted by {ev.hostName}</span>
                    </div>
                  )}
                  {ev.description && <p className="event-description">{ev.description}</p>}
                  {ev.notes && <p className="event-notes">{ev.notes}</p>}
                </div>

              </div>
            );
          })}
        </div>
      )}

      {isAdmin && showFormModal && (
        <div className="modal-backdrop" onClick={closeFormModal}>
          <div className="modal-content event-modal" onClick={e => e.stopPropagation()}>
            <div className="modal-header">
              <h3>{editingId !== null ? 'Edit Event' : 'Add Event'}</h3>
              <button className="modal-close" onClick={closeFormModal}>&times;</button>
            </div>
            <form onSubmit={handleSubmit}>
              <div className="modal-body">
                <div className="event-form-grid-modal">
                  <label>
                    Title
                    <input name="title" value={form.title} onChange={handleChange} required autoFocus />
                  </label>
                  <label>
                    Date & Time
                    <input
                      type="datetime-local"
                      name="eventDateTime"
                      value={form.eventDateTime}
                      onChange={handleChange}
                      required
                    />
                  </label>
                  <label>
                    Address
                    <input name="address" value={form.address} onChange={handleChange} required />
                  </label>
                  <label>
                    Host Name
                    <input name="hostName" value={form.hostName} onChange={handleChange} placeholder="Optional" />
                  </label>
                  <label>
                    Description
                    <textarea name="description" value={form.description} onChange={handleChange} placeholder="Optional" />
                  </label>
                  <label>
                    Notes
                    <textarea name="notes" value={form.notes} onChange={handleChange} placeholder="Optional" />
                  </label>
                </div>
                {error && <p className="event-form-error">{error}</p>}
              </div>
              <div className="modal-footer">
                <button type="button" className="btn-modal-cancel" onClick={closeFormModal}>Cancel</button>
                <button type="submit" className="btn-modal-register">
                  {editingId !== null ? 'Update Event' : 'Add Event'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
