import { useState, useEffect } from 'react';
import { createRsvp, updateRsvp } from '../api';
import type { RsvpRequest, RsvpResponse, AgeGroup, FamilyMemberDto } from '../types';
import './RsvpForm.css';

interface Props {
  onSaved: () => void;
  editingRsvp: RsvpResponse | null;
  onCancelEdit: () => void;
}

const emptyMember = (): FamilyMemberDto => ({
  name: '',
  ageGroup: 'ADULT',
  dietaryNeeds: '',
});

const emptyForm = (): RsvpRequest => ({
  familyName: '',
  headOfHouseholdName: '',
  email: '',
  phone: '',
  familyMembers: [emptyMember()],
  needsLodging: false,
  arrivalDate: '',
  departureDate: '',
  notes: '',
});

export default function RsvpForm({ onSaved, editingRsvp, onCancelEdit }: Props) {
  const [form, setForm] = useState<RsvpRequest>(emptyForm());
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    if (editingRsvp) {
      setForm({
        familyName: editingRsvp.familyName,
        headOfHouseholdName: editingRsvp.headOfHouseholdName,
        email: editingRsvp.email,
        phone: editingRsvp.phone || '',
        familyMembers: editingRsvp.familyMembers.map((m) => ({
          name: m.name,
          ageGroup: m.ageGroup,
          dietaryNeeds: m.dietaryNeeds || '',
        })),
        needsLodging: editingRsvp.needsLodging,
        arrivalDate: editingRsvp.arrivalDate || '',
        departureDate: editingRsvp.departureDate || '',
        notes: editingRsvp.notes || '',
      });
    } else {
      setForm(emptyForm());
    }
  }, [editingRsvp]);

  const updateField = (field: keyof RsvpRequest, value: string | boolean) => {
    setForm((prev) => ({ ...prev, [field]: value }));
  };

  const updateMember = (index: number, field: keyof FamilyMemberDto, value: string) => {
    setForm((prev) => {
      const members = [...prev.familyMembers];
      members[index] = { ...members[index], [field]: value };
      return { ...prev, familyMembers: members };
    });
  };

  const addMember = () => {
    setForm((prev) => ({
      ...prev,
      familyMembers: [...prev.familyMembers, emptyMember()],
    }));
  };

  const removeMember = (index: number) => {
    setForm((prev) => ({
      ...prev,
      familyMembers: prev.familyMembers.filter((_, i) => i !== index),
    }));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setSubmitting(true);

    const payload: RsvpRequest = {
      ...form,
      phone: form.phone || undefined,
      arrivalDate: form.arrivalDate || undefined,
      departureDate: form.departureDate || undefined,
      notes: form.notes || undefined,
      familyMembers: form.familyMembers.map((m) => ({
        ...m,
        dietaryNeeds: m.dietaryNeeds || undefined,
      })),
    };

    try {
      if (editingRsvp) {
        await updateRsvp(editingRsvp.id, payload);
      } else {
        await createRsvp(payload);
      }
      setForm(emptyForm());
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
            <input
              type="text"
              value={form.familyName}
              onChange={(e) => updateField('familyName', e.target.value)}
              required
            />
          </label>
          <label>
            Head of Household *
            <input
              type="text"
              value={form.headOfHouseholdName}
              onChange={(e) => updateField('headOfHouseholdName', e.target.value)}
              required
            />
          </label>
        </div>
        <div className="form-row">
          <label>
            Email *
            <input
              type="email"
              value={form.email}
              onChange={(e) => updateField('email', e.target.value)}
              required
            />
          </label>
          <label>
            Phone
            <input
              type="tel"
              value={form.phone}
              onChange={(e) => updateField('phone', e.target.value)}
            />
          </label>
        </div>
      </div>

      <div className="form-section">
        <h3>Family Members</h3>
        {form.familyMembers.map((member, index) => (
          <div key={index} className="member-row">
            <input
              type="text"
              placeholder="Name"
              value={member.name}
              onChange={(e) => updateMember(index, 'name', e.target.value)}
              required
            />
            <select
              value={member.ageGroup}
              onChange={(e) => updateMember(index, 'ageGroup', e.target.value as AgeGroup)}
            >
              <option value="ADULT">Adult</option>
              <option value="CHILD">Child</option>
              <option value="INFANT">Infant</option>
            </select>
            <input
              type="text"
              placeholder="Dietary needs"
              value={member.dietaryNeeds || ''}
              onChange={(e) => updateMember(index, 'dietaryNeeds', e.target.value)}
            />
            {form.familyMembers.length > 1 && (
              <button type="button" className="btn-remove" onClick={() => removeMember(index)}>
                Remove
              </button>
            )}
          </div>
        ))}
        <button type="button" className="btn-add" onClick={addMember}>
          + Add Family Member
        </button>
      </div>

      <div className="form-section">
        <h3>Details</h3>
        <div className="form-row">
          <label>
            Arrival Date
            <input
              type="date"
              value={form.arrivalDate}
              onChange={(e) => updateField('arrivalDate', e.target.value)}
            />
          </label>
          <label>
            Departure Date
            <input
              type="date"
              value={form.departureDate}
              onChange={(e) => updateField('departureDate', e.target.value)}
            />
          </label>
        </div>
        <label className="checkbox-label">
          <input
            type="checkbox"
            checked={form.needsLodging}
            onChange={(e) => updateField('needsLodging', e.target.checked)}
          />
          We need lodging
        </label>
        <label>
          Notes
          <textarea
            value={form.notes}
            onChange={(e) => updateField('notes', e.target.value)}
            rows={3}
          />
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
