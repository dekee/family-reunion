import { useEffect, useState, useCallback } from 'react';
import { fetchFamilyTree, createFamilyMember, updateFamilyMember, deleteFamilyMember } from '../api';
import { useAuth } from '../AuthContext';
import { getBranchColor } from '../branchColors';
import type { FamilyTreeNode, AgeGroup, FamilyMemberRequest } from '../types';
import { useToast } from './Toast';
import { SkeletonCard } from './Skeleton';
import './FamilyMembers.css';
import './RegistrationModal.css';

interface FlatMember {
  id: number;
  name: string;
  ageGroup: AgeGroup;
  parentId?: number;
  generation: number | null;
  depth: number;
}

function flattenTree(node: FamilyTreeNode, depth: number): FlatMember[] {
  const result: FlatMember[] = [{
    id: node.id,
    name: node.name,
    ageGroup: node.ageGroup,
    parentId: node.parentId,
    generation: node.generation,
    depth,
  }];
  for (const child of node.children) {
    result.push(...flattenTree(child, depth + 1));
  }
  return result;
}

export default function FamilyMembers() {
  const [roots, setRoots] = useState<FamilyTreeNode[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  // Modal form state
  const [showModal, setShowModal] = useState(false);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [formName, setFormName] = useState('');
  const [formAgeGroup, setFormAgeGroup] = useState<AgeGroup>('ADULT');
  const [formParentId, setFormParentId] = useState<number | string>('');
  const [formError, setFormError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);

  // Search
  const [searchQuery, setSearchQuery] = useState('');

  // Collapsed branches
  const [collapsed, setCollapsed] = useState<Set<number>>(new Set());
  const { showToast } = useToast();
  const { isAdmin } = useAuth();

  // Promote children of root nodes (Wesley & Esther) as top-level branches
  const branches: FamilyTreeNode[] = roots.flatMap(r => r.children);

  const allMembers: FlatMember[] = [];
  for (const branch of branches) {
    allMembers.push(...flattenTree(branch, 0));
  }

  const loadTree = useCallback(() => {
    fetchFamilyTree()
      .then((res) => {
        setRoots(res.roots);
        setLoading(false);
      })
      .catch((err) => {
        setError(err.message);
        setLoading(false);
      });
  }, []);

  useEffect(() => {
    loadTree();
  }, [loadTree]);

  const closeModal = () => {
    setShowModal(false);
    setEditingId(null);
    setFormName('');
    setFormAgeGroup('ADULT');
    setFormParentId('');
    setFormError(null);
  };

  const handleAddChild = (parentId: number) => {
    closeModal();
    setFormParentId(parentId);
    setShowModal(true);
  };

  const handleEdit = (member: FlatMember) => {
    setEditingId(member.id);
    setFormName(member.name);
    setFormAgeGroup(member.ageGroup);
    setFormParentId(member.parentId ?? '');
    setFormError(null);
    setShowModal(true);
  };

  const handleDelete = async (member: FlatMember) => {
    if (!window.confirm(`Delete "${member.name}"? This will also delete all their children.`)) return;
    try {
      await deleteFamilyMember(member.id);
      showToast(`${member.name} deleted`);
      loadTree();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to delete');
      showToast('Failed to delete member', 'error');
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    const name = formName.trim();
    if (!name) return;

    setSaving(true);
    setFormError(null);

    try {
      if (editingId) {
        await updateFamilyMember(editingId, { name, ageGroup: formAgeGroup });
        showToast('Member updated');
      } else {
        const data: FamilyMemberRequest = {
          name,
          ageGroup: formAgeGroup,
        };
        if (formParentId !== '') {
          data.parentId = Number(formParentId);
          const parent = allMembers.find(m => m.id === data.parentId);
          if (parent?.generation != null) {
            data.generation = parent.generation + 1;
          }
        }
        await createFamilyMember(data);
        showToast('Member added');
      }
      loadTree();
      closeModal();
    } catch (err) {
      setFormError(err instanceof Error ? err.message : 'Failed to save');
      showToast('Failed to save member', 'error');
    } finally {
      setSaving(false);
    }
  };

  const toggleBranch = (id: number) => {
    setCollapsed(prev => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
  };

  const collapseAll = () => {
    setCollapsed(new Set(branches.map(b => b.id)));
  };

  const expandAll = () => {
    setCollapsed(new Set());
  };

  if (loading) return (
    <div className="members-page">
      <h2>Family Members</h2>
      <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
        <SkeletonCard lines={3} />
        <SkeletonCard lines={5} />
        <SkeletonCard lines={5} />
      </div>
    </div>
  );
  if (error && roots.length === 0) return <div className="members-error">Error: {error}</div>;

  return (
    <div className="members-page">
      <h2>Family Members</h2>
      <p className="members-count">{allMembers.length} members across {branches.length} branches</p>

      <div className="members-controls">
        <button onClick={expandAll}>Expand All</button>
        <button onClick={collapseAll}>Collapse All</button>
      </div>

      <div className="members-search-wrapper">
        <input
          type="text"
          className="members-search"
          placeholder="Search members..."
          value={searchQuery}
          onChange={e => setSearchQuery(e.target.value)}
        />
        {searchQuery && (
          <button className="members-search-clear" onClick={() => setSearchQuery('')}>&times;</button>
        )}
      </div>

      {isAdmin && showModal && (
        <div className="modal-backdrop" onClick={closeModal}>
          <div className="modal-content" onClick={e => e.stopPropagation()}>
            <div className="modal-header">
              <h3>{editingId ? 'Edit Member' : 'Add Person'}</h3>
              {!editingId && formParentId !== '' && (
                <p className="modal-subtitle">
                  Under {allMembers.find(m => m.id === Number(formParentId))?.name}
                </p>
              )}
              <button className="modal-close" onClick={closeModal}>&times;</button>
            </div>
            <form onSubmit={handleSubmit}>
              <div className="modal-body">
                <div className="member-form-grid-modal">
                  <label>
                    Name
                    <input
                      type="text"
                      value={formName}
                      onChange={e => setFormName(e.target.value)}
                      placeholder="Member name"
                      required
                      autoFocus
                    />
                  </label>
                  <label>
                    Age Group
                    <select value={formAgeGroup} onChange={e => setFormAgeGroup(e.target.value as AgeGroup)}>
                      <option value="ADULT">Adult</option>
                      <option value="SPOUSE">Spouse</option>
                      <option value="CHILD">Child</option>
                      <option value="INFANT">Infant</option>
                    </select>
                  </label>
                  {!editingId && (
                    <label>
                      Parent
                      <select
                        value={formParentId}
                        onChange={e => setFormParentId(e.target.value)}
                      >
                        <option value="">— No parent (root) —</option>
                        {allMembers.map(m => (
                          <option key={m.id} value={m.id}>
                            {'  '.repeat(m.depth)}{m.name}
                          </option>
                        ))}
                      </select>
                    </label>
                  )}
                </div>
                {formError && <p className="member-form-error">{formError}</p>}
              </div>
              <div className="modal-footer">
                <button type="button" className="btn-modal-cancel" onClick={closeModal}>
                  Cancel
                </button>
                <button type="submit" className="btn-modal-register" disabled={saving || !formName.trim()}>
                  {saving ? 'Saving...' : editingId ? 'Update' : 'Add Person'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {(() => {
        const query = searchQuery.toLowerCase().trim();
        const isSearching = query.length > 0;
        let matchCount = 0;

        const branchElements = branches.map(root => {
          const branchMembers = flattenTree(root, 0);
          const filtered = isSearching
            ? branchMembers.filter(m => m.name.toLowerCase().includes(query))
            : branchMembers;

          if (isSearching && filtered.length === 0) return null;
          matchCount += filtered.length;

          const isCollapsed = isSearching ? false : collapsed.has(root.id);
          const displayMembers = isSearching ? filtered : branchMembers;
          const branchColor = getBranchColor(root.name);

          return (
            <div
              className="branch-section"
              key={root.id}
              style={{ borderLeft: `4px solid ${branchColor}` }}
            >
              <div className="branch-header" onClick={() => toggleBranch(root.id)}>
                <h3>
                  {root.name}
                  <span
                    className="branch-count-badge"
                    style={{ background: branchColor }}
                  >
                    {filtered.length}
                  </span>
                  {isSearching && filtered.length !== branchMembers.length && (
                    <span className="branch-count"> of {branchMembers.length}</span>
                  )}
                </h3>
                <span className={`branch-toggle ${isCollapsed ? '' : 'open'}`}>&#9654;</span>
              </div>
              {!isCollapsed && (
                <div className="branch-members">
                  {displayMembers.map(member => (
                    <div className="member-row" key={member.id}>
                      <span className="member-indent" style={{ width: member.depth * 24 }} />
                      <div className="member-info">
                        <span className="member-name">{member.name}</span>
                        <span className="member-age-group">{member.ageGroup}</span>
                      </div>
                      {isAdmin && (
                        <div className="member-actions">
                          <button
                            className="btn-member-action btn-add-child"
                            onClick={() => handleAddChild(member.id)}
                            title={`Add person under ${member.name}`}
                          >
                            + Person
                          </button>
                          <button
                            className="btn-member-action btn-member-edit"
                            onClick={() => handleEdit(member)}
                            title="Edit"
                          >
                            Edit
                          </button>
                          <button
                            className="btn-member-action btn-member-delete"
                            onClick={() => handleDelete(member)}
                            title="Delete"
                          >
                            Delete
                          </button>
                        </div>
                      )}
                    </div>
                  ))}
                </div>
              )}
            </div>
          );
        });

        return (
          <>
            {isSearching && (
              <p className="members-search-result">Showing {matchCount} of {allMembers.length} members</p>
            )}
            {branchElements}
          </>
        );
      })()}
    </div>
  );
}
