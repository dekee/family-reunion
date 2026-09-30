import { useEffect, useState } from 'react';
import { useSearchParams, useParams, useNavigate, Link } from 'react-router-dom';
import { fetchFamilyTree, fetchPaymentSummaries, fetchFees, createContributionCheckout } from '../api';
import { getBranchColor } from '../branchColors';
import { ageLabel, setFees, shirtPrice } from '../constants/ageGroups';
import { ANGEL_MAX_DOLLARS } from '../constants/angelFund';
import { ANGEL_LINE_ITEM_NAME, DONATION_LINE_ITEM_NAME, sizeLabel } from '../constants/tshirtSizes';
import { dollars } from '../utils/formatting';
import { flattenBranch, markPaidMembers, branchSlug } from '../utils/branches';
import type { FlatMember, BranchData } from '../utils/branches';
import type { TshirtSize, ContributionAttendee } from '../types';
import { SkeletonCard } from './Skeleton';
import SizeSelect from './SizeSelect';
import './Donations.css';

type GuestAgeGroup = 'ADULT' | 'CHILD' | 'INFANT';

/** A guest being added now. Not on the family tree, so identified by name rather than an id. */
interface DonationGuest {
  tempId: number;
  name: string;
  ageGroup: GuestAgeGroup;
  wantsShirt: boolean;
  tshirtSize?: TshirtSize;
}

/**
 * Pay-what-you-can page. Deliberately not a mode of PayAndRsvp: this page's whole shape is different
 * — already-paid people are absent rather than badged, there is one freeform amount instead of a
 * computed total, and shirts are an opt-in extra rather than included in every fee.
 */
export default function Donations() {
  const [branches, setBranches] = useState<BranchData[]>([]);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState('');
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const [selected, setSelected] = useState<Set<number>>(new Set());
  const [shirtFor, setShirtFor] = useState<Set<number>>(new Set());
  const [sizes, setSizes] = useState<Record<number, TshirtSize>>({});
  const [donation, setDonation] = useState('');

  const [guests, setGuests] = useState<DonationGuest[]>([]);
  const [nextGuestId, setNextGuestId] = useState(1);
  const [showGuestForm, setShowGuestForm] = useState(false);
  const [guestName, setGuestName] = useState('');
  const [guestAgeGroup, setGuestAgeGroup] = useState<GuestAgeGroup>('ADULT');
  const [guestWantsShirt, setGuestWantsShirt] = useState(true);
  const [guestSize, setGuestSize] = useState<TshirtSize | ''>('');

  const [searchParams] = useSearchParams();
  const { branch: branchParam } = useParams<{ branch?: string }>();
  const navigate = useNavigate();

  const paymentStatus = searchParams.get('payment');
  const token = searchParams.get('token');

  const loadData = () => {
    setLoading(true);
    setLoadError('');
    Promise.all([fetchFamilyTree(), fetchPaymentSummaries(), fetchFees()])
      .then(([tree, payments, fees]) => {
        // Must precede flattenBranch: fees are baked into each member at flatten time.
        setFees(fees);
        const branchList: BranchData[] = [];
        for (const root of tree.roots) {
          for (const child of root.children) {
            const branchKey = child.name.replace(/ - Done$/, '').toLowerCase();
            const payment = payments.find(p => {
              const payName = p.familyName.toLowerCase();
              return branchKey.startsWith(payName) || payName.startsWith(branchKey.split(' ')[0]);
            });
            const marked = markPaidMembers(
              flattenBranch(child, 0),
              payment?.paidMemberIds ?? [],
              payment?.paidMembers ?? []
            );
            // Every branch is listed, including ones that have paid in full — they may still be
            // bringing a guest. Paid people stay visible as context but are never selectable, so
            // nobody can be paid for twice.
            const paidGuests = (payment?.paidGuests ?? []).filter(
              g => g.name !== ANGEL_LINE_ITEM_NAME && g.name !== DONATION_LINE_ITEM_NAME
            );
            branchList.push({ node: child, members: marked, payment, paidGuests });
          }
        }
        setBranches(branchList);
      })
      .catch(err => {
        console.error(err);
        setLoadError('Unable to load family data. Please check your connection and try again.');
      })
      .finally(() => setLoading(false));
  };

  useEffect(() => { loadData(); }, []);

  const expandedBranch = branchParam
    ? branches.find(b => branchSlug(b.node.name) === branchParam)?.node.id ?? null
    : null;
  const activeBranch = branches.find(b => b.node.id === expandedBranch);

  const resetForm = () => {
    setSelected(new Set());
    setShirtFor(new Set());
    setSizes({});
    setDonation('');
    setGuests([]);
    setShowGuestForm(false);
    setGuestName('');
    setGuestAgeGroup('ADULT');
    setGuestWantsShirt(true);
    setGuestSize('');
    setError('');
  };

  const handleExpandBranch = (branchId: number) => {
    const branch = branches.find(b => b.node.id === branchId);
    if (!branch) return;
    navigate(expandedBranch === branchId ? '/donations' : `/donations/${branchSlug(branch.node.name)}`);
    resetForm();
  };

  // Each setter gets its own pure updater. Deselecting has to clear the shirt and the size too, or
  // a hidden shirt would keep inflating the total.
  const toggleMember = (id: number) => {
    const wasSelected = selected.has(id);
    setSelected(prev => {
      const next = new Set(prev);
      if (wasSelected) next.delete(id);
      else next.add(id);
      return next;
    });
    if (wasSelected) clearShirt(id);
  };

  const toggleShirt = (id: number) => {
    if (shirtFor.has(id)) {
      clearShirt(id);
      return;
    }
    setShirtFor(prev => new Set(prev).add(id));
  };

  const clearShirt = (id: number) => {
    setShirtFor(prev => { const next = new Set(prev); next.delete(id); return next; });
    setSizes(prev => { const { [id]: _removed, ...rest } = prev; return rest; });
  };

  const guestFormValid = guestName.trim().length > 0 && (!guestWantsShirt || guestSize !== '');

  const handleAddGuest = () => {
    if (!guestFormValid) return;
    setGuests(prev => [...prev, {
      tempId: nextGuestId,
      name: guestName.trim(),
      ageGroup: guestAgeGroup,
      wantsShirt: guestWantsShirt,
      tshirtSize: guestWantsShirt ? (guestSize as TshirtSize) : undefined,
    }]);
    setNextGuestId(id => id + 1);
    setGuestName('');
    setGuestAgeGroup('ADULT');
    setGuestWantsShirt(true);
    setGuestSize('');
    setShowGuestForm(false);
  };

  const removeGuest = (tempId: number) => {
    setGuests(prev => prev.filter(g => g.tempId !== tempId));
  };

  // Each age group has its own size set, so a group change invalidates the chosen size.
  const handleGuestAgeGroupChange = (ageGroup: GuestAgeGroup) => {
    setGuestAgeGroup(ageGroup);
    setGuestSize('');
  };

  const selectedMembers: FlatMember[] = activeBranch
    ? activeBranch.members.filter(m => selected.has(m.id))
    : [];
  const shirtMembers = selectedMembers.filter(m => shirtFor.has(m.id));

  // Derived rather than effect-driven, following AngelGiveForm.
  const shirtCost = shirtPrice();
  const donationDollars = parseFloat(donation) || 0;
  const guestShirts = guests.filter(g => g.wantsShirt);
  // A guest's shirt costs the same $15 as a member's — this page never charges an age-group fee.
  const shirtCount = shirtMembers.length + guestShirts.length;
  const shirtTotal = shirtCount * shirtCost;
  const total = donationDollars + shirtTotal;
  const peopleCount = selectedMembers.length + guests.length;

  const missingSizes = shirtMembers.filter(m => !sizes[m.id]);
  const donationTooBig = donationDollars > ANGEL_MAX_DOLLARS;
  const canSubmit =
    peopleCount > 0 &&
    total >= 1 &&
    missingSizes.length === 0 &&
    !donationTooBig &&
    !submitting;

  const handleSubmit = async () => {
    if (!activeBranch?.payment) {
      setError('No record found for this branch. Please contact an admin.');
      return;
    }
    // Re-gated here rather than trusting the disabled button, and kept in the donor's language: the
    // server's messages are accurate but terse.
    if (peopleCount === 0) {
      setError('Choose at least one person this gift is for, or add a guest.');
      return;
    }
    if (missingSizes.length > 0) {
      setError(`Choose a T-shirt size for ${missingSizes.map(m => m.name).join(', ')}.`);
      return;
    }
    if (total < 1) {
      setError('Enter an amount of at least $1, or add a T-shirt.');
      return;
    }
    if (donationTooBig) {
      setError(`Online gifts are capped at ${dollars(ANGEL_MAX_DOLLARS)} — please contact an admin for a larger gift.`);
      return;
    }

    setSubmitting(true);
    setError('');
    try {
      const { url } = await createContributionCheckout({
        rsvpId: activeBranch.payment.rsvpId,
        amount: Math.round(total * 100),
        donationCents: Math.round(donationDollars * 100),
        attendees: [
          ...selectedMembers.map((m): ContributionAttendee => ({
            memberId: m.id,
            wantsShirt: shirtFor.has(m.id),
            tshirtSize: shirtFor.has(m.id) ? sizes[m.id] : undefined,
          })),
          ...guests.map((g): ContributionAttendee => ({
            guestName: g.name,
            ageGroup: g.ageGroup,
            wantsShirt: g.wantsShirt,
            tshirtSize: g.wantsShirt ? g.tshirtSize : undefined,
          })),
        ],
      });
      window.location.href = url;
    } catch (err: any) {
      setError(err.message || 'Could not start checkout. Please try again.');
      // Only reset on failure. On success it stays true through the redirect so a second click
      // cannot open a second Stripe session.
      setSubmitting(false);
    }
  };

  if (loading) return (
    <div className="don-page">
      <div className="don-branch-grid">
        {Array.from({ length: 6 }).map((_, i) => <SkeletonCard key={i} />)}
      </div>
    </div>
  );

  if (loadError) return (
    <div className="don-page">
      <div className="don-load-error">
        <p>{loadError}</p>
        <button onClick={loadData} className="don-retry-btn">Try Again</button>
      </div>
    </div>
  );

  return (
    <div className="don-page">
      {paymentStatus === 'success' && (
        <div className="don-banner don-banner-success">
          <strong>Thank you.</strong> Your gift is on its way — it may take a moment to appear here.
          {token && <> <Link to={`/ticket/${token}`} className="don-ticket-link">View your ticket &rarr;</Link></>}
        </div>
      )}
      {paymentStatus === 'cancelled' && (
        <div className="don-banner don-banner-cancelled">Checkout was cancelled — nothing was charged.</div>
      )}

      {!activeBranch ? (
        <>
          <h2 className="don-title">Give What You Can</h2>
          <p className="don-subtitle">
            Every family should be at this reunion, whatever their year has looked like. Pick the
            people you want to cover, add any guests you are bringing, and give whatever you are
            able. T-shirts are {dollars(shirtCost)} each. Anyone you choose is counted as attending.
          </p>

          {branches.length === 0 ? (
            <div className="don-all-paid">
              <h3>Everyone is covered.</h3>
              <p>
                Every family member has been paid for. If you would still like to help, the{' '}
                <Link to="/thank-you">Angel Fund</Link> supports the reunion itself.
              </p>
            </div>
          ) : (
            <div className="don-branch-grid">
              {branches.map(b => {
                const branchName = b.node.name.replace(/ - Done$/, '');
                const branchColor = getBranchColor(branchName);
                const unpaid = b.members.filter(m => !m.paid);
                const stillOwed = unpaid.reduce((sum, m) => sum + m.fee, 0);
                const fullyPaid = unpaid.length === 0;
                return (
                  <button
                    key={b.node.id}
                    className={`don-branch-card ${fullyPaid ? 'don-branch-card-paid' : ''}`}
                    onClick={() => handleExpandBranch(b.node.id)}
                    style={{ borderTopColor: branchColor }}
                  >
                    {fullyPaid && <span className="don-branch-paid-badge">All Covered</span>}
                    <span className="don-branch-name">{branchName}</span>
                    <span className="don-branch-count">
                      {fullyPaid
                        ? `All ${b.members.length} covered`
                        : `${unpaid.length} ${unpaid.length === 1 ? 'person' : 'people'} still need help`}
                    </span>
                    <span className="don-branch-owed">
                      {fullyPaid ? 'Add a guest or give extra' : `${dollars(stillOwed)} in unpaid fees`}
                    </span>
                  </button>
                );
              })}
            </div>
          )}
        </>
      ) : (
        <div className="don-detail">
          <button className="don-back-btn" onClick={() => { navigate('/donations'); resetForm(); }}>
            &larr; Back to all branches
          </button>

          <div
            className="don-detail-header"
            style={{ borderLeftColor: getBranchColor(activeBranch.node.name.replace(/ - Done$/, '')) }}
          >
            <h2>{activeBranch.node.name.replace(/ - Done$/, '')} Family</h2>
            <p>
              {activeBranch.members.filter(m => !m.paid).length === 0
                ? 'Everyone here is covered — you can still add a guest or give extra'
                : `${activeBranch.members.filter(m => !m.paid).length} of ${activeBranch.members.length} still to be covered`}
            </p>
          </div>

          <div className="don-amount-section">
            <label className="don-amount-label" htmlFor="don-amount">Your gift</label>
            <div className="don-amount-row">
              <span className="don-dollar-sign">$</span>
              <input
                id="don-amount"
                type="number"
                min="0"
                step="1"
                value={donation}
                placeholder="0"
                className="don-amount-input"
                onChange={e => setDonation(e.target.value)}
                onKeyDown={e => { if (e.key === 'Escape') setDonation(''); }}
              />
            </div>
            <p className="don-amount-hint">
              Give whatever you are able — there is no minimum beyond $1. T-shirts are {dollars(shirtCost)} each on top.
            </p>
          </div>

          <div className="don-members-list">
            <div className="don-members-heading">Who is this for?</div>
            {activeBranch.members.map(m => {
              const isSelected = selected.has(m.id);
              const wantsShirt = shirtFor.has(m.id);
              // Already covered: shown so the donor can see who is taken care of, but never an
              // option — paying for the same person twice is the thing this page must not allow.
              if (m.paid) {
                return (
                  <div
                    key={m.id}
                    className="don-member-row paid"
                    style={{ paddingLeft: `${1 + m.depth * 1.5}rem` }}
                  >
                    <span className="don-member-main">
                      <span className="don-member-check">&#10003;</span>
                      <span className="don-member-name">{m.name}</span>
                      <span className={`don-member-age age-${m.ageGroup.toLowerCase()}`}>{ageLabel(m.ageGroup)}</span>
                    </span>
                    <span className="don-member-paid-badge">Covered</span>
                  </div>
                );
              }
              return (
                <div
                  key={m.id}
                  className={`don-member-row ${isSelected ? 'selected' : ''}`}
                  style={{ paddingLeft: `${1 + m.depth * 1.5}rem` }}
                >
                  <label className="don-member-main">
                    <input type="checkbox" checked={isSelected} onChange={() => toggleMember(m.id)} />
                    <span className="don-member-name">{m.name}</span>
                    <span className={`don-member-age age-${m.ageGroup.toLowerCase()}`}>{ageLabel(m.ageGroup)}</span>
                  </label>

                  {isSelected && (
                    <div className="don-member-shirt">
                      <label className="don-shirt-toggle">
                        <input type="checkbox" checked={wantsShirt} onChange={() => toggleShirt(m.id)} />
                        <span>T-shirt +{dollars(shirtCost)}</span>
                      </label>
                      {wantsShirt && (
                        <SizeSelect
                          ageGroup={m.ageGroup}
                          value={sizes[m.id] ?? ''}
                          onChange={size => setSizes(prev => ({ ...prev, [m.id]: size }))}
                          ariaLabel={`T-shirt size for ${m.name}`}
                        />
                      )}
                    </div>
                  )}
                </div>
              );
            })}

            {activeBranch.paidGuests.length > 0 && (
              <>
                <div className="don-guests-divider">Guests already covered</div>
                {activeBranch.paidGuests.map(g => (
                  <div key={`paid-guest-${g.lineItemId}`} className="don-member-row paid">
                    <span className="don-member-main">
                      <span className="don-member-check">&#10003;</span>
                      <span className="don-member-name">{g.name}</span>
                      <span className={`don-member-age age-${g.ageGroup.toLowerCase()}`}>{ageLabel(g.ageGroup)}</span>
                    </span>
                    <span className="don-member-paid-badge">Covered</span>
                  </div>
                ))}
              </>
            )}

            {guests.length > 0 && (
              <>
                <div className="don-guests-divider">Guests you are bringing</div>
                {guests.map(g => (
                  <div key={g.tempId} className="don-member-row selected">
                    <span className="don-member-main">
                      <span className="don-guest-icon">+</span>
                      <span className="don-member-name">{g.name}</span>
                      <span className={`don-member-age age-${g.ageGroup.toLowerCase()}`}>{ageLabel(g.ageGroup)}</span>
                    </span>
                    <span className="don-member-shirt">
                      <span className="don-guest-shirt-note">
                        {g.wantsShirt ? `T-shirt ${g.tshirtSize ? sizeLabel(g.tshirtSize) : ''} +${dollars(shirtCost)}` : 'No T-shirt'}
                      </span>
                      <button
                        className="don-guest-remove"
                        onClick={() => removeGuest(g.tempId)}
                        title={`Remove ${g.name}`}
                      >
                        &times;
                      </button>
                    </span>
                  </div>
                ))}
              </>
            )}
          </div>

          <div className="don-add-guest-section">
            {showGuestForm ? (
              <div className="don-guest-form">
                <input
                  type="text"
                  placeholder="Guest name"
                  value={guestName}
                  onChange={e => setGuestName(e.target.value)}
                  className="don-guest-name-input"
                  autoFocus
                  onKeyDown={e => {
                    if (e.key === 'Enter') handleAddGuest();
                    if (e.key === 'Escape') { setShowGuestForm(false); setGuestName(''); setGuestSize(''); }
                  }}
                />
                {/* Plain age labels, not ageLabelWithFee: a guest's shirt is $15 like everyone
                    else's here, so showing the age-group fee would misstate the price. */}
                <select
                  value={guestAgeGroup}
                  onChange={e => handleGuestAgeGroupChange(e.target.value as GuestAgeGroup)}
                  className="don-guest-age-select"
                >
                  <option value="ADULT">{ageLabel('ADULT')}</option>
                  <option value="CHILD">{ageLabel('CHILD')}</option>
                  <option value="INFANT">{ageLabel('INFANT')}</option>
                </select>
                <label className="don-shirt-toggle">
                  <input
                    type="checkbox"
                    checked={guestWantsShirt}
                    onChange={() => { setGuestWantsShirt(v => !v); setGuestSize(''); }}
                  />
                  <span>T-shirt +{dollars(shirtCost)}</span>
                </label>
                {guestWantsShirt && (
                  <SizeSelect
                    ageGroup={guestAgeGroup}
                    value={guestSize}
                    onChange={setGuestSize}
                    ariaLabel="Guest T-shirt size"
                  />
                )}
                <button className="don-guest-add-btn" onClick={handleAddGuest} disabled={!guestFormValid}>
                  Add
                </button>
                <button
                  className="don-guest-cancel-btn"
                  onClick={() => { setShowGuestForm(false); setGuestName(''); setGuestSize(''); }}
                >
                  Cancel
                </button>
              </div>
            ) : (
              <button className="don-add-guest-btn" onClick={() => setShowGuestForm(true)}>
                + Add a guest
              </button>
            )}
          </div>

          <div className="don-summary">
            <div className="don-summary-row">
              <span>Gift</span>
              <span>{dollars(donationDollars)}</span>
            </div>
            <div className="don-summary-row">
              <span>
                {shirtCount} {shirtCount === 1 ? 'T-shirt' : 'T-shirts'}
              </span>
              <span>{dollars(shirtTotal)}</span>
            </div>
            <div className="don-summary-row don-summary-total">
              <span>Total</span>
              <span>{dollars(total)}</span>
            </div>
            {peopleCount > 0 && (
              <p className="don-summary-note">
                {peopleCount} {peopleCount === 1 ? 'person' : 'people'} will be counted as attending
                and will get a check-in ticket.
              </p>
            )}
          </div>

          {error && <p className="don-error">{error}</p>}

          <button className="don-submit-btn" disabled={!canSubmit} onClick={handleSubmit}>
            {submitting ? 'Redirecting to checkout…' : total > 0 ? `Give ${dollars(total)}` : 'Give'}
          </button>
          {missingSizes.length > 0 && (
            <p className="don-hint">
              Choose a T-shirt size for {missingSizes.map(m => m.name).join(', ')}.
            </p>
          )}
        </div>
      )}
    </div>
  );
}
