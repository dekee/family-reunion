import { useEffect, useState, useRef, useCallback } from 'react';
import { fetchFamilyTree, createFamilyMember, updateFamilyMember, deleteFamilyMember } from '../api';
import { useAuth } from '../AuthContext';
import type { FamilyTreeNode, AgeGroup, FamilyMemberRequest } from '../types';
import { useToast } from './Toast';
import { SkeletonCard } from './Skeleton';
import './FamilyMembers.css';

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
  const [totalMembers, setTotalMembers] = useState(0);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  // Form state
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

  const formRef = useRef<HTMLDivElement>(null);

  const allMembers: FlatMember[] = [];
  for (const root of roots) {
    allMembers.push(...flattenTree(root, 0));
  }

  const loadTree = useCallback(() => {
    fetchFamilyTree()
      .then((res) => {
        setRoots(res.roots);
        setTotalMembers(res.totalMembers);
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

  const resetForm = () => {
    setEditingId(null);
    setFormName('');
    setFormAgeGroup('ADULT');
    setFormParentId('');
    setFormError(null);
  };

  const scrollToForm = () => {
    formRef.current?.scrollIntoView({ behavior: 'smooth' });
  };

  const handleAddChild = (parentId: number) => {
    resetForm();
    setFormParentId(parentId);
    scrollToForm();
  };

  const handleEdit = (member: FlatMember) => {
    setEditingId(member.id);
    setFormName(member.name);
    setFormAgeGroup(member.ageGroup);
    setFormParentId(member.parentId ?? '');
    setFormError(null);
    scrollToForm();
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
      resetForm();
      loadTree();
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
      <p className="members-count">{totalMembers} members across {roots.length} branches</p>

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

      {isAdmin && (
        <div className="member-form" ref={formRef}>
          <h3>{editingId ? 'Edit Member' : 'Add Member'}</h3>
          <form onSubmit={handleSubmit}>
            <div className="member-form-grid">
              <label>
                Name
                <input
                  type="text"
                  value={formName}
                  onChange={e => setFormName(e.target.value)}
                  placeholder="Member name"
                  required
                  autoFocus={!!editingId}
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
            <div className="member-form-actions">
              <button type="submit" className="btn-member-submit" disabled={saving || !formName.trim()}>
                {saving ? 'Saving...' : editingId ? 'Update Member' : 'Add Member'}
              </button>
              {editingId && (
                <button type="button" className="btn-member-cancel" onClick={resetForm}>
                  Cancel
                </button>
              )}
            </div>
          </form>
        </div>
      )}

      {(() => {
        const query = searchQuery.toLowerCase().trim();
        const isSearching = query.length > 0;
        let matchCount = 0;

        const branchElements = roots.map(root => {
          const branchMembers = flattenTree(root, 0);
          const filtered = isSearching
            ? branchMembers.filter(m => m.name.toLowerCase().includes(query))
            : branchMembers;

          if (isSearching && filtered.length === 0) return null;
          matchCount += filtered.length;

          const isCollapsed = isSearching ? false : collapsed.has(root.id);
          const displayMembers = isSearching ? filtered : branchMembers;

          return (
            <div className="branch-section" key={root.id}>
              <div className="branch-header" onClick={() => toggleBranch(root.id)}>
                <h3>
                  {root.name}
                  <span className="branch-count">({filtered.length}{isSearching && filtered.length !== branchMembers.length ? ` of ${branchMembers.length}` : ''})</span>
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
                            title={`Add child under ${member.name}`}
                          >
                            + Child
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
              <p className="members-search-result">Showing {matchCount} of {totalMembers} members</p>
            )}
            {branchElements}
          </>
        );
      })()}
    </div>
  );
}
