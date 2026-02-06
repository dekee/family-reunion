import { useState, useEffect } from 'react';
import { createRsvp, updateRsvp, fetchFamilyTree } from '../api';
import type { RsvpRequest, RsvpResponse, AgeGroup, AttendeeDto, FlatFamilyMember, FamilyTreeNode } from '../types';
import './RsvpForm.css';

interface Props {
  onSaved: () => void;
  editingRsvp: RsvpResponse | null;
  onCancelEdit: () => void;
}

interface AttendeeFormRow {
  type: 'family' | 'guest';
  familyMemberId?: number;
  guestName: string;
  guestAgeGroup: AgeGroup;
  dietaryNeeds: string;
}

const emptyGuest = (): AttendeeFormRow => ({
  type: 'guest',
  guestName: '',
  guestAgeGroup: 'ADULT',
  dietaryNeeds: '',
});

function flattenTree(nodes: FamilyTreeNode[], parentName?: string): FlatFamilyMember[] {
  const result: FlatFamilyMember[] = [];
  for (const node of nodes) {
    result.push({ id: node.id, name: node.name, ageGroup: node.ageGroup, parentName });
    result.push(...flattenTree(node.children, node.name));
  }
  return result;
}

export default function RsvpForm({ onSaved, editingRsvp, onCancelEdit }: Props) {
  const [familyName, setFamilyName] = useState('');
  const [headOfHouseholdName, setHeadOfHouseholdName] = useState('');
  const [email, setEmail] = useState('');
  const [phone, setPhone] = useState('');
  const [needsLodging, setNeedsLodging] = useState(false);
  const [arrivalDate, setArrivalDate] = useState('');
  const [departureDate, setDepartureDate] = useState('');
  const [notes, setNotes] = useState('');
  const [rows, setRows] = useState<AttendeeFormRow[]>([emptyGuest()]);
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [familyMembers, setFamilyMembers] = useState<FlatFamilyMember[]>([]);

  useEffect(() => {
    fetchFamilyTree()
      .then((tree) => setFamilyMembers(flattenTree(tree.roots)))
      .catch(console.error);
  }, []);

  useEffect(() => {
    if (editingRsvp) {
      setFamilyName(editingRsvp.familyName);
      setHeadOfHouseholdName(editingRsvp.headOfHouseholdName);
      setEmail(editingRsvp.email);
      setPhone(editingRsvp.phone || '');
      setNeedsLodging(editingRsvp.needsLodging);
      setArrivalDate(editingRsvp.arrivalDate || '');
      setDepartureDate(editingRsvp.departureDate || '');
      setNotes(editingRsvp.notes || '');
      setRows(
        editingRsvp.attendees.map((a) =>
          a.familyMemberId
            ? { type: 'family' as const, familyMemberId: a.familyMemberId, guestName: '', guestAgeGroup: 'ADULT' as AgeGroup, dietaryNeeds: a.dietaryNeeds || '' }
            : { type: 'guest' as const, guestName: a.guestName || '', guestAgeGroup: a.guestAgeGroup || 'ADULT', dietaryNeeds: a.dietaryNeeds || '' }
        )
      );
    } else {
      resetForm();
    }
  }, [editingRsvp]);

  const resetForm = () => {
    setFamilyName('');
    setHeadOfHouseholdName('');
    setEmail('');
    setPhone('');
    setNeedsLodging(false);
    setArrivalDate('');
    setDepartureDate('');
    setNotes('');
    setRows([emptyGuest()]);
  };

  const updateRow = (index: number, updates: Partial<AttendeeFormRow>) => {
    setRows((prev) => prev.map((r, i) => (i === index ? { ...r, ...updates } : r)));
  };

  const addFamilyMember = () => {
    setRows((prev) => [...prev, { type: 'family', familyMemberId: undefined, guestName: '', guestAgeGroup: 'ADULT', dietaryNeeds: '' }]);
  };

  const addGuest = () => {
    setRows((prev) => [...prev, emptyGuest()]);
  };

  const removeRow = (index: number) => {
    setRows((prev) => prev.filter((_, i) => i !== index));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setSubmitting(true);

    const attendees: AttendeeDto[] = rows.map((r) =>
      r.type === 'family'
        ? { familyMemberId: r.familyMemberId, dietaryNeeds: r.dietaryNeeds || undefined }
        : { guestName: r.guestName, guestAgeGroup: r.guestAgeGroup, dietaryNeeds: r.dietaryNeeds || undefined }
    );

    const payload: RsvpRequest = {
      familyName,
      headOfHouseholdName,
      email,
      phone: phone || undefined,
      attendees,
      needsLodging,
      arrivalDate: arrivalDate || undefined,
      departureDate: departureDate || undefined,
      notes: notes || undefined,
    };

    try {
      if (editingRsvp) {
        await updateRsvp(editingRsvp.id, payload);
      } else {
        await createRsvp(payload);
      }
      resetForm();
      onSaved();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Something went wrong');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <form className="rsvp-form" onSubmit={handleSubmit}>
      <h2>{editingRsvp ? 'Edit RSVP' : 'Submit Your RSVP'}</h2>

      {error && <div className="form-error">{error}</div>}

      <div className="form-section">
        <h3>Family Information</h3>
        <div className="form-row">
          <label>
            Family Name *
            <input type="text" value={familyName} onChange={(e) => setFamilyName(e.target.value)} required />
          </label>
          <label>
            Head of Household *
            <input type="text" value={headOfHouseholdName} onChange={(e) => setHeadOfHouseholdName(e.target.value)} required />
          </label>
        </div>
        <div className="form-row">
          <label>
            Email *
            <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} required />
          </label>
          <label>
            Phone
            <input type="tel" value={phone} onChange={(e) => setPhone(e.target.value)} />
          </label>
        </div>
      </div>

      <div className="form-section">
        <h3>Who's Attending</h3>
        {rows.map((row, index) => (
          <div key={index} className="attendee-row">
            {row.type === 'family' ? (
              <select
                className="attendee-select"
                value={row.familyMemberId ?? ''}
                onChange={(e) => updateRow(index, { familyMemberId: Number(e.target.value) || undefined })}
                required
              >
                <option value="">Select family member...</option>
                {familyMembers.map((m) => (
                  <option key={m.id} value={m.id}>
                    {m.name}{m.parentName ? ` (child of ${m.parentName})` : ''}
                  </option>
                ))}
              </select>
            ) : (
              <>
                <input
                  type="text"
                  placeholder="Guest name"
                  value={row.guestName}
                  onChange={(e) => updateRow(index, { guestName: e.target.value })}
                  required
                />
                <select
                  value={row.guestAgeGroup}
                  onChange={(e) => updateRow(index, { guestAgeGroup: e.target.value as AgeGroup })}
                >
                  <option value="ADULT">Adult</option>
                  <option value="CHILD">Child</option>
                  <option value="INFANT">Infant</option>
                </select>
              </>
            )}
            <input
              type="text"
              placeholder="Dietary needs"
              value={row.dietaryNeeds}
              onChange={(e) => updateRow(index, { dietaryNeeds: e.target.value })}
            />
            <span className="attendee-type-badge">{row.type === 'family' ? 'Family' : 'Guest'}</span>
            {rows.length > 1 && (
              <button type="button" className="btn-remove" onClick={() => removeRow(index)}>
                Remove
              </button>
            )}
          </div>
        ))}
        <div className="attendee-add-buttons">
          <button type="button" className="btn-add" onClick={addFamilyMember}>
            + Add Family Member
          </button>
          <button type="button" className="btn-add" onClick={addGuest}>
            + Add Guest
          </button>
        </div>
      </div>

      <div className="form-section">
        <h3>Details</h3>
        <div className="form-row">
          <label>
            Arrival Date
            <input type="date" value={arrivalDate} onChange={(e) => setArrivalDate(e.target.value)} />
          </label>
          <label>
            Departure Date
            <input type="date" value={departureDate} onChange={(e) => setDepartureDate(e.target.value)} />
          </label>
        </div>
        <label className="checkbox-label">
          <input type="checkbox" checked={needsLodging} onChange={(e) => setNeedsLodging(e.target.checked)} />
          We need lodging
        </label>
        <label>
          Notes
          <textarea value={notes} onChange={(e) => setNotes(e.target.value)} rows={3} />
        </label>
      </div>

      <div className="form-actions">
        <button type="submit" className="btn-submit" disabled={submitting}>
          {submitting ? 'Saving...' : editingRsvp ? 'Update RSVP' : 'Submit RSVP'}
        </button>
        {editingRsvp && (
          <button type="button" className="btn-cancel" onClick={onCancelEdit}>
            Cancel
          </button>
        )}
      </div>
    </form>
  );
}
