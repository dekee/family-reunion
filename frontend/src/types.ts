export type AgeGroup = 'ADULT' | 'CHILD' | 'INFANT' | 'SPOUSE';

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

export interface PaymentResponse {
  id: number;
  rsvpId: number;
  familyName: string;
  amount: number;
  status: string;
  createdAt: string;
  checkinToken?: string;
}

export interface TicketAttendee {
  name: string;
  ageGroup: string;
  isGuest: boolean;
}

export interface TicketResponse {
  checkinToken: string;
  familyName: string;
  payerName: string;
  payerEmail?: string;
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
}

export interface CheckoutGuestInfo {
  name: string;
  ageGroup: string;
  fee: number;
}

export interface CheckoutRequest {
  rsvpId: number;
  amount: number;
  memberIds: number[];
  guests: CheckoutGuestInfo[];
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
}

export interface GalleryResponse {
  photos: GalleryPhoto[];
  nextPageToken: string | null;
  totalCount: number;
}
