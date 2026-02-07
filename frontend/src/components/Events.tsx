import { useEffect, useState } from 'react';
import {
  fetchEvents, createEvent, updateEvent, deleteEvent,
  registerForEvent, unregisterFromEvent, fetchFamilyTree,
} from '../api';
import { useAuth } from '../AuthContext';
import type { EventRequest, EventResponse, FamilyTreeNode } from '../types';
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
      // Group under each child of the root (e.g. Gail, Wesley II, Norris...)
      for (const child of root.children) {
        walk(child, child.name);
      }
      // Root itself gets its own name
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
  const [selectedMembers, setSelectedMembers] = useState<Record<number, number[]>>({});
  const [regSearch, setRegSearch] = useState<Record<number, string>>({});
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
    try {
      if (editingId !== null) {
        await updateEvent(editingId, form);
        showToast('Event updated');
      } else {
        await createEvent(form);
        showToast('Event created');
      }
      setForm(emptyForm);
      setEditingId(null);
      load();
    } catch (err: any) {
      setError(err.message || 'Failed to save event');
      showToast('Failed to save event', 'error');
    }
  };

  const handleEdit = (ev: EventResponse) => {
    setEditingId(ev.id);
    setForm({
      title: ev.title,
      description: ev.description || '',
      eventDateTime: ev.eventDateTime,
      address: ev.address,
      hostName: ev.hostName || '',
      notes: ev.notes || '',
    });
  };

  const handleCancelEdit = () => {
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

  const handleRegister = async (eventId: number) => {
    const ids = selectedMembers[eventId] || [];
    if (ids.length === 0) return;
    try {
      await registerForEvent(eventId, { familyMemberIds: ids });
      showToast(`${ids.length} member${ids.length > 1 ? 's' : ''} registered`);
      setSelectedMembers((prev) => ({ ...prev, [eventId]: [] }));
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

  const toggleMember = (eventId: number, memberId: number) => {
    setSelectedMembers((prev) => {
      const current = prev[eventId] || [];
      return {
        ...prev,
        [eventId]: current.includes(memberId)
          ? current.filter((id) => id !== memberId)
          : [...current, memberId],
      };
    });
  };

  const toggleBranchMembers = (eventId: number, memberIds: number[]) => {
    setSelectedMembers((prev) => {
      const current = prev[eventId] || [];
      const allSelected = memberIds.every((id) => current.includes(id));
      if (allSelected) {
        return { ...prev, [eventId]: current.filter((id) => !memberIds.includes(id)) };
      } else {
        const merged = new Set([...current, ...memberIds]);
        return { ...prev, [eventId]: [...merged] };
      }
    });
  };

  return (
    <div className="events-page">
      <div className="page-header">
        <h2>Reunion Events</h2>
        <p>Events happening during the reunion weekend</p>
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
            const selected = selectedMembers[ev.id] || [];

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
                          <div key={branch} className="reg-branch-group">
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

                  {available.length > 0 && (() => {
                    const regQuery = (regSearch[ev.id] || '').toLowerCase().trim();
                    const filteredAvailable = regQuery
                      ? available.filter(m => m.name.toLowerCase().includes(regQuery))
                      : available;

                    return (
                      <div className="event-reg-add">
                        <div className="reg-search-and-list">
                          {available.length > 10 && (
                            <div className="reg-search-wrapper">
                              <input
                                type="text"
                                className="reg-search-input"
                                placeholder="Search members..."
                                value={regSearch[ev.id] || ''}
                                onChange={e => setRegSearch(prev => ({ ...prev, [ev.id]: e.target.value }))}
                              />
                              {regSearch[ev.id] && (
                                <button
                                  className="reg-search-clear"
                                  onClick={() => setRegSearch(prev => ({ ...prev, [ev.id]: '' }))}
                                >&times;</button>
                              )}
                            </div>
                          )}
                          <div className="member-checkbox-list">
                            {(() => {
                              const byBranch = new Map<string, FlatMember[]>();
                              for (const m of filteredAvailable) {
                                const list = byBranch.get(m.branchName) || [];
                                list.push(m);
                                byBranch.set(m.branchName, list);
                              }
                              const sorted = [...byBranch.entries()].sort((a, b) => a[0].localeCompare(b[0]));
                              return sorted.map(([branch, branchMembers]) => {
                                const branchIds = branchMembers.map(m => m.id);
                                const allSelected = branchIds.every(id => selected.includes(id));
                                return (
                                <div key={branch} className="checkbox-branch-group">
                                  <div className="checkbox-branch-header">
                                    <span className="checkbox-branch-label">{branch}</span>
                                    <button
                                      type="button"
                                      className="btn-select-branch"
                                      onClick={() => toggleBranchMembers(ev.id, branchIds)}
                                    >
                                      {allSelected ? 'Deselect all' : 'Select all'}
                                    </button>
                                  </div>
                                  {branchMembers.map((m) => (
                                    <label key={m.id} className="member-checkbox-item">
                                      <input
                                        type="checkbox"
                                        checked={selected.includes(m.id)}
                                        onChange={() => toggleMember(ev.id, m.id)}
                                      />
                                      <span>{m.name}</span>
                                    </label>
                                  ))}
                                </div>
                              );
                              });
                            })()}
                          </div>
                        </div>
                        <button
                          className="btn-register"
                          onClick={() => handleRegister(ev.id)}
                          disabled={selected.length === 0}
                        >
                          Register ({selected.length})
                        </button>
                      </div>
                    );
                  })()}
                </div>
              </div>
            );
          })}
        </div>
      )}

      {isAdmin && (
        <div className="event-form">
          <h3>{editingId !== null ? 'Edit Event' : 'Add Event'}</h3>
          <form onSubmit={handleSubmit}>
            <div className="event-form-grid">
              <label className="full-width">
                Title
                <input name="title" value={form.title} onChange={handleChange} required />
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
              <label className="full-width">
                Description
                <textarea name="description" value={form.description} onChange={handleChange} placeholder="Optional" />
              </label>
              <label className="full-width">
                Notes
                <textarea name="notes" value={form.notes} onChange={handleChange} placeholder="Optional" />
              </label>
            </div>
            {error && <p className="event-form-error">{error}</p>}
            <div className="event-form-actions">
              <button type="submit" className="btn-event-submit">
                {editingId !== null ? 'Update Event' : 'Add Event'}
              </button>
              {editingId !== null && (
                <button type="button" className="btn-event-cancel" onClick={handleCancelEdit}>
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
