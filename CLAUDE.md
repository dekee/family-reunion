# Family Reunion Project — Claude Instructions

## Build & Test
- **Backend build**: `./gradlew build -x test` (from project root)
- **Backend test**: `./gradlew test` (102 tests across 5 files)
- **Frontend build**: `cd frontend && npm run build`
- **Frontend test**: `cd frontend && npx vitest run` (40 API contract tests)
- **Always run tests** after modifying backend security rules or API contracts

## Deployment
- **Build images**: `docker buildx build --builder builder --platform linux/amd64,linux/arm64 -t ghcr.io/dekee/family-reunion-<backend|frontend>:latest --push .`
- **OCI deploy**: `ssh -i ~/.ssh/oci_family_reunion opc@146.235.195.248 "sudo /usr/local/bin/k3s kubectl rollout restart deployment/<backend|frontend> -n family-reunion"`
- **Homelab**: ArgoCD auto-syncs from `k8s/` directory; manual: `ssh derrick@192.168.0.186 "KUBECONFIG=~/.kube/config kubectl rollout restart deployment/<name> -n family-reunion"`
- Backend Dockerfile is in project root, frontend Dockerfile is in `frontend/`

## Security Rules
- **SecurityConfig.kt** uses first-match-wins ordering. ALWAYS put specific admin rules BEFORE any broad catch-all.
- Public payment summaries must NOT include `payerEmail` — strip PII from public-facing DTOs.
- `checkinToken` is acceptable in per-RSVP payment summary (needed for ticket linking).
- `/api/checkin/send` is rate-limited (3 per token per 10 min) — do not remove this.
- RSVP creation (POST) is public; RSVP editing (PUT) requires admin auth.

## Secrets
- Use **SealedSecrets** — never commit plaintext secrets to git.
- Seal with cluster-specific certs: `kubeseal --format yaml --cert <cert.pem> < secret.yaml > sealed-secret.yaml`
- Homelab sealed secrets go in `k8s/sealed-*-secret.yaml`
- OCI sealed secrets go in `k8s/oci/sealed-*-secret.yaml`
- After applying a new SealedSecret, delete the old K8s secret first if it exists.

## Conventions
- Backend: Kotlin, Spring Boot. JPA entities are regular classes; DTOs are data classes.
- Frontend: React + TypeScript. API functions in `src/api.ts`, types in `src/types.ts`.
- Auth-required frontend API calls use `authHeaders()`. Public calls use plain `{ 'Content-Type': 'application/json' }`.
- CSS: Component-specific `.css` files, design tokens as CSS custom properties, dark mode support.
- Test profiles: backend tests run without Spring profiles (H2 in-memory); DataInitializer uses `@Profile("dev")`.
