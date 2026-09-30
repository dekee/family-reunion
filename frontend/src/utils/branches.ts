import { feeForAge } from '../constants/ageGroups';
import type { FamilyTreeNode, PaymentSummaryResponse, PaidGuestInfo, PaidMemberInfo, TshirtSize } from '../types';

/**
 * Shared by the pay page and the donations page. Both walk the family tree into a flat, per-person
 * list and both need to know who has already been paid for — the donations page to hide them, the pay
 * page to badge them.
 */
export interface FlatMember {
  id: number;
  name: string;
  ageGroup: string;
  fee: number;
  depth: number;
  paid?: boolean;
  /** Set when paid: the payment line item that holds this member's T-shirt size */
  lineItemId?: number;
  tshirtSize?: TshirtSize | null;
}

export interface BranchData {
  node: FamilyTreeNode;
  members: FlatMember[];
  payment?: PaymentSummaryResponse;
  paidGuests: PaidGuestInfo[];
}

/**
 * Flattens a branch into its people, skipping anyone excluded from the RSVP.
 *
 * `fee` is resolved here, at flatten time, so callers must have applied the server's fee schedule
 * (`setFees`) before calling this — otherwise every member carries the build-time default.
 */
export function flattenBranch(node: FamilyTreeNode, depth: number): FlatMember[] {
  const result: FlatMember[] = [];
  if (!node.excludeFromRsvp) {
    result.push({
      id: node.id,
      name: node.name,
      ageGroup: node.ageGroup,
      fee: feeForAge(node.ageGroup),
      depth,
    });
  }
  for (const child of node.children) {
    result.push(...flattenBranch(child, depth + 1));
  }
  return result;
}

export function markPaidMembers(members: FlatMember[], paidMemberIds: number[], paidMembers: PaidMemberInfo[]): FlatMember[] {
  const paidSet = new Set(paidMemberIds);
  const infoById = new Map(paidMembers.map(pm => [pm.memberId, pm]));
  return members.map(m => {
    const info = infoById.get(m.id);
    return {
      ...m,
      paid: paidSet.has(m.id) || info !== undefined,
      lineItemId: info?.lineItemId,
      tshirtSize: info?.tshirtSize ?? null,
    };
  });
}

export function branchSlug(name: string): string {
  return name.replace(/ - Done$/, '').toLowerCase().replace(/[^a-z0-9]+/g, '-').replace(/-+$/, '');
}
