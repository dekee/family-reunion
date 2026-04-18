/**
 * Central configuration for the family reunion website.
 *
 * To customize for your family:
 * 1. Copy reunion.config.example.ts for reference
 * 2. Edit the values below to match your family
 * 3. Replace images in /public/ (heroPhoto, angelBackground)
 *
 * See SETUP.md for full instructions.
 */

export const reunionConfig = {
  // ── Family Identity ──────────────────────────────────────────────
  family: {
    name: 'Tumblin',
    fullTitle: 'Tumblin Family Reunion',
    founders: 'Wesley & Esther Tumblin',
    established: 1948,
    subtitle: 'Celebrating the legacy of Wesley & Esther Tumblin — Est. 1948',
    motto: {
      text: "How good and pleasant it is when God's people live together in unity!",
      attribution: '— Psalm 133:1',
    },
    whoInvited: 'All descendants of Wesley & Esther Tumblin, plus guests',
  },

  // ── Event Details ────────────────────────────────────────────────
  event: {
    dates: 'October 16 – 18, 2026',
    startDate: '2026-10-16T10:00:00',  // ISO format — used for countdown timer
    time: 'Friday 10:00 AM – Sunday Evening',
    address: '439 4th Street, Saint Rose, LA 70068',
    mapEmbedUrl:
      'https://maps.google.com/maps?q=439+4th+Street,+Saint+Rose,+LA+70068&t=&z=15&ie=UTF8&iwloc=&output=embed',
    directionsUrl:
      'https://www.google.com/maps/dir/?api=1&destination=439+4th+Street,+Saint+Rose,+LA+70068',
  },

  // ── Fundraising ──────────────────────────────────────────────────
  angelGoal: 2000,

  // ── Analytics ────────────────────────────────────────────────────
  // Google Analytics 4 Measurement ID. Set to '' to disable.
  googleAnalyticsId: 'G-FXLETRQ3ZG',

  // ── Images (paths relative to /public/) ──────────────────────────
  images: {
    heroPhoto: '/FamilyFirst.jpg',
    founderPhoto: '/founders.jpg',
    angelBackground: '/angel-contributor.png',
  },

  // ── Theme Colors ─────────────────────────────────────────────────
  // These override the CSS custom properties at runtime.
  theme: {
    primaryColor: '#16213e',    // Navy — headers, nav, primary buttons
    accentColor: '#c9a84c',     // Gold — CTAs, highlights, badges
  },

  // ── Branch Color Palette ─────────────────────────────────────────
  // Auto-assigned to family branches in order. Provide at least as
  // many colors as your family has branches.
  branchPalette: {
    light: [
      '#e8d5f5', '#d5e8f5', '#d5f5e0', '#f5e8d5', '#f5d5d5',
      '#d5f5f0', '#f5f0d5', '#e0d5f5', '#d5f5d5', '#f5d5e8',
      '#d5e0f5', '#f5e5f5', '#e5f5d5', '#f5d5f0', '#d5f5f5',
    ],
    dark: [
      '#3d2a4d', '#2a3d4d', '#2a4d35', '#4d3d2a', '#4d2a2a',
      '#2a4d45', '#4d4a2a', '#352a4d', '#2a4d2a', '#4d2a3d',
      '#2a354d', '#4d2a4d', '#354d2a', '#4d2a45', '#2a4d4d',
    ],
  },

  // ── Family Tree Visualization ────────────────────────────────────
  generationColors: ['#c9a84c', '#2c3e6b', '#c0392b', '#1a8a6e', '#8e44ad'],
  generationLabels: ['Founders', 'Gen 1', 'Gen 2', 'Gen 3', 'Gen 4+'],
};
