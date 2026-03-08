const BRANCH_COLORS: Record<string, string> = {
  'Gail': 'var(--branch-gail)',
  'Wesley II': 'var(--branch-wesley-ii)',
  'Norris': 'var(--branch-norris)',
  'Michael': 'var(--branch-michael)',
  'Cheryl': 'var(--branch-cheryl)',
  'Stephen': 'var(--branch-stephen)',
  'Kendra': 'var(--branch-kendra)',
  'Wendell': 'var(--branch-wendell)',
  'Donald': 'var(--branch-donald)',
  'Myra': 'var(--branch-myra)',
  'Chantell': 'var(--branch-chantell)',
};

export function getBranchColor(name: string): string {
  return BRANCH_COLORS[name] || 'var(--color-border)';
}
