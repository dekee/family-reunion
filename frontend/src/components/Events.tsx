import { useEffect, useState } from 'react';
import {
  fetchEvents, createEvent, updateEvent, deleteEvent,
  registerForEvent, unregisterFromEvent, fetchFamilyTree,
} from '../api';
import { useAuth } from '../AuthContext';
import { getBranchColor } from '../branchColors';
import type { EventRequest, EventResponse, FamilyTreeNode } from '../types';
import { useToast } from './Toast';
import { SkeletonCard } from './Skeleton';
import RegistrationModal from './RegistrationModal';
import './Events.css';
import './RegistrationModal.css';

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

interface FlatMember {
  id: number;
  name: string;
  branchName: string;
}

function flattenTree(nodes: FamilyTreeNode[]): FlatMember[] {
  const result: FlatMember[] = [];
  function walk(n: FamilyTreeNode, branch: string) {
    result.push({ id: n.id, name: n.name, branchName: branch });
    n.children.forEach(c => walk(c, branch));
  }
  for (const root of nodes) {
    if (root.children.length > 0) {
      for (const child of root.children) {
        walk(child, child.name);
      }
      result.push({ id: root.id, name: root.name, branchName: root.name });
    } else {
      result.push({ id: root.id, name: root.name, branchName: root.name });
    }
  }
  return result.sort((a, b) => a.name.localeCompare(b.name));
}

export default function Events() {
  const [events, setEvents] = useState<EventResponse[]>([]);
  const [members, setMembers] = useState<FlatMember[]>([]);
  const [loading, setLoading] = useState(true);
  const [form, setForm] = useState<EventRequest>(emptyForm);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [error, setError] = useState('');
  const [registeringEventId, setRegisteringEventId] = useState<number | null>(null);
  const [showFormModal, setShowFormModal] = useState(false);
  const { showToast } = useToast();
  const { isAdmin } = useAuth();

  const load = () => {
    setLoading(true);
    Promise.all([fetchEvents(), fetchFamilyTree()])
      .then(([evts, tree]) => {
        setEvents(evts);
        setMembers(flattenTree(tree.roots));
      })
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

  const handleModalRegister = async (eventId: number, memberIds: number[]) => {
    try {
      await registerForEvent(eventId, { familyMemberIds: memberIds });
      showToast(`${memberIds.length} member${memberIds.length > 1 ? 's' : ''} registered`);
      setRegisteringEventId(null);
      load();
    } catch (err) {
      showToast('Failed to register', 'error');
    }
  };

  const handleUnregister = async (eventId: number, memberId: number) => {
    try {
      await unregisterFromEvent(eventId, memberId);
      showToast('Member unregistered');
      load();
    } catch (err) {
      showToast('Failed to unregister', 'error');
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
            const registeredIds = new Set(ev.registrations.map((r) => r.familyMemberId));
            const available = members.filter((m) => !registeredIds.has(m.id));

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

                <div className="event-registration">
                  <div className="event-reg-header">
                    <strong>Registered ({ev.registrationCount})</strong>
                    {available.length > 0 && (
                      <button
                        className="btn-register-open"
                        onClick={() => setRegisteringEventId(ev.id)}
                      >
                        Register Members
                      </button>
                    )}
                  </div>
                  {ev.registrations.length > 0 && (() => {
                    const memberMap = new Map(members.map(m => [m.id, m]));
                    const grouped = new Map<string, typeof ev.registrations>();
                    for (const r of ev.registrations) {
                      const branch = memberMap.get(r.familyMemberId)?.branchName || 'Other';
                      const list = grouped.get(branch) || [];
                      list.push(r);
                      grouped.set(branch, list);
                    }
                    const sortedBranches = [...grouped.entries()].sort((a, b) => a[0].localeCompare(b[0]));

                    return (
                      <div className="event-reg-grouped">
                        {sortedBranches.map(([branch, regs]) => (
                          <div
                            key={branch}
                            className="reg-branch-group"
                            style={{ borderLeft: `3px solid ${getBranchColor(branch)}`, paddingLeft: '0.75rem' }}
                          >
                            <span className="reg-branch-label">{branch}</span>
                            <div className="event-reg-badges">
                              {regs.map((r) => (
                                <span key={r.id} className="reg-badge">
                                  {r.familyMemberName}
                                  <button
                                    className="reg-badge-remove"
                                    onClick={() => handleUnregister(ev.id, r.familyMemberId)}
                                    title={`Remove ${r.familyMemberName}`}
                                  >
                                    &times;
                                  </button>
                                </span>
                              ))}
                            </div>
                          </div>
                        ))}
                      </div>
                    );
                  })()}

                </div>
              </div>
            );
          })}
        </div>
      )}

      {registeringEventId !== null && (() => {
        const ev = events.find(e => e.id === registeringEventId);
        if (!ev) return null;
        const registeredIds = new Set(ev.registrations.map(r => r.familyMemberId));
        const available = members.filter(m => !registeredIds.has(m.id));
        return (
          <RegistrationModal
            eventTitle={ev.title}
            available={available}
            onRegister={(ids) => handleModalRegister(ev.id, ids)}
            onClose={() => setRegisteringEventId(null)}
          />
        );
      })()}

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
