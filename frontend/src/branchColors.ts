import { reunionConfig } from './reunion.config';

const branchColorMap = new Map<string, { light: string; dark: string }>();
let initialized = false;

/**
 * Initialize branch colors from a list of branch names.
 * Call this once when the family tree data loads.
 * Colors are auto-assigned from the palette in reunion.config.ts.
 */
export function initBranchColors(branchNames: string[]): void {
  branchColorMap.clear();
  const { light, dark } = reunionConfig.branchPalette;

  branchNames.forEach((name, i) => {
    const key = name.split(' ')[0]; // Use first name as key
    branchColorMap.set(key, {
      light: light[i % light.length],
      dark: dark[i % dark.length],
    });
  });

  // Inject CSS custom properties for both themes
  const root = document.documentElement;
  branchColorMap.forEach((colors, key) => {
    const cssKey = `--branch-${key.toLowerCase().replace(/\s+/g, '-')}`;
    root.style.setProperty(cssKey, colors.light);
  });

  // Also set dark mode vars via a style element
  let darkStyle = document.getElementById('branch-dark-colors');
  if (!darkStyle) {
    darkStyle = document.createElement('style');
    darkStyle.id = 'branch-dark-colors';
    document.head.appendChild(darkStyle);
  }
  const darkRules: string[] = [];
  branchColorMap.forEach((colors, key) => {
    const cssKey = `--branch-${key.toLowerCase().replace(/\s+/g, '-')}`;
    darkRules.push(`  ${cssKey}: ${colors.dark};`);
  });
  darkStyle.textContent = `[data-theme="dark"] {\n${darkRules.join('\n')}\n}`;

  initialized = true;
}

export function getBranchColor(name: string): string {
  if (!initialized) return 'var(--color-border)';
  const key = name.split(' ')[0];
  const colors = branchColorMap.get(key);
  if (!colors) return 'var(--color-border)';
  return `var(--branch-${key.toLowerCase().replace(/\s+/g, '-')}, ${colors.light})`;
}
