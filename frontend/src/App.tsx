import { useState, useRef, useEffect } from 'react';
import { BrowserRouter, Routes, Route, NavLink, Link, Navigate, useLocation } from 'react-router-dom';
import { GoogleLogin } from '@react-oauth/google';
import { useAuth } from './AuthContext';
import HomePage from './components/HomePage';
import RsvpSummary from './components/RsvpSummary';
import RsvpForm from './components/RsvpForm';
import RsvpList from './components/RsvpList';
import FamilyTree from './components/FamilyTree';
import FamilyMembers from './components/FamilyMembers';
import Meetings from './components/Meetings';
import Events from './components/Events';
import Volunteer from './components/Volunteer';
import Budget from './components/Budget';
import PayAndRsvp from './components/PayAndRsvp';
import Donations from './components/Donations';
import TicketPage from './components/TicketPage';
import AdminPage from './components/AdminPage';
import PaymentHistory from './components/PaymentHistory';
import CheckinDashboard from './components/CheckinDashboard';
import Gallery from './components/Gallery';
import ThankYou from './components/ThankYou';
import TshirtSurvey from './components/TshirtSurvey';
import TshirtDesignVote from './components/TshirtDesignVote';
import Tributes from './components/Tributes';
import CommandPalette from './components/CommandPalette';
import { ToastProvider } from './components/Toast';
import type { RsvpResponse } from './types';
import './App.css';

function RsvpPage() {
  const [refreshKey, setRefreshKey] = useState(0);
  const [editingRsvp, setEditingRsvp] = useState<RsvpResponse | null>(null);
  const formRef = useRef<HTMLDivElement>(null);
  const { isAdmin } = useAuth();

  const refresh = () => setRefreshKey((k) => k + 1);

  const handleEdit = (rsvp: RsvpResponse) => {
    setEditingRsvp(rsvp);
    formRef.current?.scrollIntoView({ behavior: 'smooth' });
  };

  const handleCancelEdit = () => setEditingRsvp(null);

  const handleSaved = () => {
    setEditingRsvp(null);
    refresh();
  };

  return (
    <>
      <div className="rsvp-hero">
        <img src="/FamilyFirst.jpg" alt="Tumblin Family" className="rsvp-hero-image" />
        <div className="rsvp-hero-overlay">
          <RsvpSummary refreshKey={refreshKey} />
        </div>
      </div>

      {(isAdmin || editingRsvp) && (
        <div ref={formRef}>
          <RsvpForm
            onSaved={handleSaved}
            editingRsvp={editingRsvp}
            onCancelEdit={handleCancelEdit}
          />
        </div>
      )}

      <RsvpList
        refreshKey={refreshKey}
        onEdit={handleEdit}
        onDeleted={refresh}
      />
    </>
  );
}

declare global {
  interface Window { gtag?: (...args: unknown[]) => void; }
}

function AnalyticsTracker() {
  const location = useLocation();
  useEffect(() => {
    window.gtag?.('event', 'page_view', { page_path: location.pathname + location.search });
  }, [location]);
  return null;
}

function NotFound() {
  return (
    <div style={{ textAlign: 'center', padding: '3rem 1.5rem' }}>
      <h2>Page Not Found</h2>
      <p style={{ color: 'var(--color-text-secondary)', marginBottom: '1.5rem' }}>
        The page you're looking for doesn't exist.
      </p>
      <Link to="/" className="btn-primary" style={{ textDecoration: 'none' }}>Go Home</Link>
    </div>
  );
}

const ADMIN_LINKS = [
  { to: '/pay', label: 'Pay & RSVP' },
  { to: '/rsvp', label: 'RSVP List' },
  { to: '/checkin', label: 'Check-In' },
  { to: '/payments', label: 'Payments' },
  { to: '/budget', label: 'Budget' },
  { to: '/admin', label: 'Admin Settings' },
];

/** Collapses the admin-only pages into a single "Admin" dropdown in the nav. */
function AdminMenu({ onNavigate }: { onNavigate: () => void }) {
  const [open, setOpen] = useState(false);
  const ref = useRef<HTMLDivElement>(null);
  const location = useLocation();
  const active = ADMIN_LINKS.some(
    (l) => location.pathname === l.to || location.pathname.startsWith(`${l.to}/`),
  );

  useEffect(() => {
    if (!open) return;
    const onPointerDown = (e: MouseEvent) => {
      if (ref.current && !ref.current.contains(e.target as Node)) setOpen(false);
    };
    const onKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') setOpen(false);
    };
    document.addEventListener('mousedown', onPointerDown);
    document.addEventListener('keydown', onKeyDown);
    return () => {
      document.removeEventListener('mousedown', onPointerDown);
      document.removeEventListener('keydown', onKeyDown);
    };
  }, [open]);

  return (
    <div className={`admin-menu ${open ? 'open' : ''}`} ref={ref}>
      <button
        type="button"
        className={`admin-menu-trigger ${active ? 'active' : ''}`}
        aria-haspopup="menu"
        aria-expanded={open}
        onClick={() => setOpen((o) => !o)}
      >
        Admin
        <span className="admin-menu-caret" aria-hidden="true">&#9662;</span>
      </button>
      {open && (
        <div className="admin-menu-panel" role="menu">
          {ADMIN_LINKS.map((l) => (
            <NavLink
              key={l.to}
              to={l.to}
              role="menuitem"
              onClick={() => {
                setOpen(false);
                onNavigate();
              }}
            >
              {l.label}
            </NavLink>
          ))}
        </div>
      )}
    </div>
  );
}

function App() {
  const { user, isAdmin, loading: authLoading, login, logout } = useAuth();
  // Don't redirect /pay until the stored token has been verified; otherwise an
  // admin refreshing /pay is bounced to /donations before isAdmin resolves.
  const payElement = authLoading ? null : isAdmin ? <PayAndRsvp /> : <Navigate to="/donations" replace />;
  const [menuOpen, setMenuOpen] = useState(false);
  const [darkMode, setDarkMode] = useState(() => {
    return localStorage.getItem('theme') === 'dark';
  });

  useEffect(() => {
    document.documentElement.setAttribute('data-theme', darkMode ? 'dark' : 'light');
    localStorage.setItem('theme', darkMode ? 'dark' : 'light');
  }, [darkMode]);

  const closeMenu = () => setMenuOpen(false);

  return (
    <BrowserRouter>
      <AnalyticsTracker />
      <ToastProvider>
        <CommandPalette />
        <div className="app">
          <header className="app-header">
            <div className="header-top">
              <h1>Tumblin Family Reunion</h1>
              <div className="header-right">
                <button
                  className="btn-theme-toggle"
                  onClick={() => setDarkMode((d) => !d)}
                  title={darkMode ? 'Switch to light mode' : 'Switch to dark mode'}
                  aria-label="Toggle dark mode"
                >
                  {darkMode ? '\u2600\uFE0F' : '\uD83C\uDF19'}
                </button>
                <div className="auth-section">
                  {user ? (
                    <>
                      <span className="auth-user-name">{user.name}</span>
                      {isAdmin && <span className="admin-badge">Admin</span>}
                      <button className="btn-logout" onClick={logout}>Sign Out</button>
                    </>
                  ) : (
                    <GoogleLogin
                      onSuccess={(response) => {
                        if (response.credential) {
                          login(response.credential).catch(() => {});
                        }
                      }}
                      onError={() => {}}
                      size="small"
                      theme="filled_blue"
                    />
                  )}
                </div>
                <button
                  className={`hamburger ${menuOpen ? 'open' : ''}`}
                  onClick={() => setMenuOpen((o) => !o)}
                  aria-label="Toggle navigation menu"
                  aria-expanded={menuOpen}
                >
                  <span />
                  <span />
                  <span />
                </button>
              </div>
            </div>
            <nav className={`app-nav ${menuOpen ? 'nav-open' : ''}`}>
              <NavLink to="/" end onClick={closeMenu}>Home</NavLink>
              <NavLink to="/donations" onClick={closeMenu}>Give</NavLink>
              <NavLink to="/events" onClick={closeMenu}>Events</NavLink>
              <NavLink to="/volunteer" onClick={closeMenu}>Volunteer</NavLink>
              <NavLink to="/meetings" onClick={closeMenu}>Meetings</NavLink>
              <NavLink to="/members" onClick={closeMenu}>Members</NavLink>
              <NavLink to="/family-tree" onClick={closeMenu}>Family Tree</NavLink>
              <NavLink to="/gallery" onClick={closeMenu}>Gallery</NavLink>
              <NavLink to="/tributes" onClick={closeMenu}>Tributes</NavLink>
              <NavLink to="/thank-you" onClick={closeMenu}>Thank You</NavLink>
              {isAdmin && <AdminMenu onNavigate={closeMenu} />}
            </nav>
          </header>

          <main className="app-main">
            <Routes>
              <Route path="/" element={<HomePage />} />
              {isAdmin && <Route path="/rsvp" element={<RsvpPage />} />}
              <Route path="/events" element={<Events />} />
              <Route path="/volunteer" element={<Volunteer />} />
              <Route path="/meetings" element={<Meetings />} />
              {isAdmin && <Route path="/budget" element={<Budget />} />}
              <Route path="/pay" element={payElement} />
              <Route path="/pay/:branch" element={payElement} />
              <Route path="/donations" element={<Donations />} />
              <Route path="/donations/:branch" element={<Donations />} />
              <Route path="/ticket/:token" element={<TicketPage />} />
              <Route path="/members" element={<FamilyMembers />} />
              <Route path="/family-tree" element={<FamilyTree />} />
              <Route path="/gallery" element={<Gallery />} />
              <Route path="/tshirt-survey" element={<TshirtSurvey />} />
              <Route path="/tshirt-design" element={<TshirtDesignVote />} />
              <Route path="/tributes" element={<Tributes />} />
              <Route path="/thank-you" element={<ThankYou />} />
              {isAdmin && <Route path="/checkin" element={<CheckinDashboard />} />}
              {isAdmin && <Route path="/payments" element={<PaymentHistory />} />}
              {isAdmin && <Route path="/admin" element={<AdminPage />} />}
              <Route path="*" element={<NotFound />} />
            </Routes>
          </main>
        </div>
      </ToastProvider>
    </BrowserRouter>
  );
}

export default App;
