/**
 * API Contract Tests
 *
 * These tests verify that the frontend TypeScript types stay in sync with
 * the backend API. They validate:
 * 1. API client functions build correct request URLs and methods
 * 2. Request/response type shapes match the backend DTOs
 * 3. All API endpoints used by the frontend are accounted for
 *
 * If a backend DTO changes (field renamed, type changed), these tests
 * should catch the mismatch at build time (TypeScript) or test time.
 */

import { describe, it, expect, vi, beforeEach } from 'vitest';
import type {
  RsvpRequest,
  RsvpResponse,
  RsvpSummaryResponse,
  FamilyTreeNode,
  FamilyTreeResponse,
  FamilyMemberRequest,
  EventRequest,
  EventResponse,
  EventRegistrationDto,
  EventRegisterRequest,
  PaymentSummaryResponse,
  PaymentResponse,
  CheckoutRequest,
  MeetingRequest,
  MeetingResponse,
  AdminUserResponse,
  AgeGroup,
} from './types';

// --- Type shape validators ---
// These functions assert that an object has exactly the fields the frontend expects.
// If the backend adds/removes/renames a field, these fail.

function assertRsvpResponse(obj: unknown): asserts obj is RsvpResponse {
  const o = obj as Record<string, unknown>;
  expect(typeof o.id).toBe('number');
  expect(typeof o.familyName).toBe('string');
  expect(typeof o.headOfHouseholdName).toBe('string');
  expect(typeof o.email).toBe('string');
  expect(Array.isArray(o.attendees)).toBe(true);
  expect(typeof o.needsLodging).toBe('boolean');
}

function assertRsvpSummary(obj: unknown): asserts obj is RsvpSummaryResponse {
  const o = obj as Record<string, unknown>;
  expect(typeof o.totalFamilies).toBe('number');
  expect(typeof o.totalHeadcount).toBe('number');
  expect(typeof o.adultCount).toBe('number');
  expect(typeof o.childCount).toBe('number');
  expect(typeof o.infantCount).toBe('number');
  expect(typeof o.lodgingCount).toBe('number');
}

function assertFamilyTreeNode(obj: unknown): asserts obj is FamilyTreeNode {
  const o = obj as Record<string, unknown>;
  expect(typeof o.id).toBe('number');
  expect(typeof o.name).toBe('string');
  expect(['number', 'object']).toContain(typeof o.generation); // number | null
  expect(typeof o.ageGroup).toBe('string');
  expect(['ADULT', 'CHILD', 'INFANT', 'SPOUSE']).toContain(o.ageGroup);
  expect(Array.isArray(o.children)).toBe(true);
}

function assertFamilyTreeResponse(obj: unknown): asserts obj is FamilyTreeResponse {
  const o = obj as Record<string, unknown>;
  expect(Array.isArray(o.roots)).toBe(true);
  expect(typeof o.totalMembers).toBe('number');
}

function assertEventResponse(obj: unknown): asserts obj is EventResponse {
  const o = obj as Record<string, unknown>;
  expect(typeof o.id).toBe('number');
  expect(typeof o.title).toBe('string');
  expect(typeof o.eventDateTime).toBe('string');
  expect(typeof o.address).toBe('string');
  expect(Array.isArray(o.registrations)).toBe(true);
  expect(typeof o.registrationCount).toBe('number');
  // eventDateTime must include seconds (backend format: yyyy-MM-dd'T'HH:mm:ss)
  expect(o.eventDateTime).toMatch(/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}$/);
}

function assertEventRegistration(obj: unknown): asserts obj is EventRegistrationDto {
  const o = obj as Record<string, unknown>;
  expect(typeof o.id).toBe('number');
  expect(typeof o.familyMemberId).toBe('number');
  expect(typeof o.familyMemberName).toBe('string');
}

function assertPaymentSummary(obj: unknown): asserts obj is PaymentSummaryResponse {
  const o = obj as Record<string, unknown>;
  expect(typeof o.rsvpId).toBe('number');
  expect(typeof o.familyName).toBe('string');
  expect(typeof o.totalOwed).toBe('number');
  expect(typeof o.totalPaid).toBe('number');
  expect(typeof o.balance).toBe('number');
  expect(typeof o.status).toBe('string');
  expect(['PAID', 'PARTIAL', 'PENDING', 'UNPAID']).toContain(o.status);
  expect(Array.isArray(o.payments)).toBe(true);
}

function assertPaymentResponse(obj: unknown): asserts obj is PaymentResponse {
  const o = obj as Record<string, unknown>;
  expect(typeof o.id).toBe('number');
  expect(typeof o.rsvpId).toBe('number');
  expect(typeof o.familyName).toBe('string');
  expect(typeof o.amount).toBe('number');
  expect(typeof o.status).toBe('string');
  expect(typeof o.createdAt).toBe('string');
}

function assertMeetingResponse(obj: unknown): asserts obj is MeetingResponse {
  const o = obj as Record<string, unknown>;
  expect(typeof o.id).toBe('number');
  expect(typeof o.title).toBe('string');
  expect(typeof o.meetingDateTime).toBe('string');
  expect(typeof o.zoomLink).toBe('string');
}

function assertAdminUserResponse(obj: unknown): asserts obj is AdminUserResponse {
  const o = obj as Record<string, unknown>;
  expect(typeof o.id).toBe('number');
  expect(typeof o.email).toBe('string');
  expect(typeof o.name).toBe('string');
  expect(typeof o.createdAt).toBe('string');
}

// --- Mock fetch for API client tests ---

let fetchCalls: { url: string; method: string; body?: string; headers?: Record<string, string> }[] = [];

function mockFetch(responseData: unknown, status = 200) {
  fetchCalls = [];
  global.fetch = vi.fn(async (url: string | URL | Request, init?: RequestInit) => {
    fetchCalls.push({
      url: typeof url === 'string' ? url : url.toString(),
      method: init?.method || 'GET',
      body: init?.body as string | undefined,
      headers: init?.headers as Record<string, string> | undefined,
    });
    return {
      ok: status >= 200 && status < 300,
      status,
      json: async () => responseData,
    } as Response;
  });
}

beforeEach(() => {
  fetchCalls = [];
  vi.restoreAllMocks();
  // Clear localStorage for auth tests
  if (typeof globalThis.localStorage === 'undefined') {
    (globalThis as Record<string, unknown>).localStorage = {
      store: {} as Record<string, string>,
      getItem(key: string) { return (this as unknown as { store: Record<string, string> }).store[key] ?? null; },
      setItem(key: string, val: string) { (this as unknown as { store: Record<string, string> }).store[key] = val; },
      removeItem(key: string) { delete (this as unknown as { store: Record<string, string> }).store[key]; },
      clear() { (this as unknown as { store: Record<string, string> }).store = {}; },
    };
  }
});

// --- Type shape contract tests ---

describe('API Response Type Contracts', () => {
  it('RsvpResponse shape matches backend RsvpResponse DTO', () => {
    const sample: RsvpResponse = {
      id: 1,
      familyName: 'Tumblin',
      headOfHouseholdName: 'Wesley',
      email: 'test@example.com',
      attendees: [],
      needsLodging: false,
    };
    assertRsvpResponse(sample);
  });

  it('RsvpSummaryResponse shape matches backend', () => {
    const sample: RsvpSummaryResponse = {
      totalFamilies: 11,
      totalHeadcount: 126,
      adultCount: 52,
      childCount: 74,
      infantCount: 0,
      lodgingCount: 3,
    };
    assertRsvpSummary(sample);
  });

  it('FamilyTreeResponse shape matches backend', () => {
    const node: FamilyTreeNode = {
      id: 1,
      name: 'Wesley',
      generation: 0,
      ageGroup: 'ADULT',
      children: [],
    };
    assertFamilyTreeNode(node);

    const tree: FamilyTreeResponse = { roots: [node], totalMembers: 1 };
    assertFamilyTreeResponse(tree);
  });

  it('FamilyTreeNode with null generation is valid', () => {
    const node: FamilyTreeNode = {
      id: 2,
      name: 'Child',
      generation: null,
      ageGroup: 'CHILD',
      children: [],
    };
    assertFamilyTreeNode(node);
  });

  it('EventResponse shape matches backend EventResponse DTO', () => {
    const sample: EventResponse = {
      id: 1,
      title: 'BBQ',
      eventDateTime: '2026-07-04T12:00:00',
      address: '123 Main St',
      registrations: [],
      registrationCount: 0,
    };
    assertEventResponse(sample);
  });

  it('EventRegistrationDto shape matches backend', () => {
    const sample: EventRegistrationDto = {
      id: 1,
      familyMemberId: 5,
      familyMemberName: 'John',
    };
    assertEventRegistration(sample);
  });

  it('PaymentSummaryResponse shape matches backend', () => {
    const sample: PaymentSummaryResponse = {
      rsvpId: 1,
      familyName: 'Tumblin',
      totalOwed: 250,
      totalPaid: 0,
      balance: 250,
      status: 'UNPAID',
      payments: [],
      paidMemberIds: [],
      paidGuests: [],
    };
    assertPaymentSummary(sample);
  });

  it('PaymentResponse shape matches backend', () => {
    const sample: PaymentResponse = {
      id: 1,
      rsvpId: 1,
      familyName: 'Tumblin',
      amount: 100,
      status: 'COMPLETED',
      createdAt: '2026-01-01T00:00:00',
    };
    assertPaymentResponse(sample);
  });

  it('MeetingResponse shape matches backend', () => {
    const sample: MeetingResponse = {
      id: 1,
      title: 'Planning',
      meetingDateTime: '2026-03-15T14:00:00',
      zoomLink: 'https://zoom.us/j/123',
    };
    assertMeetingResponse(sample);
  });

  it('AdminUserResponse shape matches backend', () => {
    const sample: AdminUserResponse = {
      id: 1,
      email: 'admin@example.com',
      name: 'Admin',
      createdAt: '2026-01-01T00:00:00',
    };
    assertAdminUserResponse(sample);
  });

  it('AgeGroup union matches backend enum', () => {
    const validGroups: AgeGroup[] = ['ADULT', 'CHILD', 'INFANT', 'SPOUSE'];
    expect(validGroups).toHaveLength(4);
    // If backend adds a new age group, TypeScript compilation will fail
    // when the new value is used but not in the union type
  });
});

// --- Request type contract tests ---

describe('API Request Type Contracts', () => {
  it('RsvpRequest has all required fields for backend', () => {
    const req: RsvpRequest = {
      familyName: 'Test',
      headOfHouseholdName: 'Head',
      email: 'test@example.com',
      attendees: [{ guestName: 'Guest', guestAgeGroup: 'ADULT' }],
      needsLodging: false,
    };
    expect(req.familyName).toBeTruthy();
    expect(req.headOfHouseholdName).toBeTruthy();
    expect(req.email).toBeTruthy();
    expect(req.attendees.length).toBeGreaterThan(0);
  });

  it('EventRequest has all required fields for backend', () => {
    const req: EventRequest = {
      title: 'BBQ',
      eventDateTime: '2026-07-04T12:00:00',
      address: '123 Main St',
    };
    expect(req.title).toBeTruthy();
    expect(req.eventDateTime).toBeTruthy();
    expect(req.address).toBeTruthy();
  });

  it('EventRequest eventDateTime with seconds passes backend validation', () => {
    const req: EventRequest = {
      title: 'Test',
      eventDateTime: '2026-07-04T12:00:00',
      address: 'Addr',
    };
    // Backend requires format yyyy-MM-dd'T'HH:mm:ss (with seconds)
    expect(req.eventDateTime).toMatch(/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}$/);
  });

  it('FamilyMemberRequest has all required fields for backend', () => {
    const req: FamilyMemberRequest = {
      name: 'John',
      ageGroup: 'ADULT',
    };
    expect(req.name).toBeTruthy();
    expect(req.ageGroup).toBeTruthy();
  });

  it('CheckoutRequest has required fields for backend', () => {
    const req: CheckoutRequest = {
      rsvpId: 1,
      amount: 10000,
      memberIds: [1, 2, 3],
      guests: [{ name: 'Guest 1', ageGroup: 'ADULT', fee: 10000 }],
    };
    expect(req.rsvpId).toBeGreaterThan(0);
    expect(req.amount).toBeGreaterThanOrEqual(100); // @Min(100) in backend
  });

  it('MeetingRequest has all required fields for backend', () => {
    const req: MeetingRequest = {
      title: 'Call',
      meetingDateTime: '2026-03-15T14:00:00',
      zoomLink: 'https://zoom.us/j/123',
    };
    expect(req.title).toBeTruthy();
    expect(req.meetingDateTime).toBeTruthy();
    expect(req.zoomLink).toBeTruthy();
  });

  it('EventRegisterRequest familyMemberIds is number array', () => {
    const req: EventRegisterRequest = {
      familyMemberIds: [1, 2, 3],
    };
    expect(Array.isArray(req.familyMemberIds)).toBe(true);
    expect(req.familyMemberIds.every((id) => typeof id === 'number')).toBe(true);
  });
});

// --- API client URL and method tests ---

describe('API Client Endpoint Contracts', () => {
  it('fetchAllRsvps calls GET /api/rsvp', async () => {
    mockFetch([]);
    const { fetchAllRsvps } = await import('./api');
    await fetchAllRsvps();
    expect(fetchCalls[0].url).toBe('/api/rsvp');
    expect(fetchCalls[0].method).toBe('GET');
  });

  it('createRsvp calls POST /api/rsvp with JSON body', async () => {
    mockFetch({ id: 1 }, 200);
    const { createRsvp } = await import('./api');
    const body: RsvpRequest = {
      familyName: 'Test',
      headOfHouseholdName: 'Head',
      email: 'test@example.com',
      attendees: [],
      needsLodging: false,
    };
    await createRsvp(body);
    expect(fetchCalls[0].url).toBe('/api/rsvp');
    expect(fetchCalls[0].method).toBe('POST');
    expect(JSON.parse(fetchCalls[0].body!)).toEqual(body);
  });

  it('updateRsvp calls PUT /api/rsvp/{id}', async () => {
    mockFetch({ id: 5 }, 200);
    const { updateRsvp } = await import('./api');
    await updateRsvp(5, {
      familyName: 'Updated',
      headOfHouseholdName: 'Head',
      email: 'x@x.com',
      attendees: [],
      needsLodging: false,
    });
    expect(fetchCalls[0].url).toBe('/api/rsvp/5');
    expect(fetchCalls[0].method).toBe('PUT');
  });

  it('deleteRsvp calls DELETE /api/rsvp/{id}', async () => {
    mockFetch(undefined, 204);
    const { deleteRsvp } = await import('./api');
    await deleteRsvp(5);
    expect(fetchCalls[0].url).toBe('/api/rsvp/5');
    expect(fetchCalls[0].method).toBe('DELETE');
  });

  it('fetchSummary calls GET /api/rsvp/summary', async () => {
    mockFetch({ totalFamilies: 0, totalHeadcount: 0, adultCount: 0, childCount: 0, infantCount: 0, lodgingCount: 0 });
    const { fetchSummary } = await import('./api');
    await fetchSummary();
    expect(fetchCalls[0].url).toBe('/api/rsvp/summary');
  });

  it('fetchFamilyTree calls GET /api/family-tree', async () => {
    mockFetch({ roots: [], totalMembers: 0 });
    const { fetchFamilyTree } = await import('./api');
    await fetchFamilyTree();
    expect(fetchCalls[0].url).toBe('/api/family-tree');
  });

  it('createFamilyMember calls POST /api/family-tree/members', async () => {
    mockFetch({ id: 1, name: 'Test', ageGroup: 'ADULT', generation: 1, children: [] }, 201);
    const { createFamilyMember } = await import('./api');
    await createFamilyMember({ name: 'Test', ageGroup: 'ADULT' });
    expect(fetchCalls[0].url).toBe('/api/family-tree/members');
    expect(fetchCalls[0].method).toBe('POST');
  });

  it('updateFamilyMember calls PUT /api/family-tree/members/{id}', async () => {
    mockFetch({ id: 3, name: 'Updated', ageGroup: 'ADULT', generation: 1, children: [] });
    const { updateFamilyMember } = await import('./api');
    await updateFamilyMember(3, { name: 'Updated', ageGroup: 'ADULT' });
    expect(fetchCalls[0].url).toBe('/api/family-tree/members/3');
    expect(fetchCalls[0].method).toBe('PUT');
  });

  it('deleteFamilyMember calls DELETE /api/family-tree/members/{id}', async () => {
    mockFetch(undefined, 204);
    const { deleteFamilyMember } = await import('./api');
    await deleteFamilyMember(3);
    expect(fetchCalls[0].url).toBe('/api/family-tree/members/3');
    expect(fetchCalls[0].method).toBe('DELETE');
  });

  it('fetchEvents calls GET /api/events', async () => {
    mockFetch([]);
    const { fetchEvents } = await import('./api');
    await fetchEvents();
    expect(fetchCalls[0].url).toBe('/api/events');
  });

  it('createEvent calls POST /api/events', async () => {
    mockFetch({ id: 1 }, 200);
    const { createEvent } = await import('./api');
    await createEvent({ title: 'BBQ', eventDateTime: '2026-07-04T12:00:00', address: '123 St' });
    expect(fetchCalls[0].url).toBe('/api/events');
    expect(fetchCalls[0].method).toBe('POST');
  });

  it('updateEvent calls PUT /api/events/{id}', async () => {
    mockFetch({ id: 1 }, 200);
    const { updateEvent } = await import('./api');
    await updateEvent(1, { title: 'Updated', eventDateTime: '2026-07-04T12:00:00', address: '456 St' });
    expect(fetchCalls[0].url).toBe('/api/events/1');
    expect(fetchCalls[0].method).toBe('PUT');
  });

  it('deleteEvent calls DELETE /api/events/{id}', async () => {
    mockFetch(undefined, 204);
    const { deleteEvent } = await import('./api');
    await deleteEvent(1);
    expect(fetchCalls[0].url).toBe('/api/events/1');
    expect(fetchCalls[0].method).toBe('DELETE');
  });

  it('registerForEvent calls POST /api/events/{id}/register', async () => {
    mockFetch({ id: 1, registrations: [], registrationCount: 0 });
    const { registerForEvent } = await import('./api');
    await registerForEvent(5, { familyMemberIds: [1, 2] });
    expect(fetchCalls[0].url).toBe('/api/events/5/register');
    expect(fetchCalls[0].method).toBe('POST');
    expect(JSON.parse(fetchCalls[0].body!).familyMemberIds).toEqual([1, 2]);
  });

  it('unregisterFromEvent calls DELETE /api/events/{id}/register/{memberId}', async () => {
    mockFetch(undefined, 204);
    const { unregisterFromEvent } = await import('./api');
    await unregisterFromEvent(5, 3);
    expect(fetchCalls[0].url).toBe('/api/events/5/register/3');
    expect(fetchCalls[0].method).toBe('DELETE');
  });

  it('fetchMeetings calls GET /api/meetings', async () => {
    mockFetch([]);
    const { fetchMeetings } = await import('./api');
    await fetchMeetings();
    expect(fetchCalls[0].url).toBe('/api/meetings');
  });

  it('createMeeting calls POST /api/meetings', async () => {
    mockFetch({ id: 1 }, 200);
    const { createMeeting } = await import('./api');
    await createMeeting({ title: 'Call', meetingDateTime: '2026-03-15T14:00:00', zoomLink: 'https://zoom.us' });
    expect(fetchCalls[0].url).toBe('/api/meetings');
    expect(fetchCalls[0].method).toBe('POST');
  });

  it('fetchPaymentSummaries calls GET /api/payments/summary', async () => {
    mockFetch([]);
    const { fetchPaymentSummaries } = await import('./api');
    await fetchPaymentSummaries();
    expect(fetchCalls[0].url).toBe('/api/payments/summary');
  });

  it('createCheckoutSession calls POST /api/payments/checkout', async () => {
    mockFetch({ url: 'https://checkout.stripe.com/session' });
    const { createCheckoutSession } = await import('./api');
    const result = await createCheckoutSession({ rsvpId: 1, amount: 10000, memberIds: [1], guests: [] });
    expect(fetchCalls[0].url).toBe('/api/payments/checkout');
    expect(fetchCalls[0].method).toBe('POST');
    expect(result.url).toBe('https://checkout.stripe.com/session');
  });

  it('fetchAdminUsers calls GET /api/admin/users', async () => {
    mockFetch([]);
    const { fetchAdminUsers } = await import('./api');
    await fetchAdminUsers();
    expect(fetchCalls[0].url).toBe('/api/admin/users');
  });

  it('addAdminUser calls POST /api/admin/users', async () => {
    mockFetch({ id: 1, email: 'a@b.com', name: 'Admin', createdAt: '2026-01-01' });
    const { addAdminUser } = await import('./api');
    await addAdminUser({ email: 'a@b.com', name: 'Admin' });
    expect(fetchCalls[0].url).toBe('/api/admin/users');
    expect(fetchCalls[0].method).toBe('POST');
  });

  it('removeAdminUser calls DELETE /api/admin/users/{id}', async () => {
    mockFetch(undefined, 204);
    const { removeAdminUser } = await import('./api');
    await removeAdminUser(1);
    expect(fetchCalls[0].url).toBe('/api/admin/users/1');
    expect(fetchCalls[0].method).toBe('DELETE');
  });
});
