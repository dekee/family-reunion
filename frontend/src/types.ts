export type AgeGroup = 'ADULT' | 'CHILD' | 'INFANT';

export interface FamilyMemberDto {
  id?: number;
  name: string;
  ageGroup: AgeGroup;
  dietaryNeeds?: string;
}

export interface RsvpRequest {
  familyName: string;
  headOfHouseholdName: string;
  email: string;
  phone?: string;
  familyMembers: FamilyMemberDto[];
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
  familyMembers: FamilyMemberDto[];
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
