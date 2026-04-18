/**
 * Example configuration for a new family reunion website.
 *
 * To get started:
 * 1. Copy this file to reunion.config.ts
 * 2. Replace all placeholder values with your family's information
 * 3. Replace images in /public/ (see "Images" section below)
 * 4. See SETUP.md for full instructions
 */

export const reunionConfig = {
  // ── Family Identity ──────────────────────────────────────────────
  family: {
    name: 'Smith',                              // Your family surname
    fullTitle: 'Smith Family Reunion',           // Shown in header, tickets, emails
    founders: 'John & Mary Smith',               // Displayed on family tree root node
    established: 1960,                           // Year the family line started
    subtitle: 'Celebrating the legacy of John & Mary Smith — Est. 1960',
    motto: {
      text: 'Family is not an important thing. It is everything.',  // Your family motto or quote
      attribution: '— Michael J. Fox',                              // Attribution
    },
    whoInvited: 'All descendants of John & Mary Smith, plus guests',
  },

  // ── Event Details ────────────────────────────────────────────────
  event: {
    dates: 'July 4 – 6, 2026',                 // Display string for the event dates
    startDate: '2026-07-04T10:00:00',           // ISO format — drives the countdown timer
    time: 'Saturday 10:00 AM – Monday Evening',
    address: '123 Main Street, Anytown, USA',
    mapEmbedUrl:                                 // Google Maps embed URL for your venue
      'https://maps.google.com/maps?q=123+Main+Street,+Anytown,+USA&t=&z=15&ie=UTF8&iwloc=&output=embed',
    directionsUrl:                               // Google Maps directions link
      'https://www.google.com/maps/dir/?api=1&destination=123+Main+Street,+Anytown,+USA',
  },

  // ── Fundraising ──────────────────────────────────────────────────
  angelGoal: 1000,  // Angel contributor fundraising goal in dollars

  // ── Analytics ────────────────────────────────────────────────────
  // Google Analytics 4 Measurement ID. Set to '' to disable tracking.
  googleAnalyticsId: '',

  // ── Images (paths relative to /public/) ──────────────────────────
  // Replace these files in the frontend/public/ directory:
  //   - heroPhoto: Main family photo shown on homepage and RSVP page
  //   - founderPhoto: Photo of founders shown on family tree root node
  //   - angelBackground: Background image for the angel contributor section
  images: {
    heroPhoto: '/FamilyFirst.jpg',
    founderPhoto: '/founders.jpg',
    angelBackground: '/angel-contributor.png',
  },

  // ── Theme Colors ─────────────────────────────────────────────────
  // These override the CSS custom properties at runtime.
  // Pick two colors that represent your family's brand.
  theme: {
    primaryColor: '#1a4d2e',    // e.g., forest green
    accentColor: '#d4a017',     // e.g., gold
  },

  // ── Branch Color Palette ─────────────────────────────────────────
  // Pastel colors auto-assigned to family branches in the order they
  // appear. Provide at least as many colors as branches in your family.
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
  // Colors for each generation level in the tree (index = generation number).
  generationColors: ['#c9a84c', '#2c3e6b', '#c0392b', '#1a8a6e', '#8e44ad'],
  generationLabels: ['Founders', 'Gen 1', 'Gen 2', 'Gen 3', 'Gen 4+'],
};
