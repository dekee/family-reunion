Build and deploy to production (Oracle Cloud). Steps:

1. Run backend tests: `cd /Users/derrick/Development/family_reunion && ./gradlew test`
2. Run frontend tests: `cd /Users/derrick/Development/family_reunion/frontend && npx vitest run`
3. If tests pass, build and push both images in parallel:
   - Backend: `cd /Users/derrick/Development/family_reunion && docker buildx build --builder builder --platform linux/amd64,linux/arm64 -t ghcr.io/dekee/family-reunion-backend:latest --push .`
   - Frontend: `cd /Users/derrick/Development/family_reunion/frontend && docker buildx build --builder builder --platform linux/amd64,linux/arm64 -t ghcr.io/dekee/family-reunion-frontend:latest --push .`
4. Restart deployments on OCI:
   - `ssh -i ~/.ssh/oci_family_reunion opc@146.235.195.248 "sudo /usr/local/bin/k3s kubectl rollout restart deployment/backend -n family-reunion"`
   - `ssh -i ~/.ssh/oci_family_reunion opc@146.235.195.248 "sudo /usr/local/bin/k3s kubectl rollout restart deployment/frontend -n family-reunion"`
5. Wait for rollout: `ssh -i ~/.ssh/oci_family_reunion opc@146.235.195.248 "sudo /usr/local/bin/k3s kubectl rollout status deployment/backend deployment/frontend -n family-reunion --timeout=120s"`
6. Smoke test: `curl -s https://tumblinfamily.com/api/rsvp/summary | head -c 200`
7. Report result to user.
