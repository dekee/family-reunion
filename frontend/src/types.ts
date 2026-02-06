export type AgeGroup = 'ADULT' | 'CHILD' | 'INFANT';

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
