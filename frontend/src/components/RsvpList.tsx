import { useEffect, useState } from 'react';
import { fetchAllRsvps, deleteRsvp } from '../api';
import type { RsvpResponse, AttendeeDto } from '../types';
import './RsvpList.css';

interface Props {
  refreshKey: number;
  onEdit: (rsvp: RsvpResponse) => void;
  onDeleted: () => void;
}

interface AttendeeNode {
  attendee: AttendeeDto;
  children: AttendeeNode[];
}

function buildAttendeeTree(attendees: AttendeeDto[]): { roots: AttendeeNode[]; guests: AttendeeDto[] } {
  const familyAttendees = attendees.filter((a) => a.familyMemberId);
  const guests = attendees.filter((a) => !a.familyMemberId);

  const nameToNode = new Map<string, AttendeeNode>();
  for (const a of familyAttendees) {
    nameToNode.set(a.familyMemberName || '', { attendee: a, children: [] });
  }

  const roots: AttendeeNode[] = [];
  for (const a of familyAttendees) {
    const node = nameToNode.get(a.familyMemberName || '')!;
    const parentNode = a.familyMemberParentName ? nameToNode.get(a.familyMemberParentName) : undefined;
    if (parentNode) {
      parentNode.children.push(node);
    } else {
      roots.push(node);
    }
  }

  return { roots, guests };
}

function AttendeeItem({ node }: { node: AttendeeNode }) {
  const a = node.attendee;
  return (
    <li>
      <span className="attendee-name">{a.familyMemberName}</span>{' '}
      <span className="age-badge">{a.familyMemberAgeGroup || 'ADULT'}</span>
      {a.dietaryNeeds && <span className="dietary"> — {a.dietaryNeeds}</span>}
      {node.children.length > 0 && (
        <ul>
          {node.children.map((child) => (
            <AttendeeItem key={child.attendee.id} node={child} />
          ))}
        </ul>
      )}
    </li>
  );
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
              <strong>Attending ({rsvp.attendees.length}):</strong>
              {(() => {
                const { roots, guests } = buildAttendeeTree(rsvp.attendees);
                return (
                  <>
                    <ul className="attendee-tree">
                      {roots.map((node) => (
                        <AttendeeItem key={node.attendee.id} node={node} />
                      ))}
                    </ul>
                    {guests.length > 0 && (
                      <>
                        <strong className="guests-label">Guests:</strong>
                        <ul>
                          {guests.map((g, i) => (
                            <li key={`guest-${i}`}>
                              <span className="attendee-name">{g.guestName}</span>{' '}
                              <span className="age-badge">{g.guestAgeGroup || 'ADULT'}</span>
                              <span className="guest-badge">Guest</span>
                              {g.dietaryNeeds && <span className="dietary"> — {g.dietaryNeeds}</span>}
                            </li>
                          ))}
                        </ul>
                      </>
                    )}
                  </>
                );
              })()}
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
