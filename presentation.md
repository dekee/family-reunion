# Building a Family Reunion App with Claude Code
### How AI-Assisted Development Turned a Side Project into a Full-Stack Production App

---

## The Problem

Every year, planning the Tumblin family reunion meant spreadsheets, group texts, and a lot of "who's coming?" Our family — descendants of Wesley & Esther Tumblin, married in 1948 — has grown to 111 members across 11 branches. Coordinating RSVPs, events, meetings, lodging, and payments was chaos.

I wanted a centralized app. I also wanted to see how far I could push AI-assisted development using **Claude Code** as my primary development partner.

---

## What We Built

**tumblinfamily.com** — a full-stack RSVP and reunion planning platform.

### Features
- **RSVP System** — Families submit RSVPs with attendee details (adults, children, spouses, infants)
- **Interactive Family Tree** — Visual tree powered by react-d3-tree with zoom, pan, expand/collapse, and auto-fit
- **Event Management** — Create events with dates, locations, and member registration
- **Meeting Tracker** — Schedule and track family meetings with agendas
- **Budget & Payments** — Per-family fee calculation ($100/adult, $50/child, infants free) with Stripe checkout integration
- **Admin Authentication** — Google OAuth sign-in with role-based access control
- **Family Members Directory** — Searchable, branch-organized member list with inline add/edit/delete
- **Dark Mode** — Full theme toggle with localStorage persistence
- **Mobile Responsive** — Hamburger nav, responsive grids, touch-friendly across all pages
- **iOS App Support** — Backend configured for both web and iOS Google OAuth audiences

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| **Backend** | Kotlin + Spring Boot 3.4 + Spring Data JPA |
| **Frontend** | React 18 + TypeScript + Vite |
| **Database** | H2 (dev) / PostgreSQL (prod) |
| **Auth** | Google OAuth 2.0 + Spring Security |
| **Payments** | Stripe Checkout |
| **Infrastructure** | Oracle Cloud E5.Flex VM, K3s (lightweight Kubernetes), nginx Ingress |
| **TLS** | Let's Encrypt via cert-manager |
| **CI/CD** | Docker multi-arch builds (amd64 + arm64), kubectl deploy |
| **Testing** | JUnit 5 + MockMvc (backend), Vitest (frontend) |

---

## How Claude Code Was Used

### 1. Architecture & Scaffolding
Claude Code helped design the initial project structure — Kotlin Spring Boot backend with REST APIs, React TypeScript frontend with Vite. It set up the JPA entities, repositories, services, and controllers following Spring conventions, and scaffolded the React component hierarchy.

### 2. Test-Driven Development
We wrote tests first. Claude Code generated integration tests using MockMvc that validated JSON response shapes, HTTP status codes, and business logic. The backend grew to **102 tests** across 8 test files. On the frontend, Claude Code set up Vitest and created **40 API contract tests** that validate TypeScript types stay in sync with backend DTOs.

### 3. Family Tree Data Population
I had a family tree image with 111 members across 11 branches. Claude Code read the image, extracted the names and relationships, and generated a `DataInitializer.kt` that pre-populates the entire family tree on startup — including generational relationships and age group classifications.

### 4. Feature Development — Conversational Iteration
Features were built conversationally. I'd describe what I wanted, Claude Code would implement it, and I'd test and refine. Examples:

- *"Add a budget page that shows how much each family owes"* — Claude Code built the fee calculation engine, summary API, and React Budget component
- *"The events page has too many checkboxes inline, use a modal"* — Claude Code refactored inline registration into a searchable modal with branch-grouped checkboxes
- *"Add dark mode"* — Claude Code extracted all colors to CSS custom properties, added `[data-theme="dark"]` overrides, and wired up a localStorage-persisted toggle

### 5. Bug Investigation & Fixing
When something broke in production, Claude Code was invaluable for debugging:

- **Delete button failing** — Claude Code SSH'd into the server, read the K8s pod logs, identified FK constraint violations between `family_members`, `attendees`, and `event_registrations` tables, then wrote the fix to properly cascade deletions
- **Google Sign-In broken** — Claude Code opened the live site in Chrome via browser automation, inspected the JS bundle, discovered the `VITE_GOOGLE_CLIENT_ID` build arg wasn't being passed during Docker builds, and rebuilt with the correct configuration
- **Stripe Pay button returning 503** — Claude Code traced the full request chain: frontend button click, backend checkout session creation (working), Stripe redirect (503). Identified it as a Stripe-side issue, not a code bug.

### 6. Cloud Deployment
Claude Code managed the entire deployment pipeline:

- Built multi-arch Docker images (`docker buildx` for amd64 + arm64)
- Pushed to Docker Hub
- SSH'd into the Oracle Cloud VM
- Applied Kubernetes manifests (deployments, services, ingress, secrets)
- Configured TLS with Let's Encrypt + cert-manager
- Managed environment variables and K8s secrets
- Performed rolling deployments and verified health

### 7. Database Operations
Claude Code performed production database operations when needed:

- Reset payment records to show correct "UNPAID" status
- Cleaned up test data after debugging sessions
- Ran pg_dump backups from inside K8s pods

### 8. Design Overhaul
A 10-phase frontend redesign was planned and executed with Claude Code:

- CSS variable system with 50+ custom properties
- Card shadows and hover animations
- Branch-specific color accents for the 11 family branches
- Scroll-triggered fade-in animations and stat counter animations
- Mobile-first hamburger navigation
- Print-friendly RSVP layout
- All without adding a single new npm dependency

---

## By the Numbers

| Metric | Count |
|--------|-------|
| Backend tests | 102 |
| Frontend contract tests | 40 |
| API endpoints | ~25 |
| React components | 15+ |
| CSS files | 12 |
| Family members pre-loaded | 111 |
| Family branches | 11 |
| New npm dependencies added for design overhaul | 0 |
| Cloud provider | Oracle Cloud (free tier + E5.Flex) |

---

## Key Takeaways

1. **Claude Code is a force multiplier** — What would have taken weeks of evenings and weekends was built iteratively in focused sessions. The breadth of the stack (Kotlin, TypeScript, CSS, Kubernetes, Docker, Stripe, Google OAuth) would normally require context-switching between docs. Claude Code held context across all of it.

2. **Conversational development works** — Describing features in plain English and iterating on the result felt natural. "The delete button doesn't work" led to log analysis, root cause identification, and a deployed fix in one conversation.

3. **Full-stack means full-stack** — Claude Code didn't just write application code. It managed infrastructure (K8s manifests, TLS certs, firewall rules), operated databases (SQL queries, backups), debugged production issues (SSH + kubectl + log analysis), and even automated browser testing.

4. **Tests as a safety net** — Having Claude Code write tests alongside features meant we could refactor aggressively. When the delete fix changed a service constructor, the test suite immediately caught it — and Claude Code fixed the tests in the same session.

5. **Ship it** — The app is live at **tumblinfamily.com**, the family is using it, and an iOS app is in progress. It's a real product solving a real problem, built primarily through human-AI collaboration.

---

## Demo

**Live site:** https://tumblinfamily.com

---

*Built by Derrick Johnson with Claude Code*
*Tumblin Family Reunion — October 16-18, 2026*
