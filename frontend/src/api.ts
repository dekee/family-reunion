import type { RsvpRequest, RsvpResponse, RsvpSummaryResponse } from './types';

const BASE_URL = '/api/rsvp';

async function handleResponse<T>(response: Response): Promise<T> {
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
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data),
  });
  return handleResponse(res);
}

export async function updateRsvp(id: number, data: RsvpRequest): Promise<RsvpResponse> {
  const res = await fetch(`${BASE_URL}/${id}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data),
  });
  return handleResponse(res);
}

export async function deleteRsvp(id: number): Promise<void> {
  const res = await fetch(`${BASE_URL}/${id}`, { method: 'DELETE' });
  return handleResponse(res);
}

export async function fetchSummary(): Promise<RsvpSummaryResponse> {
  const res = await fetch(`${BASE_URL}/summary`);
  return handleResponse(res);
}
