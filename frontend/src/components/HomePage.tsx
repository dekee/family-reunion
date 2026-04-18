import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { fetchSummary } from '../api';
import { useScrollFadeIn } from '../hooks/useScrollFadeIn';
import { useCountUp } from '../hooks/useCountUp';
import { useSiteConfig } from '../SiteConfigContext';
import type { RsvpSummaryResponse } from '../types';
import './HomePage.css';

interface TimeLeft {
  months: number;
  days: number;
  hours: number;
  minutes: number;
  seconds: number;
}

function calcTimeLeft(startDate: string): TimeLeft {
  const now = new Date();
  let diff = new Date(startDate).getTime() - now.getTime();
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

function AnimatedStat({ value, label, start }: { value: number; label: string; start: boolean }) {
  const animated = useCountUp(value, 1200, start);
  return (
    <div className="stat-card">
      <span className="stat-number">{animated}</span>
      <span className="stat-label">{label}</span>
    </div>
  );
}

export default function HomePage() {
  const { config: reunionConfig } = useSiteConfig();
  const [timeLeft, setTimeLeft] = useState<TimeLeft>(() => calcTimeLeft(reunionConfig.event.startDate));
  const [summary, setSummary] = useState<RsvpSummaryResponse | null>(null);
  const navigate = useNavigate();

  const mottoFade = useScrollFadeIn();
  const countdownFade = useScrollFadeIn();
  const detailsFade = useScrollFadeIn();
  const statsFade = useScrollFadeIn();
  const ctaFade = useScrollFadeIn();

  useEffect(() => {
    const timer = setInterval(() => setTimeLeft(calcTimeLeft(reunionConfig.event.startDate)), 1000);
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
        <img src={reunionConfig.images.heroPhoto} alt={`${reunionConfig.family.name} Family`} className="hero-family-photo" />
        <p className="hero-eyebrow">You're Invited</p>
        <h1 className="hero-title">The {reunionConfig.family.fullTitle}</h1>
        <p className="hero-subtitle">
          {reunionConfig.family.subtitle}
        </p>
        <p className="hero-date">{reunionConfig.event.dates}</p>
      </section>

      {/* Motto */}
      <div ref={mottoFade.ref} className={`fade-in-section ${mottoFade.isVisible ? 'visible' : ''}`}>
        <section className="motto-section">
          <blockquote className="family-motto">
            {reunionConfig.family.motto.text}
          </blockquote>
          <p className="motto-attribution">{reunionConfig.family.motto.attribution}</p>
        </section>
      </div>

      {/* Countdown */}
      <div ref={countdownFade.ref} className={`fade-in-section ${countdownFade.isVisible ? 'visible' : ''}`}>
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
      </div>

      {/* Event Details + Map */}
      <div ref={detailsFade.ref} className={`fade-in-section ${detailsFade.isVisible ? 'visible' : ''}`}>
        <section className="details-section">
          <div className="details-grid">
            <div className="event-info">
              <h2 className="section-heading">Event Details</h2>
              <div className="info-item">
                <span className="info-icon">&#128197;</span>
                <div>
                  <strong>When</strong>
                  <p>{reunionConfig.event.dates}</p>
                </div>
              </div>
              <div className="info-item">
                <span className="info-icon">&#128205;</span>
                <div>
                  <strong>Where</strong>
                  <p>{reunionConfig.event.address}</p>
                </div>
              </div>
              <div className="info-item">
                <span className="info-icon">&#128336;</span>
                <div>
                  <strong>Time</strong>
                  <p>{reunionConfig.event.time}</p>
                </div>
              </div>
              <div className="info-item">
                <span className="info-icon">&#128106;</span>
                <div>
                  <strong>Who</strong>
                  <p>{reunionConfig.family.whoInvited}</p>
                </div>
              </div>
            </div>

            <div className="map-container">
              <iframe
                title="Reunion Location"
                src={reunionConfig.event.mapEmbedUrl}
                loading="lazy"
                referrerPolicy="no-referrer-when-downgrade"
                allowFullScreen
              />
              <a
                className="get-directions-btn"
                href={reunionConfig.event.directionsUrl}
                target="_blank"
                rel="noopener noreferrer"
              >
                Get Directions
              </a>
            </div>
          </div>
        </section>
      </div>

      {/* RSVP Stats */}
      {summary && (
        <div ref={statsFade.ref} className={`fade-in-section ${statsFade.isVisible ? 'visible' : ''}`}>
          <section className="stats-section">
            <h2 className="section-heading">Who's Coming?</h2>
            <div className="stats-grid">
              <AnimatedStat value={summary.totalFamilies} label="Families" start={statsFade.isVisible} />
              <AnimatedStat value={summary.totalHeadcount} label="Total Attending" start={statsFade.isVisible} />
              <AnimatedStat value={summary.adultCount} label="Adults" start={statsFade.isVisible} />
              <AnimatedStat value={summary.childCount + summary.infantCount} label="Kids" start={statsFade.isVisible} />
              <AnimatedStat value={summary.lodgingCount} label="Need Lodging" start={statsFade.isVisible} />
            </div>
          </section>
        </div>
      )}

      {/* CTA */}
      <div ref={ctaFade.ref} className={`fade-in-section ${ctaFade.isVisible ? 'visible' : ''}`}>
        <section className="cta-section">
          <h2>Ready to Join Us?</h2>
          <p>Let us know your family is coming so we can plan the best reunion yet.</p>
          <button className="cta-button" onClick={() => navigate('/pay')}>
            Pay & RSVP Now
          </button>
        </section>
      </div>
    </div>
  );
}
