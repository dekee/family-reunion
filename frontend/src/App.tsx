import { useState, useRef } from 'react';
import RsvpSummary from './components/RsvpSummary';
import RsvpForm from './components/RsvpForm';
import RsvpList from './components/RsvpList';
import type { RsvpResponse } from './types';
import './App.css';

function App() {
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
    <div className="app">
      <header className="app-header">
        <h1>Family Reunion RSVP</h1>
        <p>Let us know your family is coming!</p>
      </header>

      <main className="app-main">
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
      </main>
    </div>
  );
}

export default App;
