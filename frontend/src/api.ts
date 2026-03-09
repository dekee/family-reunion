import type { RsvpRequest, RsvpResponse, RsvpSummaryResponse, FamilyTreeResponse, FamilyTreeNode, FamilyMemberRequest, MeetingRequest, MeetingResponse, EventRequest, EventResponse, EventRegisterRequest, PaymentSummaryResponse, CheckoutRequest, AdminUserResponse, TicketResponse, CheckinResponse, SendTicketRequest, GalleryResponse } from './types';

const BASE_URL = '/api/rsvp';

function authHeaders(): Record<string, string> {
  const token = localStorage.getItem('auth_token');
  if (token) {
    return { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` };
  }
  return { 'Content-Type': 'application/json' };
}

async function handleResponse<T>(response: Response): Promise<T> {
  if (response.status === 401 || response.status === 403) {
    localStorage.removeItem('auth_token');
    const error = await response.json().catch(() => ({ message: 'Authentication required' }));
    throw new Error(error.error || error.message || 'Authentication required');
  }
  if (!response.ok) {
    const error = await response.json().catch(() => ({ message: 'Request failed' }));
    throw new Error(error.error || error.message || `HTTP ${response.status}`);
  }
  if (response.status === 204) return undefined as T;
  return response.json();
}

export async function fetchAllRsvps(): Promise<RsvpResponse[]> {
  const res = await fetch(BASE_URL);
  return handleResponse(res);
}

export async function fetchRsvpById(id: number): Promise<RsvpResponse> {
  const res = await fetch(`${BASE_URL}/${id}`);
  return handleResponse(res);
}

export async function createRsvp(data: RsvpRequest): Promise<RsvpResponse> {
  const res = await fetch(BASE_URL, {
    method: 'POST',
    headers: authHeaders(),
    body: JSON.stringify(data),
  });
  return handleResponse(res);
}

export async function updateRsvp(id: number, data: RsvpRequest): Promise<RsvpResponse> {
  const res = await fetch(`${BASE_URL}/${id}`, {
    method: 'PUT',
    headers: authHeaders(),
    body: JSON.stringify(data),
  });
  return handleResponse(res);
}

export async function deleteRsvp(id: number): Promise<void> {
  const res = await fetch(`${BASE_URL}/${id}`, {
    method: 'DELETE',
    headers: authHeaders(),
  });
  return handleResponse(res);
}

export async function fetchSummary(): Promise<RsvpSummaryResponse> {
  const res = await fetch(`${BASE_URL}/summary`);
  return handleResponse(res);
}

export async function fetchFamilyTree(): Promise<FamilyTreeResponse> {
  const res = await fetch('/api/family-tree');
  return handleResponse(res);
}

export async function updateFamilyMember(
  id: number,
  data: { name: string; ageGroup: string }
): Promise<FamilyTreeNode> {
  const res = await fetch(`/api/family-tree/members/${id}`, {
    method: 'PUT',
    headers: authHeaders(),
    body: JSON.stringify(data),
  });
  return handleResponse(res);
}

export async function createFamilyMember(data: FamilyMemberRequest): Promise<FamilyTreeNode> {
  const res = await fetch('/api/family-tree/members', {
    method: 'POST',
    headers: authHeaders(),
    body: JSON.stringify(data),
  });
  return handleResponse(res);
}

export async function deleteFamilyMember(id: number): Promise<void> {
  const res = await fetch(`/api/family-tree/members/${id}`, {
    method: 'DELETE',
    headers: authHeaders(),
  });
  return handleResponse(res);
}

// --- Meetings ---

const MEETINGS_URL = '/api/meetings';

export async function fetchMeetings(): Promise<MeetingResponse[]> {
  const res = await fetch(MEETINGS_URL);
  return handleResponse(res);
}

export async function createMeeting(data: MeetingRequest): Promise<MeetingResponse> {
  const res = await fetch(MEETINGS_URL, {
    method: 'POST',
    headers: authHeaders(),
    body: JSON.stringify(data),
  });
  return handleResponse(res);
}

export async function updateMeeting(id: number, data: MeetingRequest): Promise<MeetingResponse> {
  const res = await fetch(`${MEETINGS_URL}/${id}`, {
    method: 'PUT',
    headers: authHeaders(),
    body: JSON.stringify(data),
  });
  return handleResponse(res);
}

export async function deleteMeeting(id: number): Promise<void> {
  const res = await fetch(`${MEETINGS_URL}/${id}`, {
    method: 'DELETE',
    headers: authHeaders(),
  });
  return handleResponse(res);
}

// --- Events ---

const EVENTS_URL = '/api/events';

export async function fetchEvents(): Promise<EventResponse[]> {
  const res = await fetch(EVENTS_URL);
  return handleResponse(res);
}

export async function createEvent(data: EventRequest): Promise<EventResponse> {
  const res = await fetch(EVENTS_URL, {
    method: 'POST',
    headers: authHeaders(),
    body: JSON.stringify(data),
  });
  return handleResponse(res);
}

export async function updateEvent(id: number, data: EventRequest): Promise<EventResponse> {
  const res = await fetch(`${EVENTS_URL}/${id}`, {
    method: 'PUT',
    headers: authHeaders(),
    body: JSON.stringify(data),
  });
  return handleResponse(res);
}

export async function deleteEvent(id: number): Promise<void> {
  const res = await fetch(`${EVENTS_URL}/${id}`, {
    method: 'DELETE',
    headers: authHeaders(),
  });
  return handleResponse(res);
}

export async function registerForEvent(eventId: number, data: EventRegisterRequest): Promise<EventResponse> {
  const res = await fetch(`${EVENTS_URL}/${eventId}/register`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data),
  });
  return handleResponse(res);
}

export async function unregisterFromEvent(eventId: number, memberId: number): Promise<void> {
  const res = await fetch(`${EVENTS_URL}/${eventId}/register/${memberId}`, { method: 'DELETE' });
  return handleResponse(res);
}

// --- Payments ---

const PAYMENTS_URL = '/api/payments';

export async function fetchPaymentSummaries(): Promise<PaymentSummaryResponse[]> {
  const res = await fetch(`${PAYMENTS_URL}/summary`, { headers: authHeaders() });
  return handleResponse(res);
}

export async function createCheckoutSession(data: CheckoutRequest): Promise<{ url: string }> {
  const res = await fetch(`${PAYMENTS_URL}/checkout`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data),
  });
  return handleResponse(res);
}

// --- Admin Users ---

const ADMIN_URL = '/api/admin/users';

export async function fetchAdminUsers(): Promise<AdminUserResponse[]> {
  const res = await fetch(ADMIN_URL, { headers: authHeaders() });
  return handleResponse(res);
}

export async function addAdminUser(data: { email: string; name: string }): Promise<AdminUserResponse> {
  const res = await fetch(ADMIN_URL, {
    method: 'POST',
    headers: authHeaders(),
    body: JSON.stringify(data),
  });
  return handleResponse(res);
}

export async function removeAdminUser(id: number): Promise<void> {
  const res = await fetch(`${ADMIN_URL}/${id}`, {
    method: 'DELETE',
    headers: authHeaders(),
  });
  return handleResponse(res);
}

// --- Admin Reset ---

export async function resetPayments(): Promise<{ deleted: number; message: string }> {
  const res = await fetch(`${ADMIN_URL.replace('/users', '/reset/payments')}`, {
    method: 'DELETE',
    headers: authHeaders(),
  });
  return handleResponse(res);
}

export async function resetCheckins(): Promise<{ reset: number; message: string }> {
  const res = await fetch(`${ADMIN_URL.replace('/users', '/reset/checkins')}`, {
    method: 'DELETE',
    headers: authHeaders(),
  });
  return handleResponse(res);
}

// --- Check-in ---

const CHECKIN_URL = '/api/checkin';

export async function fetchTicket(token: string): Promise<TicketResponse> {
  const res = await fetch(`${CHECKIN_URL}/ticket/${token}`);
  return handleResponse(res);
}

export async function performCheckin(token: string): Promise<CheckinResponse> {
  const res = await fetch(`${CHECKIN_URL}/${token}`, {
    method: 'POST',
    headers: authHeaders(),
  });
  return handleResponse(res);
}

export async function sendTicket(data: SendTicketRequest): Promise<{ message: string }> {
  const res = await fetch(`${CHECKIN_URL}/send`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data),
  });
  return handleResponse(res);
}

export async function fetchCheckinCapabilities(): Promise<{ email: boolean; sms: boolean }> {
  const res = await fetch(`${CHECKIN_URL}/capabilities`);
  return handleResponse(res);
}

export async function fetchCheckinStatus(): Promise<{ total: number; checkedIn: number; tickets: TicketResponse[] }> {
  const res = await fetch(`${CHECKIN_URL}/status`, { headers: authHeaders() });
  return handleResponse(res);
}

// --- Gallery ---

const GALLERY_URL = '/api/gallery';

export async function fetchGalleryPhotos(pageToken?: string): Promise<GalleryResponse> {
  const params = new URLSearchParams();
  if (pageToken) params.set('pageToken', pageToken);
  const url = params.toString() ? `${GALLERY_URL}?${params}` : GALLERY_URL;
  const res = await fetch(url);
  return handleResponse(res);
}
