Check the health and status of both production (OCI) and homelab deployments.

1. **OCI Production** (tumblinfamily.com):
   - Pod status: `ssh -i ~/.ssh/oci_family_reunion opc@146.235.195.248 "sudo /usr/local/bin/k3s kubectl get pods -n family-reunion -o wide"`
   - API health: `curl -s -o /dev/null -w '%{http_code}' https://tumblinfamily.com/api/rsvp/summary`
   - Recent events: `ssh -i ~/.ssh/oci_family_reunion opc@146.235.195.248 "sudo /usr/local/bin/k3s kubectl get events -n family-reunion --sort-by='.lastTimestamp' | tail -10"`

2. **Homelab** (reunion.home):
   - Pod status: `ssh derrick@192.168.0.186 "KUBECONFIG=~/.kube/config kubectl get pods -n family-reunion -o wide"`

3. Report a summary table of pod status, restarts, and API health.
