import { useState, useEffect, useRef } from 'react';
import { useSiteConfig } from '../SiteConfigContext';
import { updateSiteConfig, uploadSiteImage, deleteSiteImage } from '../api';
import './SiteSettings.css';

interface ImageUploadProps {
  label: string;
  imageKey: string;
  currentUrl: string;
  onUploaded: () => void;
}

function ImageUpload({ label, imageKey, currentUrl, onUploaded }: ImageUploadProps) {
  const [uploading, setUploading] = useState(false);
  const [error, setError] = useState('');
  const fileRef = useRef<HTMLInputElement>(null);

  const handleFile = async (file: File) => {
    if (file.size > 2 * 1024 * 1024) {
      setError('Image must be under 2MB');
      return;
    }
    if (!['image/jpeg', 'image/png', 'image/webp'].includes(file.type)) {
      setError('Only JPEG, PNG, or WebP images allowed');
      return;
    }
    setError('');
    setUploading(true);
    try {
      await uploadSiteImage(imageKey, file);
      onUploaded();
    } catch (err: any) {
      setError(err.message || 'Upload failed');
    } finally {
      setUploading(false);
    }
  };

  const handleRevert = async () => {
    setUploading(true);
    try {
      await deleteSiteImage(imageKey);
      onUploaded();
    } catch (err: any) {
      setError(err.message || 'Revert failed');
    } finally {
      setUploading(false);
    }
  };

  return (
    <div className="image-upload">
      <label className="image-upload-label">{label}</label>
      <div className="image-upload-preview">
        <img src={currentUrl} alt={label} />
      </div>
      <div className="image-upload-actions">
        <input
          ref={fileRef}
          type="file"
          accept="image/jpeg,image/png,image/webp"
          style={{ display: 'none' }}
          onChange={(e) => {
            const file = e.target.files?.[0];
            if (file) handleFile(file);
            e.target.value = '';
          }}
        />
        <button
          className="btn-upload"
          onClick={() => fileRef.current?.click()}
          disabled={uploading}
        >
          {uploading ? 'Uploading...' : 'Upload New'}
        </button>
        {currentUrl.startsWith('/api/') && (
          <button
            className="btn-revert"
            onClick={handleRevert}
            disabled={uploading}
          >
            Revert to Default
          </button>
        )}
      </div>
      {error && <p className="image-upload-error">{error}</p>}
    </div>
  );
}

export default function SiteSettings() {
  const { config, refreshConfig } = useSiteConfig();
  const [saving, setSaving] = useState(false);
  const [message, setMessage] = useState('');

  // Form state
  const [familyName, setFamilyName] = useState('');
  const [fullTitle, setFullTitle] = useState('');
  const [founders, setFounders] = useState('');
  const [established, setEstablished] = useState('');
  const [subtitle, setSubtitle] = useState('');
  const [mottoText, setMottoText] = useState('');
  const [mottoAttribution, setMottoAttribution] = useState('');
  const [whoInvited, setWhoInvited] = useState('');

  const [eventDates, setEventDates] = useState('');
  const [startDate, setStartDate] = useState('');
  const [eventTime, setEventTime] = useState('');
  const [address, setAddress] = useState('');
  const [mapEmbedUrl, setMapEmbedUrl] = useState('');
  const [directionsUrl, setDirectionsUrl] = useState('');

  const [angelGoal, setAngelGoal] = useState('');
  const [primaryColor, setPrimaryColor] = useState('');
  const [accentColor, setAccentColor] = useState('');

  // Populate form from config
  useEffect(() => {
    setFamilyName(config.family.name);
    setFullTitle(config.family.fullTitle);
    setFounders(config.family.founders);
    setEstablished(String(config.family.established));
    setSubtitle(config.family.subtitle);
    setMottoText(config.family.motto.text);
    setMottoAttribution(config.family.motto.attribution);
    setWhoInvited(config.family.whoInvited);
    setEventDates(config.event.dates);
    setStartDate(config.event.startDate);
    setEventTime(config.event.time);
    setAddress(config.event.address);
    setMapEmbedUrl(config.event.mapEmbedUrl);
    setDirectionsUrl(config.event.directionsUrl);
    setAngelGoal(String(config.angelGoal));
    setPrimaryColor(config.theme.primaryColor);
    setAccentColor(config.theme.accentColor);
  }, [config]);

  const handleSave = async () => {
    setSaving(true);
    setMessage('');
    try {
      await updateSiteConfig({
        'family.name': familyName,
        'family.fullTitle': fullTitle,
        'family.founders': founders,
        'family.established': established,
        'family.subtitle': subtitle,
        'family.motto.text': mottoText,
        'family.motto.attribution': mottoAttribution,
        'family.whoInvited': whoInvited,
        'event.dates': eventDates,
        'event.startDate': startDate,
        'event.time': eventTime,
        'event.address': address,
        'event.mapEmbedUrl': mapEmbedUrl,
        'event.directionsUrl': directionsUrl,
        'angelGoal': angelGoal,
        'theme.primaryColor': primaryColor,
        'theme.accentColor': accentColor,
      });
      await refreshConfig();
      setMessage('Settings saved!');
      setTimeout(() => setMessage(''), 3000);
    } catch (err: any) {
      setMessage(err.message || 'Failed to save');
    } finally {
      setSaving(false);
    }
  };

  const handleImageUploaded = async () => {
    await refreshConfig();
    setMessage('Image updated!');
    setTimeout(() => setMessage(''), 3000);
  };

  return (
    <div className="site-settings">
      <div className="page-header">
        <h2>Site Settings</h2>
        <p>Configure your reunion website appearance and details</p>
      </div>

      {message && (
        <div className={`settings-message ${message.includes('Failed') ? 'error' : 'success'}`}>
          {message}
        </div>
      )}

      {/* Family Identity */}
      <section className="settings-section">
        <h3>Family Identity</h3>
        <div className="settings-grid">
          <div className="field">
            <label>Family Name</label>
            <input value={familyName} onChange={(e) => setFamilyName(e.target.value)} placeholder="Smith" />
          </div>
          <div className="field">
            <label>Full Title</label>
            <input value={fullTitle} onChange={(e) => setFullTitle(e.target.value)} placeholder="Smith Family Reunion" />
          </div>
          <div className="field">
            <label>Founders</label>
            <input value={founders} onChange={(e) => setFounders(e.target.value)} placeholder="John & Mary Smith" />
          </div>
          <div className="field">
            <label>Established Year</label>
            <input type="number" value={established} onChange={(e) => setEstablished(e.target.value)} placeholder="1960" />
          </div>
          <div className="field full-width">
            <label>Subtitle</label>
            <input value={subtitle} onChange={(e) => setSubtitle(e.target.value)} placeholder="Celebrating the legacy of..." />
          </div>
          <div className="field full-width">
            <label>Motto / Quote</label>
            <input value={mottoText} onChange={(e) => setMottoText(e.target.value)} placeholder="Family is everything..." />
          </div>
          <div className="field">
            <label>Motto Attribution</label>
            <input value={mottoAttribution} onChange={(e) => setMottoAttribution(e.target.value)} placeholder="— Author" />
          </div>
          <div className="field">
            <label>Who's Invited</label>
            <input value={whoInvited} onChange={(e) => setWhoInvited(e.target.value)} placeholder="All descendants of..." />
          </div>
        </div>
      </section>

      {/* Event Details */}
      <section className="settings-section">
        <h3>Event Details</h3>
        <div className="settings-grid">
          <div className="field">
            <label>Display Dates</label>
            <input value={eventDates} onChange={(e) => setEventDates(e.target.value)} placeholder="July 4 - 6, 2026" />
          </div>
          <div className="field">
            <label>Start Date (for countdown)</label>
            <input type="datetime-local" value={startDate} onChange={(e) => setStartDate(e.target.value)} />
          </div>
          <div className="field">
            <label>Time</label>
            <input value={eventTime} onChange={(e) => setEventTime(e.target.value)} placeholder="Saturday 10 AM - Monday" />
          </div>
          <div className="field">
            <label>Address</label>
            <input value={address} onChange={(e) => setAddress(e.target.value)} placeholder="123 Main St, City, ST" />
          </div>
          <div className="field full-width">
            <label>Google Maps Embed URL</label>
            <input value={mapEmbedUrl} onChange={(e) => setMapEmbedUrl(e.target.value)} placeholder="https://maps.google.com/maps?q=..." />
          </div>
          <div className="field full-width">
            <label>Directions URL</label>
            <input value={directionsUrl} onChange={(e) => setDirectionsUrl(e.target.value)} placeholder="https://www.google.com/maps/dir/..." />
          </div>
        </div>
      </section>

      {/* Fundraising */}
      <section className="settings-section">
        <h3>Fundraising</h3>
        <div className="settings-grid">
          <div className="field">
            <label>Angel Contributor Goal ($)</label>
            <input type="number" value={angelGoal} onChange={(e) => setAngelGoal(e.target.value)} placeholder="2000" />
          </div>
        </div>
      </section>

      {/* Theme Colors */}
      <section className="settings-section">
        <h3>Theme Colors</h3>
        <div className="settings-grid">
          <div className="field color-field">
            <label>Primary Color</label>
            <div className="color-picker-row">
              <input type="color" value={primaryColor} onChange={(e) => setPrimaryColor(e.target.value)} />
              <span className="color-hex">{primaryColor}</span>
            </div>
          </div>
          <div className="field color-field">
            <label>Accent Color</label>
            <div className="color-picker-row">
              <input type="color" value={accentColor} onChange={(e) => setAccentColor(e.target.value)} />
              <span className="color-hex">{accentColor}</span>
            </div>
          </div>
        </div>
        <div className="color-preview">
          <div className="color-swatch" style={{ background: primaryColor }}>Primary</div>
          <div className="color-swatch" style={{ background: accentColor, color: '#1a1a2e' }}>Accent</div>
        </div>
      </section>

      <div className="settings-save">
        <button className="btn-save" onClick={handleSave} disabled={saving}>
          {saving ? 'Saving...' : 'Save All Settings'}
        </button>
      </div>

      {/* Images */}
      <section className="settings-section">
        <h3>Images</h3>
        <div className="image-upload-grid">
          <ImageUpload
            label="Hero Photo"
            imageKey="heroPhoto"
            currentUrl={config.images.heroPhoto}
            onUploaded={handleImageUploaded}
          />
          <ImageUpload
            label="Founder Photo"
            imageKey="founderPhoto"
            currentUrl={config.images.founderPhoto}
            onUploaded={handleImageUploaded}
          />
          <ImageUpload
            label="Angel Background"
            imageKey="angelBackground"
            currentUrl={config.images.angelBackground}
            onUploaded={handleImageUploaded}
          />
        </div>
      </section>
    </div>
  );
}
