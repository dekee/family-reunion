# Family Reunion Website — Setup Guide

This is a full-featured family reunion RSVP and event management website. Fork this repo and customize it for your own family reunion.

## Quick Start (5 minutes)

1. **Fork this repository** on GitHub
2. **Edit `frontend/src/reunion.config.ts`** — update family name, event dates, address, motto, and colors. See `reunion.config.example.ts` for a clean template.
3. **Replace images** in `frontend/public/`:
   - `FamilyFirst.jpg` — hero photo shown on homepage
   - `founders.jpg` — photo of family founders (shown on family tree root node)
   - `angel-contributor.png` — background for the angel contributor section
4. **Set up Google OAuth** (see below) and update `frontend/.env`
5. **Run locally**:
   ```bash
   # Backend (runs on port 8080)
   ./gradlew bootRun --args='--spring.profiles.active=dev'

   # Frontend (runs on port 5173, proxies API to 8080)
   cd frontend && npm install && npm run dev
   ```

## Configuration Reference

### Frontend Config (`frontend/src/reunion.config.ts`)

This single file controls all family-specific content in the UI:

| Section | Fields | Description |
|---------|--------|-------------|
| `family.name` | `'Smith'` | Surname used throughout the site |
| `family.fullTitle` | `'Smith Family Reunion'` | Shown in header, tickets, emails |
| `family.founders` | `'John & Mary Smith'` | Displayed on family tree root |
| `family.established` | `1960` | Year the family line started |
| `family.subtitle` | `'Celebrating...'` | Hero section subtitle |
| `family.motto` | `{ text, attribution }` | Family motto or scripture |
| `family.whoInvited` | `'All descendants...'` | Shown in event details |
| `event.dates` | `'July 4 – 6, 2026'` | Display string for event dates |
| `event.startDate` | `'2026-07-04T10:00:00'` | ISO date for countdown timer |
| `event.time` | `'Saturday 10 AM...'` | Time range display |
| `event.address` | `'123 Main St...'` | Venue address |
| `event.mapEmbedUrl` | Google Maps embed URL | Embedded map on homepage |
| `event.directionsUrl` | Google Maps directions URL | "Get Directions" link |
| `angelGoal` | `1000` | Fundraising goal in dollars |
| `googleAnalyticsId` | `'G-XXXXX'` or `''` | GA4 ID (empty to disable) |
| `images` | `{ heroPhoto, founderPhoto, angelBackground }` | Paths to images in `/public/` |
| `theme.primaryColor` | `'#1a4d2e'` | Primary brand color (headers, nav) |
| `theme.accentColor` | `'#d4a017'` | Accent color (buttons, highlights) |
| `branchPalette` | `{ light: [...], dark: [...] }` | Auto-assigned branch colors |
| `generationColors` | `['#c9a84c', ...]` | Family tree generation colors |
| `generationLabels` | `['Founders', 'Gen 1', ...]` | Legend labels for tree |

### Backend Config (`application.properties`)

| Property | Env Var | Description |
|----------|---------|-------------|
| `app.reunion.family-name` | `REUNION_FAMILY_NAME` | Family name for emails/payments |
| `app.reunion.full-title` | `REUNION_FULL_TITLE` | Full title for emails/Stripe |
| `app.reunion.cors-origins` | `CORS_ORIGINS` | Comma-separated allowed origins |
| `app.base-url` | `APP_BASE_URL` | Public URL of the app |
| `app.fees.adult` | `FEE_ADULT` | Adult fee in cents (default: 10000) |
| `app.fees.spouse` | `FEE_SPOUSE` | Spouse fee in cents (default: 10000) |
| `app.fees.child` | `FEE_CHILD` | Child fee in cents (default: 5000) |
| `app.fees.infant` | `FEE_INFANT` | Infant fee in cents (default: 1500) |

## Family Data

You have two options for populating your family tree:

### Option A: Admin UI (Recommended)
1. Start the app and sign in with Google
2. Add yourself as an admin (first admin is set via `INITIAL_ADMIN_EMAIL` env var)
3. Go to Members page and add family members through the UI

### Option B: Custom Data Initializer
1. Edit `src/main/kotlin/.../config/SampleDataInitializer.kt`
2. Replace the example "Smith" family with your family's structure
3. Run with `--spring.profiles.active=dev` to seed the data
4. The initializer only runs once (skips if members already exist)

## Third-Party Services

### Google OAuth (Required)
1. Go to [Google Cloud Console](https://console.cloud.google.com/apis/credentials)
2. Create a new OAuth 2.0 Client ID (Web application)
3. Add authorized redirect URIs: `http://localhost:5173` (dev) and your production domain
4. Copy the Client ID to `frontend/.env`:
   ```
   VITE_GOOGLE_CLIENT_ID=your-client-id.apps.googleusercontent.com
   ```
5. Set `GOOGLE_CLIENT_ID` env var for the backend

### Stripe Payments (Required for payment features)
1. Create a [Stripe account](https://stripe.com)
2. Get your API keys from the Stripe Dashboard
3. Create a webhook endpoint: `https://yourdomain.com/api/payments/webhook`
4. Set env vars: `STRIPE_SECRET_KEY`, `STRIPE_WEBHOOK_SECRET`

### Gmail SMTP (Optional — for email notifications)
1. Enable 2-Step Verification on your Gmail account
2. Create an App Password at: Google Account > Security > App Passwords
3. Set env vars: `GMAIL_USERNAME`, `GMAIL_APP_PASSWORD`

### Twilio SMS (Optional — for text notifications)
1. Create a [Twilio account](https://www.twilio.com)
2. Get a phone number
3. Set env vars: `TWILIO_ACCOUNT_SID`, `TWILIO_AUTH_TOKEN`, `TWILIO_FROM_NUMBER`

### Google Drive Gallery (Optional — for photo sharing)
1. Create a Google Cloud Service Account
2. Share a Google Drive folder with the service account email
3. Download the credentials JSON file
4. Set env vars: `GOOGLE_DRIVE_FOLDER_ID`, `GOOGLE_DRIVE_CREDENTIALS_FILE` (path to JSON)

### Google Analytics (Optional)
1. Create a GA4 property at [analytics.google.com](https://analytics.google.com)
2. Get the Measurement ID (e.g., `G-XXXXXXX`)
3. Set it in `reunion.config.ts` → `googleAnalyticsId`

## Deployment

### Docker Build

Build multi-architecture images (amd64 + arm64):

```bash
# Backend (from project root)
docker buildx build --builder builder --platform linux/amd64,linux/arm64 \
  -t ghcr.io/YOUR_USER/family-reunion-backend:latest --push .

# Frontend (from frontend/)
cd frontend
docker buildx build --builder builder --platform linux/amd64,linux/arm64 \
  -t ghcr.io/YOUR_USER/family-reunion-frontend:latest --push .
```

### Kubernetes

1. Update `k8s/ingress.yaml` — replace `tumblinfamily.com` with your domain
2. Update `k8s/backend-deployment.yaml` — set `APP_BASE_URL` to your domain
3. Update `frontend/nginx.conf` — replace the www redirect domain
4. Create secrets for your cluster (see Secrets section below)
5. Apply: `kubectl apply -f k8s/`

### Secrets

The app uses 5 Kubernetes secrets. For each:

1. Create a plaintext secret YAML (do NOT commit to git)
2. Seal it with your cluster's SealedSecrets controller:
   ```bash
   kubeseal --format yaml --cert <your-cluster-cert.pem> < secret.yaml > sealed-secret.yaml
   ```
3. Commit the sealed version, apply it to the cluster

**Required secrets:**
- `postgres-secret` — `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`
- `google-oauth-secret` — `GOOGLE_CLIENT_ID`, `INITIAL_ADMIN_EMAIL`, `INITIAL_ADMIN_NAME`
- `stripe-secret` — `STRIPE_SECRET_KEY`, `STRIPE_WEBHOOK_SECRET`

**Optional secrets:**
- `notification-secret` — Gmail + Twilio credentials
- `gallery-secret` — Google Drive folder ID + credentials file

## Tech Stack

- **Backend**: Kotlin + Spring Boot 3 + Spring Data JPA + PostgreSQL (prod) / H2 (dev)
- **Frontend**: React 18 + TypeScript + Vite
- **Testing**: JUnit 5 + MockMvc (backend), Vitest (frontend)
- **Deployment**: Docker + Kubernetes + SealedSecrets

## Running Tests

```bash
# Backend (102 tests)
./gradlew test

# Frontend (40 API contract tests)
cd frontend && npx vitest run
```
