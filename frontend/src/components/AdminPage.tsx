import { useEffect, useState } from 'react';
import { fetchAdminUsers, addAdminUser, removeAdminUser, resetPayments, resetCheckins } from '../api';
import type { AdminUserResponse } from '../types';
import { useToast } from './Toast';
import './AdminPage.css';

export default function AdminPage() {
  const [admins, setAdmins] = useState<AdminUserResponse[]>([]);
  const [email, setEmail] = useState('');
  const [name, setName] = useState('');
  const [error, setError] = useState('');
  const { showToast } = useToast();

  const load = () => {
    fetchAdminUsers()
      .then(setAdmins)
      .catch((err) => setError(err.message));
  };

  useEffect(() => { load(); }, []);

  const handleAdd = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    try {
      await addAdminUser({ email: email.trim(), name: name.trim() });
      showToast('Admin added');
      setEmail('');
      setName('');
      load();
    } catch (err: any) {
      setError(err.message || 'Failed to add admin');
      showToast('Failed to add admin', 'error');
    }
  };

  const handleRemove = async (id: number, adminEmail: string) => {
    if (!window.confirm(`Remove admin "${adminEmail}"?`)) return;
    try {
      await removeAdminUser(id);
      showToast('Admin removed');
      load();
    } catch (err) {
      showToast('Failed to remove admin', 'error');
    }
  };

  return (
    <div className="admin-page">
      <div className="page-header">
        <h2>Admin Management</h2>
        <p>Manage who can create and modify content</p>
      </div>

      <div className="admin-list">
        <h3>Current Admins ({admins.length})</h3>
        {admins.length === 0 ? (
          <p className="admin-empty">No admins configured.</p>
        ) : (
          <div className="admin-cards">
            {admins.map((admin) => (
              <div key={admin.id} className="admin-card">
                <div className="admin-info">
                  <span className="admin-name">{admin.name}</span>
                  <span className="admin-email">{admin.email}</span>
                </div>
                <button
                  className="btn-remove-admin"
                  onClick={() => handleRemove(admin.id, admin.email)}
                >
                  Remove
                </button>
              </div>
            ))}
          </div>
        )}
      </div>

      <div className="admin-danger-zone">
        <h3>Danger Zone</h3>
        <p className="admin-danger-desc">These actions are destructive and cannot be undone.</p>
        <div className="admin-danger-actions">
          <div className="admin-danger-item">
            <div>
              <strong>Clear All Check-Ins</strong>
              <span>Reset all families to "not checked in" status</span>
            </div>
            <button
              className="btn-danger"
              onClick={async () => {
                if (!window.confirm('Clear all check-ins? This cannot be undone.')) return;
                try {
                  const res = await resetCheckins();
                  showToast(res.message);
                } catch (err: any) {
                  showToast(err.message || 'Failed to reset check-ins', 'error');
                }
              }}
            >
              Clear Check-Ins
            </button>
          </div>
          <div className="admin-danger-item">
            <div>
              <strong>Delete All Payments</strong>
              <span>Remove all payment records and line items</span>
            </div>
            <button
              className="btn-danger"
              onClick={async () => {
                if (!window.confirm('DELETE all payments? This removes all payment records and ticket data. This cannot be undone.')) return;
                if (!window.confirm('Are you sure? This will delete ALL payment data.')) return;
                try {
                  const res = await resetPayments();
                  showToast(res.message);
                } catch (err: any) {
                  showToast(err.message || 'Failed to reset payments', 'error');
                }
              }}
            >
              Delete All Payments
            </button>
          </div>
        </div>
      </div>

      <div className="admin-form">
        <h3>Add Admin</h3>
        <form onSubmit={handleAdd}>
          <div className="admin-form-grid">
            <label>
              Email
              <input
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="admin@example.com"
                required
              />
            </label>
            <label>
              Name
              <input
                type="text"
                value={name}
                onChange={(e) => setName(e.target.value)}
                placeholder="Admin name"
                required
              />
            </label>
          </div>
          {error && <p className="admin-form-error">{error}</p>}
          <button type="submit" className="btn-add-admin" disabled={!email.trim() || !name.trim()}>
            Add Admin
          </button>
        </form>
      </div>
    </div>
  );
}
