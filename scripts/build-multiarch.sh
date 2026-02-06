#!/bin/bash
# =============================================================================
# Build & push multi-arch Docker images (amd64 + arm64)
# Run from the project root: ./scripts/build-multiarch.sh
# Requires: docker buildx (comes with Docker Desktop)
# =============================================================================
set -euo pipefail

REPO="dekee/family-reunion"
TAG="${1:-latest}"

# Ensure buildx builder exists
if ! docker buildx inspect multiarch-builder &>/dev/null; then
  echo "Creating buildx builder..."
  docker buildx create --name multiarch-builder --use
else
  docker buildx use multiarch-builder
fi

echo "=== Building backend (linux/amd64 + linux/arm64) ==="
docker buildx build --platform linux/amd64,linux/arm64 \
  -t "${REPO}-backend:${TAG}" \
  --push \
  .

echo ""
echo "=== Building frontend (linux/amd64 + linux/arm64) ==="
docker buildx build --platform linux/amd64,linux/arm64 \
  -t "${REPO}-frontend:${TAG}" \
  --push \
  ./frontend

echo ""
echo "=== Done! Images pushed: ==="
echo "  ${REPO}-backend:${TAG}"
echo "  ${REPO}-frontend:${TAG}"
