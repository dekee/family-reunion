import { useEffect, useState } from 'react';
import {
  fetchVolunteerTasks, createVolunteerTask, updateVolunteerTask, deleteVolunteerTask,
  signUpForTask, withdrawFromTask, fetchEvents, fetchFamilyTree,
} from '../api';
import { useAuth } from '../AuthContext';
import { getBranchColor } from '../branchColors';
import type { EventResponse, FamilyTreeNode, VolunteerTaskRequest, VolunteerTaskResponse } from '../types';
import { useToast } from './Toast';
import { SkeletonCard } from './Skeleton';
import RegistrationModal from './RegistrationModal';
import './Volunteer.css';
import './RegistrationModal.css';

const emptyForm: VolunteerTaskRequest = {
  title: '',
  description: '',
  eventId: 0,
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
    if (!n.excludeFromRsvp) {
      result.push({ id: n.id, name: n.name, branchName: branch });
    }
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

interface EventGroup {
  eventId: number;
  eventTitle: string;
  eventDateTime: string;
  tasks: VolunteerTaskResponse[];
}

function groupByEvent(tasks: VolunteerTaskResponse[]): EventGroup[] {
  const groups = new Map<number, EventGroup>();
  for (const t of tasks) {
    const g = groups.get(t.eventId) || {
      eventId: t.eventId,
      eventTitle: t.eventTitle,
      eventDateTime: t.eventDateTime,
      tasks: [],
    };
    g.tasks.push(t);
    groups.set(t.eventId, g);
  }
  return [...groups.values()].sort((a, b) => a.eventDateTime.localeCompare(b.eventDateTime));
}

export default function Volunteer() {
  const [tasks, setTasks] = useState<VolunteerTaskResponse[]>([]);
  const [events, setEvents] = useState<EventResponse[]>([]);
  const [members, setMembers] = useState<FlatMember[]>([]);
  const [loading, setLoading] = useState(true);
  const [form, setForm] = useState<VolunteerTaskRequest>(emptyForm);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [error, setError] = useState('');
  const [signingUpTaskId, setSigningUpTaskId] = useState<number | null>(null);
  const [showFormModal, setShowFormModal] = useState(false);
  const { showToast } = useToast();
  const { isAdmin } = useAuth();

  const load = () => {
    setLoading(true);
    Promise.all([fetchVolunteerTasks(), fetchEvents(), fetchFamilyTree()])
      .then(([taskList, evts, tree]) => {
        setTasks(taskList);
        setEvents(evts);
        setMembers(flattenTree(tree.roots));
      })
      .catch(console.error)
      .finally(() => setLoading(false));
  };

  useEffect(() => { load(); }, []);

  const handleChange = (
    e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement>
  ) => {
    const { name, value } = e.target;
    setForm((f) => ({ ...f, [name]: name === 'eventId' ? Number(value) : value }));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    if (!form.eventId) {
      setError('Please choose an event');
      return;
    }
    try {
      if (editingId !== null) {
        await updateVolunteerTask(editingId, form);
        showToast('Task updated');
      } else {
        await createVolunteerTask(form);
        showToast('Task created');
      }
      setForm(emptyForm);
      setEditingId(null);
      setShowFormModal(false);
      load();
    } catch (err: any) {
      setError(err.message || 'Failed to save task');
      showToast('Failed to save task', 'error');
    }
  };

  const handleEdit = (task: VolunteerTaskResponse) => {
    setEditingId(task.id);
    setForm({
      title: task.title,
      description: task.description || '',
      eventId: task.eventId,
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
    if (!window.confirm(`Delete the volunteer task "${title}"?`)) return;
    try {
      await deleteVolunteerTask(id);
      showToast('Task deleted');
      load();
    } catch (err) {
      showToast('Failed to delete task', 'error');
    }
  };

  const handleModalSignUp = async (taskId: number, memberIds: number[]) => {
    try {
      await signUpForTask(taskId, { familyMemberIds: memberIds });
      showToast(`${memberIds.length} volunteer${memberIds.length > 1 ? 's' : ''} signed up`);
      setSigningUpTaskId(null);
      load();
    } catch (err) {
      showToast('Failed to sign up', 'error');
    }
  };

  const handleWithdraw = async (taskId: number, memberId: number) => {
    try {
      await withdrawFromTask(taskId, memberId);
      showToast('Volunteer removed');
      load();
    } catch (err) {
      showToast('Failed to remove volunteer', 'error');
    }
  };

  const eventGroups = groupByEvent(tasks);

  return (
    <div className="volunteer-page">
      <div className="page-header">
        <h2>Volunteer</h2>
        <p>Lend a hand! Sign up to help out at reunion events.</p>
        {isAdmin && (
          <button className="btn-add-task" onClick={handleAddNew}>+ Add Task</button>
        )}
      </div>

      {loading ? (
        <div className="vol-task-cards">
          <SkeletonCard lines={4} />
          <SkeletonCard lines={4} />
        </div>
      ) : eventGroups.length === 0 ? (
        <p className="volunteer-empty">No volunteer tasks yet. Check back soon!</p>
      ) : (
        eventGroups.map((group) => (
          <section key={group.eventId} className="vol-event-group">
            <div className="vol-event-header">
              <h3>{group.eventTitle}</h3>
              <p className="vol-event-date">{formatDateTime(group.eventDateTime)}</p>
            </div>
            <div className="vol-task-cards">
              {group.tasks.map((task) => {
                const signedUpIds = new Set(task.signups.map((s) => s.familyMemberId));
                const available = members.filter((m) => !signedUpIds.has(m.id));

                return (
                  <div key={task.id} className="vol-task-card">
                    <div className="vol-task-card-header">
                      <h4>{task.title}</h4>
                      {isAdmin && (
                        <div className="vol-task-actions">
                          <button className="btn-edit" onClick={() => handleEdit(task)}>Edit</button>
                          <button className="btn-delete" onClick={() => handleDelete(task.id, task.title)}>Delete</button>
                        </div>
                      )}
                    </div>

                    {task.description && <p className="vol-task-description">{task.description}</p>}

                    <div className="vol-task-signups">
                      <div className="vol-signup-header">
                        <strong>Volunteers ({task.signupCount})</strong>
                        {available.length > 0 && (
                          <button
                            className="btn-signup-open"
                            onClick={() => setSigningUpTaskId(task.id)}
                          >
                            Sign Up
                          </button>
                        )}
                      </div>
                      {task.signups.length > 0 && (() => {
                        const memberMap = new Map(members.map(m => [m.id, m]));
                        const grouped = new Map<string, typeof task.signups>();
                        for (const s of task.signups) {
                          const branch = memberMap.get(s.familyMemberId)?.branchName || 'Other';
                          const list = grouped.get(branch) || [];
                          list.push(s);
                          grouped.set(branch, list);
                        }
                        const sortedBranches = [...grouped.entries()].sort((a, b) => a[0].localeCompare(b[0]));

                        return (
                          <div className="vol-signups-grouped">
                            {sortedBranches.map(([branch, signups]) => (
                              <div
                                key={branch}
                                className="vol-branch-group"
                                style={{ borderLeft: `3px solid ${getBranchColor(branch)}`, paddingLeft: '0.75rem' }}
                              >
                                <span className="vol-branch-label">{branch}</span>
                                <div className="vol-signup-badges">
                                  {signups.map((s) => (
                                    <span key={s.id} className="vol-badge">
                                      {s.familyMemberName}
                                      <button
                                        className="vol-badge-remove"
                                        onClick={() => handleWithdraw(task.id, s.familyMemberId)}
                                        title={`Remove ${s.familyMemberName}`}
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
          </section>
        ))
      )}

      {signingUpTaskId !== null && (() => {
        const task = tasks.find(t => t.id === signingUpTaskId);
        if (!task) return null;
        const signedUpIds = new Set(task.signups.map(s => s.familyMemberId));
        const available = members.filter(m => !signedUpIds.has(m.id));
        return (
          <RegistrationModal
            title="Sign Up to Volunteer"
            actionLabel="Sign Up"
            eventTitle={`${task.title} — ${task.eventTitle}`}
            available={available}
            onRegister={(ids) => handleModalSignUp(task.id, ids)}
            onClose={() => setSigningUpTaskId(null)}
          />
        );
      })()}

      {isAdmin && showFormModal && (
        <div className="modal-backdrop" onClick={closeFormModal}>
          <div className="modal-content vol-task-modal" onClick={e => e.stopPropagation()}>
            <div className="modal-header">
              <h3>{editingId !== null ? 'Edit Task' : 'Add Task'}</h3>
              <button className="modal-close" onClick={closeFormModal}>&times;</button>
            </div>
            <form onSubmit={handleSubmit}>
              <div className="modal-body">
                <div className="vol-form-grid">
                  <label>
                    Task
                    <input name="title" value={form.title} onChange={handleChange} required autoFocus />
                  </label>
                  <label>
                    Event
                    <select name="eventId" value={form.eventId || ''} onChange={handleChange} required>
                      <option value="" disabled>Choose an event…</option>
                      {events.map((ev) => (
                        <option key={ev.id} value={ev.id}>
                          {ev.title} — {formatDateTime(ev.eventDateTime)}
                        </option>
                      ))}
                    </select>
                  </label>
                  <label>
                    Description
                    <textarea name="description" value={form.description} onChange={handleChange} placeholder="Optional" />
                  </label>
                </div>
                {error && <p className="vol-form-error">{error}</p>}
              </div>
              <div className="modal-footer">
                <button type="button" className="btn-modal-cancel" onClick={closeFormModal}>Cancel</button>
                <button type="submit" className="btn-modal-register">
                  {editingId !== null ? 'Update Task' : 'Add Task'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
