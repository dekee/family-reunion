Launch a multi-agent review of the website. Spawn 4 background agents in parallel:

1. **UX Agent** — Review navigation, payment flow, ticket experience, mobile responsiveness, accessibility (aria labels, contrast, keyboard nav), loading/error states, dark mode
2. **Tech Architecture Agent** — Review API design, data models, server-side validation, frontend-backend contract, database strategy, K8s deployment, performance (N+1 queries, caching), error handling
3. **Devil's Advocate Agent** — Challenge assumptions, find edge cases, test what happens with concurrent payments, empty data, brute-force tokens, missing auth, data persistence, disaster recovery
4. **Security Agent** — Audit authentication/authorization, data exposure, input validation, payment security, secret management, CORS/CSP/CSRF, token entropy, OWASP Top 10

Each agent should:
- Use WebFetch to probe the live site at https://tumblinfamily.com
- Read relevant source files (controllers, services, configs, frontend components)
- Provide a structured report with severity-ranked findings and recommendations

After all agents complete, compile a unified report with cross-referenced findings sorted by severity.
