import { useState, useEffect, useRef, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../AuthContext';
import './CommandPalette.css';

interface NavItem {
  label: string;
  path: string;
  admin?: boolean;
}

const ALL_ITEMS: NavItem[] = [
  { label: 'Home', path: '/' },
  { label: 'Pay & RSVP', path: '/pay' },
  { label: 'Events', path: '/events' },
  { label: 'Meetings', path: '/meetings' },
  { label: 'Budget', path: '/budget' },
  { label: 'Members', path: '/members' },
  { label: 'Family Tree', path: '/family-tree' },
  { label: 'Gallery', path: '/gallery' },
  { label: 'RSVP', path: '/rsvp', admin: true },
  { label: 'Check-In', path: '/checkin', admin: true },
  { label: 'Admin', path: '/admin', admin: true },
];

export default function CommandPalette() {
  const [open, setOpen] = useState(false);
  const [query, setQuery] = useState('');
  const [selectedIndex, setSelectedIndex] = useState(0);
  const inputRef = useRef<HTMLInputElement>(null);
  const listRef = useRef<HTMLDivElement>(null);
  const navigate = useNavigate();
  const { isAdmin } = useAuth();

  const items = ALL_ITEMS.filter((item) => {
    if (item.admin && !isAdmin) return false;
    if (!query) return true;
    return item.label.toLowerCase().includes(query.toLowerCase());
  });

  const close = useCallback(() => {
    setOpen(false);
    setQuery('');
    setSelectedIndex(0);
  }, []);

  const go = useCallback(
    (path: string) => {
      navigate(path);
      close();
    },
    [navigate, close],
  );

  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if ((e.metaKey || e.ctrlKey) && e.key === 'k') {
        e.preventDefault();
        setOpen((prev) => !prev);
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, []);

  useEffect(() => {
    if (open) {
      inputRef.current?.focus();
    }
  }, [open]);

  useEffect(() => {
    setSelectedIndex(0);
  }, [query]);

  useEffect(() => {
    if (!open) return;
    const el = listRef.current?.children[selectedIndex] as HTMLElement | undefined;
    el?.scrollIntoView({ block: 'nearest' });
  }, [selectedIndex, open]);

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'ArrowDown') {
      e.preventDefault();
      setSelectedIndex((i) => (i + 1) % items.length);
    } else if (e.key === 'ArrowUp') {
      e.preventDefault();
      setSelectedIndex((i) => (i - 1 + items.length) % items.length);
    } else if (e.key === 'Enter' && items.length > 0) {
      e.preventDefault();
      go(items[selectedIndex].path);
    } else if (e.key === 'Escape') {
      close();
    }
  };

  if (!open) return null;

  return (
    <div className="cmd-palette-overlay" onClick={close}>
      <div className="cmd-palette" onClick={(e) => e.stopPropagation()}>
        <div className="cmd-palette-input-wrap">
          <svg className="cmd-palette-search-icon" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <circle cx="11" cy="11" r="8" />
            <line x1="21" y1="21" x2="16.65" y2="16.65" />
          </svg>
          <input
            ref={inputRef}
            className="cmd-palette-input"
            placeholder="Go to page..."
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            onKeyDown={handleKeyDown}
          />
          <kbd className="cmd-palette-kbd">esc</kbd>
        </div>
        <div className="cmd-palette-list" ref={listRef}>
          {items.length === 0 && (
            <div className="cmd-palette-empty">No results</div>
          )}
          {items.map((item, i) => (
            <button
              key={item.path}
              className={`cmd-palette-item ${i === selectedIndex ? 'selected' : ''} ${item.admin ? 'admin' : ''}`}
              onClick={() => go(item.path)}
              onMouseEnter={() => setSelectedIndex(i)}
            >
              <span className="cmd-palette-item-label">{item.label}</span>
              <span className="cmd-palette-item-path">{item.path}</span>
            </button>
          ))}
        </div>
        <div className="cmd-palette-footer">
          <span><kbd>&uarr;</kbd><kbd>&darr;</kbd> navigate</span>
          <span><kbd>&crarr;</kbd> open</span>
          <span><kbd>esc</kbd> close</span>
        </div>
      </div>
    </div>
  );
}
