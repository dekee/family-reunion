import { useEffect, useState } from 'react';
import { useSearchParams, useParams, useNavigate, Link } from 'react-router-dom';
import { fetchFamilyTree, fetchPaymentSummaries, createCheckoutSession, fetchFees } from '../api';
import { getBranchColor } from '../branchColors';
import { feeForAge, ageLabel, ageLabelWithFee, setFees } from '../constants/ageGroups';
import { dollars } from '../utils/formatting';
import type { FamilyTreeNode, PaymentSummaryResponse, PaidGuestInfo } from '../types';
import { SkeletonCard } from './Skeleton';
import './PayAndRsvp.css';

type GuestAgeGroup = 'ADULT' | 'CHILD' | 'INFANT';

interface FlatMember {
  id: number;
  name: string;
  ageGroup: string;
  fee: number;
  depth: number;
  paid?: boolean;
}

interface Guest {
  tempId: number;
  name: string;
  ageGroup: GuestAgeGroup;
  fee: number;
}

function flattenBranch(node: FamilyTreeNode, depth: number): FlatMember[] {
  const result: FlatMember[] = [{
    id: node.id,
    name: node.name,
    ageGroup: node.ageGroup,
    fee: feeForAge(node.ageGroup),
    depth,
  }];
  for (const child of node.children) {
    result.push(...flattenBranch(child, depth + 1));
  }
  return result;
}

function markPaidMembers(members: FlatMember[], paidMemberIds: number[]): FlatMember[] {
  const paidSet = new Set(paidMemberIds);
  return members.map(m => ({ ...m, paid: paidSet.has(m.id) }));
}

interface BranchData {
  node: FamilyTreeNode;
  members: FlatMember[];
  payment?: PaymentSummaryResponse;
  paidGuests: PaidGuestInfo[];
}

function branchSlug(name: string): string {
  return name.replace(/ - Done$/, '').toLowerCase().replace(/[^a-z0-9]+/g, '-').replace(/-+$/, '');
}

export default function PayAndRsvp() {
  const [branches, setBranches] = useState<BranchData[]>([]);
  const [loading, setLoading] = useState(true);
  const [selected, setSelected] = useState<Set<number>>(new Set());
  const [checkingOut, setCheckingOut] = useState(false);
  const [error, setError] = useState('');
  const [loadError, setLoadError] = useState('');
  const [searchParams] = useSearchParams();
  const { branch: branchParam } = useParams<{ branch?: string }>();
  const navigate = useNavigate();

  // Guest state
  const [guests, setGuests] = useState<Guest[]>([]);
  const [nextGuestId, setNextGuestId] = useState(1);
  const [showGuestForm, setShowGuestForm] = useState(false);
  const [guestName, setGuestName] = useState('');
  const [guestAgeGroup, setGuestAgeGroup] = useState<GuestAgeGroup>('ADULT');

  // Angel contributor state
  const [angelAmount, setAngelAmount] = useState('');
  const [showAngelForm, setShowAngelForm] = useState(false);

  const paymentStatus = searchParams.get('payment');
  const returnRsvpId = searchParams.get('rsvpId');

  const loadData = () => {
    setLoading(true);
    setLoadError('');
    Promise.all([fetchFamilyTree(), fetchPaymentSummaries(), fetchFees()])
      .then(([tree, payments, fees]) => {
        setFees(fees);
        const branchList: BranchData[] = [];
        for (const root of tree.roots) {
          for (const child of root.children) {
            const members = flattenBranch(child, 0);
            const branchKey = child.name.replace(/ - Done$/, '').toLowerCase();
            const payment = payments.find(p => {
              const payName = p.familyName.toLowerCase();
              return branchKey.startsWith(payName) || payName.startsWith(branchKey.split(' ')[0]);
            });
            const paidMemberIds = payment?.paidMemberIds ?? [];
            const markedMembers = paidMemberIds.length > 0 ? markPaidMembers(members, paidMemberIds) : members;
            const paidGuests = payment?.paidGuests ?? [];
            branchList.push({ node: child, members: markedMembers, payment, paidGuests });
          }
        }
        setBranches(branchList);

        // Auto-navigate to the branch the user just paid for
        if (returnRsvpId && !branchParam) {
          const rsvpIdNum = Number(returnRsvpId);
          const match = branchList.find(b => b.payment?.rsvpId === rsvpIdNum);
          if (match) {
            const slug = branchSlug(match.node.name);
            navigate(`/pay/${slug}?${searchParams.toString()}`, { replace: true });
          }
        }
      })
      .catch((err) => {
        console.error(err);
        setLoadError('Unable to load payment data. Please check your connection and try again.');
      })
      .finally(() => setLoading(false));
  };

  useEffect(() => { loadData(); }, []);

  // Resolve the active branch from the URL param
  const expandedBranch = branchParam
    ? branches.find(b => branchSlug(b.node.name) === branchParam)?.node.id ?? null
    : null;

  const handleExpandBranch = (branchId: number) => {
    const branch = branches.find(b => b.node.id === branchId);
    if (!branch) return;
    if (expandedBranch === branchId) {
      navigate('/pay');
    } else {
      navigate(`/pay/${branchSlug(branch.node.name)}`);
    }
    setSelected(new Set());
    setGuests([]);
    setShowGuestForm(false);
    setAngelAmount('');
    setShowAngelForm(false);
    setError('');
  };

  const toggleMember = (id: number) => {
    setSelected(prev => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
  };

  const selectAll = (members: FlatMember[]) => {
    setSelected(new Set(members.filter(m => !m.paid).map(m => m.id)));
  };

  const deselectAll = () => {
    setSelected(new Set());
  };

  const handleAddGuest = () => {
    const name = guestName.trim();
    if (!name) return;
    const guest: Guest = {
      tempId: nextGuestId,
      name,
      ageGroup: guestAgeGroup,
      fee: feeForAge(guestAgeGroup),
    };
    setGuests(prev => [...prev, guest]);
    setNextGuestId(id => id + 1);
    setGuestName('');
    setGuestAgeGroup('ADULT');
    setShowGuestForm(false);
  };

  const removeGuest = (tempId: number) => {
    setGuests(prev => prev.filter(g => g.tempId !== tempId));
  };

  const activeBranch = branches.find(b => b.node.id === expandedBranch);
  const selectedMembers = activeBranch
    ? activeBranch.members.filter(m => selected.has(m.id))
    : [];
  const memberTotal = selectedMembers.reduce((sum, m) => sum + m.fee, 0);
  const guestTotal = guests.reduce((sum, g) => sum + g.fee, 0);
  const angelDollars = parseFloat(angelAmount) || 0;
  const angelCents = Math.round(angelDollars * 100);
  const selectedTotal = memberTotal + guestTotal + angelDollars;

  // Combined counts for summary
  const adultCount = selectedMembers.filter(m => m.ageGroup === 'ADULT' || m.ageGroup === 'SPOUSE').length
    + guests.filter(g => g.ageGroup === 'ADULT').length;
  const childCount = selectedMembers.filter(m => m.ageGroup === 'CHILD').length
    + guests.filter(g => g.ageGroup === 'CHILD').length;
  const infantCount = selectedMembers.filter(m => m.ageGroup === 'INFANT').length
    + guests.filter(g => g.ageGroup === 'INFANT').length;

  const handleCheckout = async () => {
    if (!activeBranch?.payment) {
      setError('No payment record found for this branch. Contact admin.');
      return;
    }
    if (selectedTotal <= 0) {
      setError('Please select at least one paid member or add a guest.');
      return;
    }
    setCheckingOut(true);
    setError('');
    try {
      const { url } = await createCheckoutSession({
        rsvpId: activeBranch.payment.rsvpId,
        amount: Math.round(selectedTotal * 100),
        memberIds: selectedMembers.map(m => m.id),
        guests: guests.map(g => ({ name: g.name, ageGroup: g.ageGroup, fee: g.fee * 100 })),
        angelAmount: angelCents > 0 ? angelCents : undefined,
      });
      window.location.href = url;
    } catch (err: any) {
      setError(err.message || 'Failed to start checkout');
    } finally {
      setCheckingOut(false);
    }
  };

  if (loading) return (
    <div className="pay-rsvp-page">
      <div className="page-header"><h2>Pay & RSVP</h2><p>Select your family and pay for attending members</p></div>
      <div className="pay-branch-grid">
        {Array.from({ length: 6 }).map((_, i) => <SkeletonCard key={i} lines={2} />)}
      </div>
    </div>
  );

  if (loadError) return (
    <div className="pay-rsvp-page">
      <div className="page-header"><h2>Pay & RSVP</h2><p>Select your family and pay for attending members</p></div>
      <div className="pay-load-error">
        <p>{loadError}</p>
        <button onClick={loadData} className="pay-retry-btn">Try Again</button>
      </div>
    </div>
  );

  return (
    <div className="pay-rsvp-page">
      <div className="page-header">
        <h2>Pay & RSVP</h2>
        <p>Select your family branch, choose attending members, and pay. Selecting a member confirms their attendance.</p>
      </div>

      {paymentStatus === 'success' && (
        <div className="payment-banner payment-success">
          <span>Payment successful! Thank you.</span>
          {(() => {
            const token = searchParams.get('token');
            if (!token) return null;
            return (
              <Link to={`/ticket/${token}`} className="payment-ticket-link">
                View Your Ticket & QR Code
              </Link>
            );
          })()}
        </div>
      )}
      {paymentStatus === 'cancelled' && (
        <div className="payment-banner payment-cancelled">Payment was cancelled.</div>
      )}

      {!expandedBranch ? (
        <>
          {branches.length > 0 && (() => {
            const overallTotal = branches.reduce((s, b) => s + b.members.reduce((ms, m) => ms + m.fee, 0), 0);
            const overallPaid = branches.reduce((s, b) => s + (b.payment?.totalPaid ?? 0), 0);
            const overallRemaining = Math.max(0, overallTotal - overallPaid);
            const familiesPaid = branches.filter(b => {
              const cost = b.members.reduce((s, m) => s + m.fee, 0);
              return (b.payment?.totalPaid ?? 0) >= cost && (b.payment?.totalPaid ?? 0) > 0;
            }).length;
            return (
              <div className="pay-overview-grid">
                <div className="pay-overview-card">
                  <span className="pay-overview-number pay-overview-green">{dollars(overallPaid)}</span>
                  <span className="pay-overview-label">Total Collected</span>
                </div>
                <div className="pay-overview-card">
                  <span className={`pay-overview-number ${overallRemaining > 0 ? 'pay-overview-amber' : 'pay-overview-green'}`}>{dollars(overallRemaining)}</span>
                  <span className="pay-overview-label">Remaining Balance</span>
                </div>
                <div className="pay-overview-card">
                  <span className="pay-overview-number">{familiesPaid} / {branches.length}</span>
                  <span className="pay-overview-label">Families Paid</span>
                </div>
              </div>
            );
          })()}

          <h3 className="pay-section-title">Select Your Family Branch</h3>
          <div className="pay-branch-grid">
            {branches.map(b => {
              const branchName = b.node.name.replace(/ - Done$/, '');
              const branchColor = getBranchColor(branchName);
              const totalCost = b.members.reduce((sum, m) => sum + m.fee, 0);
              const paid = b.payment?.totalPaid ?? 0;
              const balance = Math.max(0, totalCost - paid);
              const paidPercent = totalCost > 0 ? Math.min(100, Math.round((paid / totalCost) * 100)) : 0;
              const fullyPaid = balance <= 0 && paid > 0;

              return (
                <button
                  key={b.node.id}
                  className={`pay-branch-card ${fullyPaid ? 'pay-branch-card-paid' : ''}`}
                  onClick={() => handleExpandBranch(b.node.id)}
                  style={{ borderTopColor: branchColor }}
                >
                  {fullyPaid && <span className="pay-branch-paid-badge">Fully Paid</span>}
                  <span className="pay-branch-name">{branchName}</span>
                  <span className="pay-branch-members">{b.members.length} members</span>
                  <div className="pay-branch-amounts">
                    <div className="pay-branch-amount-row">
                      <span>Total</span>
                      <span>{dollars(totalCost)}</span>
                    </div>
                    <div className="pay-branch-amount-row pay-branch-amount-paid">
                      <span>Paid</span>
                      <span>{dollars(paid)}</span>
                    </div>
                    <div className={`pay-branch-amount-row ${fullyPaid ? 'pay-branch-amount-paid' : 'pay-branch-amount-due'}`}>
                      <span>Remaining</span>
                      <span>{fullyPaid ? 'Paid in full' : dollars(balance)}</span>
                    </div>
                  </div>
                  <div className="pay-branch-progress">
                    <div
                      className="pay-branch-progress-fill"
                      style={{ width: `${paidPercent}%`, background: fullyPaid ? '#27ae60' : branchColor }}
                    />
                  </div>
                </button>
              );
            })}
          </div>

        </>
      ) : activeBranch && (
        <div className="pay-detail-view">
          <button className="pay-back-btn" onClick={() => { navigate('/pay'); setSelected(new Set()); setGuests([]); setAngelAmount(''); setShowAngelForm(false); }}>
            &larr; Back to all branches
          </button>

          <div className="pay-detail-header" style={{ borderLeftColor: getBranchColor(activeBranch.node.name.replace(/ - Done$/, '')) }}>
            <h3>{activeBranch.node.name.replace(/ - Done$/, '')} Family</h3>
            <p>{activeBranch.members.length} members</p>
            {(() => {
              const detailTotal = activeBranch.members.reduce((sum, m) => sum + m.fee, 0);
              const detailPaid = activeBranch.payment?.totalPaid ?? 0;
              const detailBalance = Math.max(0, detailTotal - detailPaid);
              return (
                <div className="pay-detail-stats">
                  <div className="pay-detail-stat">
                    <span className="pay-detail-stat-label">Total</span>
                    <span className="pay-detail-stat-value">{dollars(detailTotal)}</span>
                  </div>
                  <div className="pay-detail-stat">
                    <span className="pay-detail-stat-label">Paid</span>
                    <span className="pay-detail-stat-value pay-detail-stat-paid">{dollars(detailPaid)}</span>
                  </div>
                  <div className="pay-detail-stat">
                    <span className="pay-detail-stat-label">Remaining</span>
                    <span className={`pay-detail-stat-value ${detailBalance <= 0 ? 'pay-detail-stat-paid' : 'pay-detail-stat-due'}`}>
                      {detailBalance <= 0 ? 'Paid in full' : dollars(detailBalance)}
                    </span>
                  </div>
                </div>
              );
            })()}
          </div>

          <div className="pay-select-controls">
            <button onClick={() => selectAll(activeBranch.members)}>Select All</button>
            <button onClick={deselectAll}>Deselect All</button>
            <span className="pay-selected-count">
              {selectedMembers.length > 0
                ? `${selectedMembers.length} selected`
                : 'Select members to pay for'}
              {guests.length > 0 && ` + ${guests.length} guest${guests.length > 1 ? 's' : ''}`}
              {activeBranch.members.some(m => m.paid) && (
                <span className="pay-paid-count"> · {activeBranch.members.filter(m => m.paid).length} paid</span>
              )}
            </span>
          </div>

          <div className="pay-age-legend">
            <span className="pay-legend-item"><span className="pay-legend-pill age-adult">Adult</span> ages 18+ · ${feeForAge('ADULT')} each</span>
            <span className="pay-legend-item"><span className="pay-legend-pill age-spouse">Spouse</span> ages 18+ · ${feeForAge('SPOUSE')} each</span>
            <span className="pay-legend-item"><span className="pay-legend-pill age-child">Child</span> ages 6 to 17 · ${feeForAge('CHILD')} each</span>
            <span className="pay-legend-item"><span className="pay-legend-pill age-infant">Under 5</span> ages 0 to 5 · ${feeForAge('INFANT')} each (t-shirt)</span>
          </div>

          <div className="pay-members-list">
            {activeBranch.members.every(m => m.paid) && (
              <div className="pay-all-paid-notice">All members are paid — nothing to do here!</div>
            )}
            {activeBranch.members.map(m => (
              <label
                key={m.id}
                className={`pay-member-row ${m.paid ? 'paid' : ''} ${!m.paid && selected.has(m.id) ? 'selected' : ''}`}
                style={{ paddingLeft: `${1 + m.depth * 1.5}rem` }}
              >
                {m.paid ? (
                  <span className="pay-member-check-icon">&#10003;</span>
                ) : (
                  <input
                    type="checkbox"
                    checked={selected.has(m.id)}
                    onChange={() => toggleMember(m.id)}
                  />
                )}
                <span className="pay-member-name">{m.name}</span>
                <span className={`pay-member-age age-${m.ageGroup.toLowerCase()}`}>{ageLabel(m.ageGroup)}</span>
                {m.paid ? (
                  <span className="pay-member-paid-badge">Paid</span>
                ) : (
                  <span className="pay-member-fee">
                    {m.fee > 0 ? dollars(m.fee) : 'Free'}
                  </span>
                )}
              </label>
            ))}

            {(guests.length > 0 || activeBranch.paidGuests.length > 0) && (
              <div className="pay-guests-divider">Guests</div>
            )}
            {activeBranch.paidGuests.map((g, i) => (
              <div key={`paid-guest-${i}`} className="pay-member-row paid pay-guest-row">
                <span className="pay-guest-icon">+</span>
                <span className="pay-member-name">{g.name}</span>
                <span className={`pay-member-age age-${g.ageGroup.toLowerCase()}`}>{ageLabel(g.ageGroup)}</span>
                <span className="pay-member-paid-badge">Paid</span>
              </div>
            ))}
            {guests.map(g => (
              <div key={`guest-${g.tempId}`} className="pay-member-row selected pay-guest-row">
                <span className="pay-guest-icon">+</span>
                <span className="pay-member-name">{g.name}</span>
                <span className={`pay-member-age age-${g.ageGroup.toLowerCase()}`}>{ageLabel(g.ageGroup)}</span>
                <span className="pay-member-fee">
                  {g.fee > 0 ? dollars(g.fee) : 'Free'}
                </span>
                <button
                  className="pay-guest-remove"
                  onClick={() => removeGuest(g.tempId)}
                  title="Remove guest"
                >
                  &times;
                </button>
              </div>
            ))}
          </div>

          {/* Add Guest */}
          <div className="pay-add-guest-section">
            {showGuestForm ? (
              <div className="pay-guest-form">
                <input
                  type="text"
                  placeholder="Guest name"
                  value={guestName}
                  onChange={e => setGuestName(e.target.value)}
                  className="pay-guest-name-input"
                  autoFocus
                  onKeyDown={e => { if (e.key === 'Enter') handleAddGuest(); }}
                />
                <select
                  value={guestAgeGroup}
                  onChange={e => setGuestAgeGroup(e.target.value as GuestAgeGroup)}
                  className="pay-guest-age-select"
                >
                  <option value="ADULT">{ageLabelWithFee('ADULT')}</option>
                  <option value="CHILD">{ageLabelWithFee('CHILD')}</option>
                  <option value="INFANT">{ageLabelWithFee('INFANT')}</option>
                </select>
                <button className="pay-guest-add-btn" onClick={handleAddGuest} disabled={!guestName.trim()}>
                  Add
                </button>
                <button className="pay-guest-cancel-btn" onClick={() => { setShowGuestForm(false); setGuestName(''); }}>
                  Cancel
                </button>
              </div>
            ) : (
              <button className="pay-add-guest-btn" onClick={() => setShowGuestForm(true)}>
                + Add Guest
              </button>
            )}
          </div>

          {/* Angel Contributor */}
          <div className="pay-angel-section">
            {showAngelForm ? (
              <div className="pay-angel-form">
                <div className="pay-angel-description">
                  <span className="pay-angel-title">Angel Contributor</span>
                  <p>Your extra contribution helps enable family members who may not be able to cover their own fees to still participate in the reunion.</p>
                </div>
                <div className="pay-angel-input-row">
                  <span className="pay-angel-dollar-sign">$</span>
                  <input
                    type="number"
                    min="1"
                    step="1"
                    placeholder="0"
                    value={angelAmount}
                    onChange={e => setAngelAmount(e.target.value)}
                    className="pay-angel-amount-input"
                    autoFocus
                    onKeyDown={e => { if (e.key === 'Escape') { setShowAngelForm(false); setAngelAmount(''); } }}
                  />
                  <button className="pay-angel-clear-btn" onClick={() => { setShowAngelForm(false); setAngelAmount(''); }}>
                    Remove
                  </button>
                </div>
              </div>
            ) : (
              <button className="pay-angel-btn" onClick={() => setShowAngelForm(true)}>
                <span className="pay-angel-btn-icon">&#10084;&#65039;</span>
                <span className="pay-angel-btn-text">
                  <span className="pay-angel-btn-label">Become an Angel Contributor</span>
                  <span className="pay-angel-btn-hint">Help a family member who can't cover their fees</span>
                </span>
              </button>
            )}
          </div>

          {(selectedMembers.length > 0 || guests.length > 0 || angelDollars > 0) && (
            <div className="pay-summary-bar">
              <div className="pay-summary-details">
                {adultCount > 0 && (
                  <div className="pay-summary-line">
                    <span>Adults/Spouses:</span>
                    <span>{adultCount} x {dollars(feeForAge('ADULT'))}</span>
                  </div>
                )}
                {childCount > 0 && (
                  <div className="pay-summary-line">
                    <span>Children:</span>
                    <span>{childCount} x {dollars(feeForAge('CHILD'))}</span>
                  </div>
                )}
                {infantCount > 0 && (
                  <div className="pay-summary-line">
                    <span>Under 5:</span>
                    <span>{infantCount} x {dollars(feeForAge('INFANT'))} (t-shirt)</span>
                  </div>
                )}
                {guests.length > 0 && (
                  <div className="pay-summary-line pay-summary-guest-note">
                    <span>Includes {guests.length} guest{guests.length > 1 ? 's' : ''}</span>
                    <span>{dollars(guestTotal)}</span>
                  </div>
                )}
                {angelDollars > 0 && (
                  <div className="pay-summary-line pay-summary-angel-note">
                    <span>Angel Contribution</span>
                    <span>{dollars(angelDollars)}</span>
                  </div>
                )}
              </div>
              <div className="pay-summary-total">
                <span className="pay-total-label">Total</span>
                <span className="pay-total-amount">{dollars(selectedTotal)}</span>
              </div>
              <button
                className="pay-checkout-btn"
                onClick={handleCheckout}
                disabled={selectedTotal <= 0 || checkingOut}
              >
                {checkingOut ? 'Redirecting...' : `Pay ${dollars(selectedTotal)} Now`}
              </button>
              {error && <p className="pay-error">{error}</p>}
            </div>
          )}
        </div>
      )}
    </div>
  );
}
