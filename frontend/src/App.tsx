import { useState, useRef } from 'react';
import { BrowserRouter, Routes, Route, NavLink } from 'react-router-dom';
import HomePage from './components/HomePage';
import RsvpSummary from './components/RsvpSummary';
import RsvpForm from './components/RsvpForm';
import RsvpList from './components/RsvpList';
import FamilyTree from './components/FamilyTree';
import type { RsvpResponse } from './types';
import './App.css';

function RsvpPage() {
  const [refreshKey, setRefreshKey] = useState(0);
  const [editingRsvp, setEditingRsvp] = useState<RsvpResponse | null>(null);
  const formRef = useRef<HTMLDivElement>(null);

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
      <RsvpSummary refreshKey={refreshKey} />

      <div ref={formRef}>
        <RsvpForm
          onSaved={handleSaved}
          editingRsvp={editingRsvp}
          onCancelEdit={handleCancelEdit}
        />
      </div>

      <RsvpList
        refreshKey={refreshKey}
        onEdit={handleEdit}
        onDeleted={refresh}
      />
    </>
  );
}

function App() {
  return (
    <BrowserRouter>
      <div className="app">
        <header className="app-header">
          <h1>Tumblin Family Reunion</h1>
          <nav className="app-nav">
            <NavLink to="/" end>Home</NavLink>
            <NavLink to="/rsvp">RSVP</NavLink>
            <NavLink to="/family-tree">Family Tree</NavLink>
          </nav>
        </header>

        <main className="app-main">
          <Routes>
            <Route path="/" element={<HomePage />} />
            <Route path="/rsvp" element={<RsvpPage />} />
            <Route path="/family-tree" element={<FamilyTree />} />
          </Routes>
        </main>
      </div>
    </BrowserRouter>
  );
}

export default App;
