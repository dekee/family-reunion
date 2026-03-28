import { useState, useRef, useEffect } from 'react';
import { BrowserRouter, Routes, Route, NavLink, Link, useLocation } from 'react-router-dom';
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
import Budget from './components/Budget';
import PayAndRsvp from './components/PayAndRsvp';
import TicketPage from './components/TicketPage';
import AdminPage from './components/AdminPage';
import PaymentHistory from './components/PaymentHistory';
import CheckinDashboard from './components/CheckinDashboard';
import Gallery from './components/Gallery';
import ThankYou from './components/ThankYou';
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

function App() {
  const { user, isAdmin, login, logout } = useAuth();
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
              <NavLink to="/pay" onClick={closeMenu}>Pay & RSVP</NavLink>
              <NavLink to="/events" onClick={closeMenu}>Events</NavLink>
              <NavLink to="/meetings" onClick={closeMenu}>Meetings</NavLink>
              {isAdmin && <NavLink to="/budget" className="nav-admin" onClick={closeMenu}>Budget</NavLink>}
              <NavLink to="/members" onClick={closeMenu}>Members</NavLink>
              <NavLink to="/family-tree" onClick={closeMenu}>Family Tree</NavLink>
              <NavLink to="/gallery" onClick={closeMenu}>Gallery</NavLink>
              <NavLink to="/thank-you" onClick={closeMenu}>Thank You</NavLink>
              {isAdmin && <NavLink to="/rsvp" className="nav-admin" onClick={closeMenu}>RSVP</NavLink>}
              {isAdmin && <NavLink to="/checkin" className="nav-admin" onClick={closeMenu}>Check-In</NavLink>}
              {isAdmin && <NavLink to="/payments" className="nav-admin" onClick={closeMenu}>Payments</NavLink>}
              {isAdmin && <NavLink to="/admin" className="nav-admin" onClick={closeMenu}>Admin</NavLink>}
            </nav>
          </header>

          <main className="app-main">
            <Routes>
              <Route path="/" element={<HomePage />} />
              <Route path="/rsvp" element={<RsvpPage />} />
              <Route path="/events" element={<Events />} />
              <Route path="/meetings" element={<Meetings />} />
              {isAdmin && <Route path="/budget" element={<Budget />} />}
              <Route path="/pay" element={<PayAndRsvp />} />
              <Route path="/pay/:branch" element={<PayAndRsvp />} />
              <Route path="/ticket/:token" element={<TicketPage />} />
              <Route path="/members" element={<FamilyMembers />} />
              <Route path="/family-tree" element={<FamilyTree />} />
              <Route path="/gallery" element={<Gallery />} />
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
