import { useEffect, useState } from 'react';
import { fetchAllRsvps, deleteRsvp } from '../api';
import type { RsvpResponse } from '../types';
import './RsvpList.css';

interface Props {
  refreshKey: number;
  onEdit: (rsvp: RsvpResponse) => void;
  onDeleted: () => void;
}

export default function RsvpList({ refreshKey, onEdit, onDeleted }: Props) {
  const [rsvps, setRsvps] = useState<RsvpResponse[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    setLoading(true);
    fetchAllRsvps()
      .then(setRsvps)
      .catch(console.error)
      .finally(() => setLoading(false));
  }, [refreshKey]);

  const handleDelete = async (id: number, familyName: string) => {
    if (!window.confirm(`Remove the ${familyName} family RSVP?`)) return;
    try {
      await deleteRsvp(id);
      onDeleted();
    } catch (err) {
      console.error(err);
    }
  };

  if (loading) return <p>Loading RSVPs...</p>;
  if (rsvps.length === 0) return <p className="empty-state">No RSVPs yet. Be the first to sign up!</p>;

  return (
    <div className="rsvp-list">
      <h2>RSVPs ({rsvps.length} families)</h2>
      <div className="rsvp-cards">
        {rsvps.map((rsvp) => (
          <div key={rsvp.id} className="rsvp-card">
            <div className="card-header">
              <h3>{rsvp.familyName} Family</h3>
              <div className="card-actions">
                <button className="btn-edit" onClick={() => onEdit(rsvp)}>Edit</button>
                <button className="btn-delete" onClick={() => handleDelete(rsvp.id, rsvp.familyName)}>Delete</button>
              </div>
            </div>
            <p className="card-contact">
              <strong>{rsvp.headOfHouseholdName}</strong> &middot; {rsvp.email}
              {rsvp.phone && <> &middot; {rsvp.phone}</>}
            </p>
            <div className="card-members">
              <strong>Attending ({rsvp.familyMembers.length}):</strong>
              <ul>
                {rsvp.familyMembers.map((m, i) => (
                  <li key={i}>
                    {m.name} <span className="age-badge">{m.ageGroup}</span>
                    {m.dietaryNeeds && <span className="dietary"> — {m.dietaryNeeds}</span>}
                  </li>
                ))}
              </ul>
            </div>
            <div className="card-details">
              {rsvp.needsLodging && <span className="badge lodging">Needs Lodging</span>}
              {rsvp.arrivalDate && <span className="badge">Arrives {rsvp.arrivalDate}</span>}
              {rsvp.departureDate && <span className="badge">Departs {rsvp.departureDate}</span>}
            </div>
            {rsvp.notes && <p className="card-notes">{rsvp.notes}</p>}
          </div>
        ))}
      </div>
    </div>
  );
}
