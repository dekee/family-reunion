import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { fetchSummary } from '../api';
import type { RsvpSummaryResponse } from '../types';
import './HomePage.css';

const REUNION_START = new Date('2026-10-16T10:00:00');

interface TimeLeft {
  months: number;
  days: number;
  hours: number;
  minutes: number;
  seconds: number;
}

function calcTimeLeft(): TimeLeft {
  const now = new Date();
  let diff = REUNION_START.getTime() - now.getTime();
  if (diff < 0) diff = 0;

  const totalSeconds = Math.floor(diff / 1000);
  const totalMinutes = Math.floor(totalSeconds / 60);
  const totalHours = Math.floor(totalMinutes / 60);
  const totalDays = Math.floor(totalHours / 24);

  // approximate months (30.44 avg days/month)
  const months = Math.floor(totalDays / 30.44);
  const days = totalDays - Math.round(months * 30.44);

  return {
    months,
    days,
    hours: totalHours % 24,
    minutes: totalMinutes % 60,
    seconds: totalSeconds % 60,
  };
}

export default function HomePage() {
  const [timeLeft, setTimeLeft] = useState<TimeLeft>(calcTimeLeft);
  const [summary, setSummary] = useState<RsvpSummaryResponse | null>(null);
  const navigate = useNavigate();

  useEffect(() => {
    const timer = setInterval(() => setTimeLeft(calcTimeLeft()), 1000);
    return () => clearInterval(timer);
  }, []);

  useEffect(() => {
    fetchSummary().then(setSummary).catch(console.error);
  }, []);

  const isOver = timeLeft.months === 0 && timeLeft.days === 0 && timeLeft.hours === 0
    && timeLeft.minutes === 0 && timeLeft.seconds === 0;

  return (
    <div className="home">
      {/* Hero */}
      <section className="hero">
        <p className="hero-eyebrow">You're Invited</p>
        <h1 className="hero-title">The Tumblin Family Reunion</h1>
        <p className="hero-subtitle">
          Celebrating the legacy of Wesley & Esther Tumblin — Est. 1948
        </p>
        <p className="hero-date">October 16 – 18, 2026</p>
      </section>

      {/* Countdown */}
      <section className="countdown-section">
        <h2 className="section-heading">
          {isOver ? 'The Reunion Has Begun!' : 'Countdown to the Reunion'}
        </h2>
        {!isOver && (
          <div className="countdown">
            <div className="countdown-unit">
              <span className="countdown-number">{timeLeft.months}</span>
              <span className="countdown-label">Months</span>
            </div>
            <div className="countdown-unit">
              <span className="countdown-number">{timeLeft.days}</span>
              <span className="countdown-label">Days</span>
            </div>
            <div className="countdown-unit">
              <span className="countdown-number">{String(timeLeft.hours).padStart(2, '0')}</span>
              <span className="countdown-label">Hours</span>
            </div>
            <div className="countdown-unit">
              <span className="countdown-number">{String(timeLeft.minutes).padStart(2, '0')}</span>
              <span className="countdown-label">Minutes</span>
            </div>
            <div className="countdown-unit">
              <span className="countdown-number">{String(timeLeft.seconds).padStart(2, '0')}</span>
              <span className="countdown-label">Seconds</span>
            </div>
          </div>
        )}
      </section>

      {/* Event Details + Map */}
      <section className="details-section">
        <div className="details-grid">
          <div className="event-info">
            <h2 className="section-heading">Event Details</h2>
            <div className="info-item">
              <span className="info-icon">&#128197;</span>
              <div>
                <strong>When</strong>
                <p>October 16 – 18, 2026</p>
              </div>
            </div>
            <div className="info-item">
              <span className="info-icon">&#128205;</span>
              <div>
                <strong>Where</strong>
                <p>Location TBD</p>
                <p className="info-sub">Details coming soon</p>
              </div>
            </div>
            <div className="info-item">
              <span className="info-icon">&#128336;</span>
              <div>
                <strong>Time</strong>
                <p>Friday 10:00 AM – Sunday Evening</p>
              </div>
            </div>
            <div className="info-item">
              <span className="info-icon">&#128106;</span>
              <div>
                <strong>Who</strong>
                <p>All descendants of Wesley & Esther Tumblin, plus guests</p>
              </div>
            </div>
          </div>

          <div className="map-container">
            <div className="map-placeholder">
              <span className="map-pin">&#128205;</span>
              <p>Venue map will appear here once the location is finalized.</p>
            </div>
          </div>
        </div>
      </section>

      {/* RSVP Stats */}
      {summary && (
        <section className="stats-section">
          <h2 className="section-heading">Who's Coming?</h2>
          <div className="stats-grid">
            <div className="stat-card">
              <span className="stat-number">{summary.totalFamilies}</span>
              <span className="stat-label">Families</span>
            </div>
            <div className="stat-card">
              <span className="stat-number">{summary.totalHeadcount}</span>
              <span className="stat-label">Total Attending</span>
            </div>
            <div className="stat-card">
              <span className="stat-number">{summary.adultCount}</span>
              <span className="stat-label">Adults</span>
            </div>
            <div className="stat-card">
              <span className="stat-number">{summary.childCount + summary.infantCount}</span>
              <span className="stat-label">Kids</span>
            </div>
            <div className="stat-card">
              <span className="stat-number">{summary.lodgingCount}</span>
              <span className="stat-label">Need Lodging</span>
            </div>
          </div>
        </section>
      )}

      {/* CTA */}
      <section className="cta-section">
        <h2>Ready to Join Us?</h2>
        <p>Let us know your family is coming so we can plan the best reunion yet.</p>
        <button className="cta-button" onClick={() => navigate('/rsvp')}>
          RSVP Now
        </button>
      </section>
    </div>
  );
}
