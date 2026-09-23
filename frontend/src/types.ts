export type AgeGroup = 'ADULT' | 'CHILD' | 'INFANT' | 'SPOUSE';

/** Backend TshirtSize enum names. Unisex S–4XL, Youth S–XL, onesies Newborn–12 mths. */
export type TshirtSize =
  | 'S' | 'M' | 'L' | 'XL' | 'XXL' | 'XXXL' | 'XXXXL'
  | 'YS' | 'YM' | 'YL' | 'YXL'
  | 'NEWBORN' | 'M0_3' | 'M3_6' | 'M6_9' | 'M9_12';

export interface AttendeeDto {
  id?: number;
  familyMemberId?: number;
  familyMemberName?: string;
  familyMemberAgeGroup?: AgeGroup;
  familyMemberParentName?: string;
  guestName?: string;
  guestAgeGroup?: AgeGroup;
  dietaryNeeds?: string;
}

export interface RsvpRequest {
  familyName: string;
  headOfHouseholdName: string;
  email: string;
  phone?: string;
  attendees: AttendeeDto[];
  needsLodging: boolean;
  arrivalDate?: string;
  departureDate?: string;
  notes?: string;
}

export interface RsvpResponse {
  id: number;
  familyName: string;
  headOfHouseholdName: string;
  email: string;
  phone?: string;
  attendees: AttendeeDto[];
  needsLodging: boolean;
  arrivalDate?: string;
  departureDate?: string;
  notes?: string;
}

export interface RsvpSummaryResponse {
  totalFamilies: number;
  totalHeadcount: number;
  adultCount: number;
  childCount: number;
  infantCount: number;
  lodgingCount: number;
}

export interface FamilyTreeNode {
  id: number;
  name: string;
  generation: number | null;
  ageGroup: AgeGroup;
  parentId?: number;
  children: FamilyTreeNode[];
  excludeFromRsvp?: boolean;
}

export interface FamilyTreeResponse {
  roots: FamilyTreeNode[];
  totalMembers: number;
}

export interface FlatFamilyMember {
  id: number;
  name: string;
  ageGroup: AgeGroup;
  parentName?: string;
  excludeFromRsvp?: boolean;
}

export interface FamilyMemberRequest {
  name: string;
  ageGroup: AgeGroup;
  parentId?: number;
  generation?: number;
}

export interface EventRegistrationDto {
  id: number;
  familyMemberId: number;
  familyMemberName: string;
}

export interface EventRequest {
  title: string;
  description?: string;
  eventDateTime: string;
  address: string;
  hostName?: string;
  notes?: string;
}

export interface EventResponse {
  id: number;
  title: string;
  description?: string;
  eventDateTime: string;
  address: string;
  hostName?: string;
  notes?: string;
  registrations: EventRegistrationDto[];
  registrationCount: number;
}

export interface EventRegisterRequest {
  familyMemberIds: number[];
}

export interface VolunteerSignupDto {
  id: number;
  familyMemberId: number;
  familyMemberName: string;
}

export interface VolunteerTaskRequest {
  title: string;
  description?: string;
  eventId: number;
}

export interface VolunteerTaskResponse {
  id: number;
  title: string;
  description?: string;
  eventId: number;
  eventTitle: string;
  eventDateTime: string;
  signups: VolunteerSignupDto[];
  signupCount: number;
}

export interface VolunteerSignupRequest {
  familyMemberIds: number[];
}

export interface PaymentResponse {
  id: number;
  rsvpId: number;
  familyName: string;
  amount: number;
  status: string;
  createdAt: string;
}

export interface PaymentLineItemResponse {
  name: string;
  ageGroup: string;
  amount: number;
  isGuest: boolean;
  lineItemId: number;
  tshirtSize: TshirtSize | null;
}

export interface PaymentDetailResponse {
  id: number;
  rsvpId: number;
  familyName: string;
  amount: number;
  status: string;
  createdAt: string;
  payerName: string | null;
  payerEmail: string | null;
  checkinToken: string | null;
  checkedIn: boolean;
  checkedInAt: string | null;
  /** True for a standalone Angel Fund gift: no RSVP, no attendees. */
  donationOnly: boolean;
  donorName: string | null;
  donorFamilyLabel: string | null;
  donorAnonymous: boolean;
  lineItems: PaymentLineItemResponse[];
}

export interface TicketAttendee {
  name: string;
  ageGroup: string;
  isGuest: boolean;
  lineItemId: number;
  tshirtSize: TshirtSize | null;
}

export interface TicketSizeEntry {
  lineItemId: number;
  tshirtSize: TshirtSize;
}

export interface UpdateTicketSizesRequest {
  sizes: TicketSizeEntry[];
}

export interface TicketResponse {
  checkinToken: string;
  familyName: string;
  payerName: string;
  amount: number;
  checkedIn: boolean;
  checkedInAt?: string;
  attendees: TicketAttendee[];
}

export interface CheckinResponse {
  success: boolean;
  message: string;
  ticket?: TicketResponse;
}

export interface SendTicketRequest {
  checkinToken: string;
  email?: string;
  phone?: string;
}

export interface PaidGuestInfo {
  name: string;
  ageGroup: string;
  amount: number;
  lineItemId: number;
  tshirtSize: TshirtSize | null;
}

export interface PaidMemberInfo {
  memberId: number;
  lineItemId: number;
  tshirtSize: TshirtSize | null;
}

export interface PaymentSummaryResponse {
  rsvpId: number;
  familyName: string;
  totalOwed: number;
  totalPaid: number;
  balance: number;
  status: string;
  payments: PaymentResponse[];
  paidMemberIds: number[];
  paidGuests: PaidGuestInfo[];
  paidMembers: PaidMemberInfo[];
}

export interface CheckoutGuestInfo {
  name: string;
  ageGroup: string;
  fee: number;
  tshirtSize: TshirtSize;
}

export interface CheckoutRequest {
  rsvpId: number;
  amount: number;
  memberIds: number[];
  /** familyMemberId -> size; required for every id in memberIds */
  memberSizes: Record<number, TshirtSize>;
  guests: CheckoutGuestInfo[];
  angelAmount?: number;
}

export interface UpdateLineItemSizeRequest {
  rsvpId: number;
  tshirtSize: TshirtSize;
}

export interface LineItemSizeResponse {
  lineItemId: number;
  tshirtSize: TshirtSize;
}

/** A standalone Angel Fund gift: no RSVP, no family member, no fees. */
export interface DonationCheckoutRequest {
  amountCents: number;
  donorName?: string;
  familyLabel?: string;
  anonymous: boolean;
}

export interface AngelContributor {
  /** Donor's chosen name, or "Anonymous". */
  payerName: string;
  /** Family label, or '' for a standalone gift with no branch — render nothing when blank. */
  familyName: string;
  amount: number;
  date: string;
}

export interface MeetingRequest {
  title: string;
  meetingDateTime: string;
  zoomLink: string;
  phoneNumber?: string;
  meetingId?: string;
  passcode?: string;
  notes?: string;
}

export interface MeetingResponse {
  id: number;
  title: string;
  meetingDateTime: string;
  zoomLink: string;
  phoneNumber?: string;
  meetingId?: string;
  passcode?: string;
  notes?: string;
}

export interface AdminUserResponse {
  id: number;
  email: string;
  name: string;
  createdAt: string;
}

export interface GalleryPhoto {
  id: string;
  name: string;
  thumbnailUrl: string;
  fullUrl: string;
  width: number | null;
  height: number | null;
  createdTime: string | null;
  dateTaken: string | null;
}

export interface GalleryResponse {
  photos: GalleryPhoto[];
  nextPageToken: string | null;
  totalCount: number;
}

export interface GalleryUploadResponse {
  uploaded: number;
  photos: GalleryPhoto[];
}

export interface SloganResponse {
  id: number;
  slogan: string;
  voteCount: number;
}

export interface SloganVoteRequest {
  sloganId: number;
  familyMemberId: number;
}

export interface DesignResponse {
  id: number;
  name: string;
  imageUrl: string;
  voteCount: number;
}

export interface DesignVoteRequest {
  designId: number;
  familyMemberId: number;
}

export interface TributeResponse {
  id: number;
  siblingId: number;
  siblingName: string;
  authorId: number;
  authorName: string;
  story: string;
  createdAt: string;
  updatedAt: string;
}

export interface TributeRequest {
  siblingId: number;
  authorId: number;
  story: string;
}
