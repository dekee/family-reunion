import { createContext, useContext, useState, useEffect, useCallback, type ReactNode } from 'react';
import { reunionConfig } from './reunion.config';
import { fetchSiteConfig } from './api';

// ── Color utilities ────────────────────────────────────────────────

function hexToRgb(hex: string): [number, number, number] {
  const h = hex.replace('#', '');
  return [
    parseInt(h.substring(0, 2), 16),
    parseInt(h.substring(2, 4), 16),
    parseInt(h.substring(4, 6), 16),
  ];
}

function rgbToHex(r: number, g: number, b: number): string {
  return '#' + [r, g, b].map(c => Math.max(0, Math.min(255, Math.round(c))).toString(16).padStart(2, '0')).join('');
}

function lighten(hex: string, amount: number): string {
  const [r, g, b] = hexToRgb(hex);
  return rgbToHex(
    r + (255 - r) * amount,
    g + (255 - g) * amount,
    b + (255 - b) * amount,
  );
}

function darken(hex: string, amount: number): string {
  const [r, g, b] = hexToRgb(hex);
  return rgbToHex(r * (1 - amount), g * (1 - amount), b * (1 - amount));
}

function rgba(hex: string, alpha: number): string {
  const [r, g, b] = hexToRgb(hex);
  return `rgba(${r}, ${g}, ${b}, ${alpha})`;
}

/** Apply a full derived palette from two base colors */
function applyThemeColors(primary: string, accent: string) {
  const root = document.documentElement;

  // Primary palette
  root.style.setProperty('--color-navy-primary', primary);
  root.style.setProperty('--color-navy-secondary', lighten(primary, 0.25));
  root.style.setProperty('--color-navy-dark', darken(primary, 0.3));
  root.style.setProperty('--color-text-heading', primary);
  root.style.setProperty('--color-bg-hover', rgba(primary, 0.02));
  root.style.setProperty('--color-bg-subtle', rgba(primary, 0.04));
  root.style.setProperty('--color-bg-badge', rgba(primary, 0.08));

  // Accent palette
  root.style.setProperty('--color-gold', accent);
  root.style.setProperty('--color-gold-hover', lighten(accent, 0.15));
  root.style.setProperty('--color-gold-light', rgba(accent, 0.15));
  root.style.setProperty('--color-gold-ring', rgba(accent, 0.2));
}

type ReunionConfig = typeof reunionConfig;

interface SiteConfigContextType {
  config: ReunionConfig;
  loading: boolean;
  refreshConfig: () => Promise<void>;
}

const SiteConfigContext = createContext<SiteConfigContextType>({
  config: reunionConfig,
  loading: true,
  refreshConfig: async () => {},
});

export function useSiteConfig() {
  return useContext(SiteConfigContext);
}

function mergeConfig(
  base: ReunionConfig,
  settings: Record<string, string>,
  imageUrls: Record<string, string>,
): ReunionConfig {
  return {
    ...base,
    family: {
      name: settings['family.name'] || base.family.name,
      fullTitle: settings['family.fullTitle'] || base.family.fullTitle,
      founders: settings['family.founders'] || base.family.founders,
      established: settings['family.established']
        ? parseInt(settings['family.established'])
        : base.family.established,
      subtitle: settings['family.subtitle'] || base.family.subtitle,
      motto: {
        text: settings['family.motto.text'] || base.family.motto.text,
        attribution: settings['family.motto.attribution'] || base.family.motto.attribution,
      },
      whoInvited: settings['family.whoInvited'] || base.family.whoInvited,
    },
    event: {
      dates: settings['event.dates'] || base.event.dates,
      startDate: settings['event.startDate'] || base.event.startDate,
      time: settings['event.time'] || base.event.time,
      address: settings['event.address'] || base.event.address,
      mapEmbedUrl: settings['event.mapEmbedUrl'] || base.event.mapEmbedUrl,
      directionsUrl: settings['event.directionsUrl'] || base.event.directionsUrl,
    },
    angelGoal: settings['angelGoal'] ? parseInt(settings['angelGoal']) : base.angelGoal,
    googleAnalyticsId: base.googleAnalyticsId,
    images: {
      heroPhoto: imageUrls['heroPhoto'] || base.images.heroPhoto,
      founderPhoto: imageUrls['founderPhoto'] || base.images.founderPhoto,
      angelBackground: imageUrls['angelBackground'] || base.images.angelBackground,
    },
    theme: {
      primaryColor: settings['theme.primaryColor'] || base.theme.primaryColor,
      accentColor: settings['theme.accentColor'] || base.theme.accentColor,
    },
    branchPalette: base.branchPalette,
    generationColors: base.generationColors,
    generationLabels: base.generationLabels,
  };
}

export function SiteConfigProvider({ children }: { children: ReactNode }) {
  const [config, setConfig] = useState<ReunionConfig>(reunionConfig);
  const [loading, setLoading] = useState(true);

  const loadConfig = useCallback(async () => {
    try {
      const data = await fetchSiteConfig();
      const merged = mergeConfig(reunionConfig, data.settings, data.imageUrls);
      setConfig(merged);
      applyThemeColors(merged.theme.primaryColor, merged.theme.accentColor);
    } catch {
      // Fallback to static config
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { loadConfig(); }, [loadConfig]);

  return (
    <SiteConfigContext.Provider value={{ config, loading, refreshConfig: loadConfig }}>
      {children}
    </SiteConfigContext.Provider>
  );
}
