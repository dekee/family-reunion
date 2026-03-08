import { useEffect, useState } from 'react';
import { getBranchColor } from '../branchColors';
import './RegistrationModal.css';

interface FlatMember {
  id: number;
  name: string;
  branchName: string;
}

interface Props {
  eventTitle: string;
  available: FlatMember[];
  onRegister: (memberIds: number[]) => void;
  onClose: () => void;
}

export default function RegistrationModal({ eventTitle, available, onRegister, onClose }: Props) {
  const [selected, setSelected] = useState<number[]>([]);
  const [search, setSearch] = useState('');

  useEffect(() => {
    document.body.style.overflow = 'hidden';
    const handleKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') onClose();
    };
    document.addEventListener('keydown', handleKey);
    return () => {
      document.body.style.overflow = '';
      document.removeEventListener('keydown', handleKey);
    };
  }, [onClose]);

  const query = search.toLowerCase().trim();
  const filtered = query
    ? available.filter(m => m.name.toLowerCase().includes(query))
    : available;

  const byBranch = new Map<string, FlatMember[]>();
  for (const m of filtered) {
    const list = byBranch.get(m.branchName) || [];
    list.push(m);
    byBranch.set(m.branchName, list);
  }
  const sortedBranches = [...byBranch.entries()].sort((a, b) => a[0].localeCompare(b[0]));

  const toggleMember = (id: number) => {
    setSelected(prev =>
      prev.includes(id) ? prev.filter(x => x !== id) : [...prev, id]
    );
  };

  const toggleBranch = (ids: number[]) => {
    const allSelected = ids.every(id => selected.includes(id));
    if (allSelected) {
      setSelected(prev => prev.filter(id => !ids.includes(id)));
    } else {
      setSelected(prev => [...new Set([...prev, ...ids])]);
    }
  };

  const handleSubmit = () => {
    if (selected.length > 0) {
      onRegister(selected);
    }
  };

  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div className="modal-content" onClick={e => e.stopPropagation()}>
        <div className="modal-header">
          <h3>Register Members</h3>
          <p className="modal-subtitle">{eventTitle}</p>
          <button className="modal-close" onClick={onClose}>&times;</button>
        </div>

        {available.length > 10 && (
          <div className="modal-search">
            <input
              type="text"
              placeholder="Search members..."
              value={search}
              onChange={e => setSearch(e.target.value)}
              autoFocus
            />
            {search && (
              <button className="modal-search-clear" onClick={() => setSearch('')}>&times;</button>
            )}
          </div>
        )}

        <div className="modal-body">
          {sortedBranches.map(([branch, members]) => {
            const branchIds = members.map(m => m.id);
            const allSelected = branchIds.every(id => selected.includes(id));
            return (
              <div key={branch} className="modal-branch-group">
                <div
                  className="modal-branch-header"
                  style={{ borderLeftColor: getBranchColor(branch) }}
                >
                  <span className="modal-branch-label">{branch}</span>
                  <button
                    type="button"
                    className="btn-select-branch"
                    onClick={() => toggleBranch(branchIds)}
                  >
                    {allSelected ? 'Deselect all' : 'Select all'}
                  </button>
                </div>
                {members.map(m => (
                  <label key={m.id} className="modal-member-item">
                    <input
                      type="checkbox"
                      checked={selected.includes(m.id)}
                      onChange={() => toggleMember(m.id)}
                    />
                    <span>{m.name}</span>
                  </label>
                ))}
              </div>
            );
          })}
          {filtered.length === 0 && (
            <p className="modal-empty">No members match your search.</p>
          )}
        </div>

        <div className="modal-footer">
          <button className="btn-modal-cancel" onClick={onClose}>Cancel</button>
          <button
            className="btn-modal-register"
            onClick={handleSubmit}
            disabled={selected.length === 0}
          >
            Register ({selected.length})
          </button>
        </div>
      </div>
    </div>
  );
}
