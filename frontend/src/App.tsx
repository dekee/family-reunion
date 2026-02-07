import { useState, useRef } from 'react';
import { BrowserRouter, Routes, Route, NavLink } from 'react-router-dom';
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
import AdminPage from './components/AdminPage';
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

function App() {
  const { user, isAdmin, login, logout } = useAuth();

  return (
    <BrowserRouter>
      <ToastProvider>
        <div className="app">
          <header className="app-header">
            <div className="header-top">
              <h1>Tumblin Family Reunion</h1>
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
            </div>
            <nav className="app-nav">
              <NavLink to="/" end>Home</NavLink>
              <NavLink to="/rsvp">RSVP</NavLink>
              <NavLink to="/events">Events</NavLink>
              <NavLink to="/meetings">Meetings</NavLink>
              <NavLink to="/budget">Budget</NavLink>
              <NavLink to="/members">Members</NavLink>
              <NavLink to="/family-tree">Family Tree</NavLink>
              {isAdmin && <NavLink to="/admin">Admin</NavLink>}
            </nav>
          </header>

          <main className="app-main">
            <Routes>
              <Route path="/" element={<HomePage />} />
              <Route path="/rsvp" element={<RsvpPage />} />
              <Route path="/events" element={<Events />} />
              <Route path="/meetings" element={<Meetings />} />
              <Route path="/budget" element={<Budget />} />
              <Route path="/members" element={<FamilyMembers />} />
              <Route path="/family-tree" element={<FamilyTree />} />
              {isAdmin && <Route path="/admin" element={<AdminPage />} />}
            </Routes>
          </main>
        </div>
      </ToastProvider>
    </BrowserRouter>
  );
}

export default App;
