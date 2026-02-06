#!/bin/bash
# =============================================================================
# Oracle Cloud VM Setup Script for Family Reunion App
# Run this on the Oracle Ampere A1 VM after SSH'ing in
# =============================================================================
set -euo pipefail

echo "=== Step 1: Install K3s (with Traefik disabled) ==="
curl -sfL https://get.k3s.io | INSTALL_K3S_EXEC="--disable traefik" sh -
# Make kubectl accessible without sudo
mkdir -p ~/.kube
sudo cp /etc/rancher/k3s/k3s.yaml ~/.kube/config
sudo chown "$(id -u):$(id -g)" ~/.kube/config
export KUBECONFIG=~/.kube/config
echo 'export KUBECONFIG=~/.kube/config' >> ~/.bashrc

echo ""
echo "=== Verifying K3s ==="
kubectl get nodes
echo ""

echo "=== Step 2: Install Helm ==="
curl https://raw.githubusercontent.com/helm/helm/main/scripts/get-helm-3 | bash
echo ""

echo "=== Step 3: Install nginx Ingress Controller ==="
helm repo add ingress-nginx https://kubernetes.github.io/ingress-nginx
helm repo update
helm install ingress-nginx ingress-nginx/ingress-nginx \
  --namespace ingress-nginx --create-namespace \
  --set controller.service.type=NodePort \
  --set controller.service.nodePorts.http=80 \
  --set controller.service.nodePorts.https=443 \
  --set controller.hostNetwork=true
echo ""

echo "=== Step 4: Install ArgoCD ==="
kubectl create namespace argocd
kubectl apply -n argocd -f https://raw.githubusercontent.com/argoproj/argo-cd/stable/manifests/install.yaml
echo ""
echo "Waiting for ArgoCD to be ready..."
kubectl wait --for=condition=available deployment/argocd-server -n argocd --timeout=300s
echo ""

ARGOCD_PASSWORD=$(kubectl -n argocd get secret argocd-initial-admin-secret -o jsonpath="{.data.password}" | base64 -d)
echo "=== ArgoCD Initial Admin Password ==="
echo "  Username: admin"
echo "  Password: $ARGOCD_PASSWORD"
echo ""

echo "=== Step 5: Deploy ArgoCD Application ==="
kubectl apply -f https://raw.githubusercontent.com/dekee/family-reunion/HEAD/k8s/argocd-app.yaml
echo ""

echo "=== Setup Complete ==="
echo ""
echo "Next steps:"
echo "  1. ArgoCD will auto-sync and deploy the app from GitHub"
echo "  2. Check pods:  kubectl get pods -n family-reunion"
echo "  3. Check ingress: kubectl get ingress -n family-reunion"
echo "  4. Access the app: http://<YOUR_PUBLIC_IP>"
echo ""
echo "  To access ArgoCD UI (port-forward):"
echo "    kubectl port-forward svc/argocd-server -n argocd 8443:443 --address 0.0.0.0"
echo "    Then visit: https://<YOUR_PUBLIC_IP>:8443"
