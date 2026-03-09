Seal a Kubernetes secret for both clusters (homelab + OCI). Usage: /seal-secret <secret-name>

Steps:
1. Fetch the sealed-secrets controller public certs from both clusters (if not cached in /tmp):
   - Homelab: `ssh derrick@192.168.0.186 "KUBECONFIG=~/.kube/config kubeseal --fetch-cert" > /tmp/homelab-sealed-secrets-cert.pem`
   - OCI: `ssh -i ~/.ssh/oci_family_reunion opc@146.235.195.248 "sudo /usr/local/bin/k3s kubectl -n kube-system get secret -l sealedsecrets.bitnami.com/sealed-secrets-key -o jsonpath='{.items[0].data.tls\.crt}' | base64 -d" > /tmp/oci-sealed-secrets-cert.pem`

2. Ask the user for the secret data (key-value pairs) and namespace (default: family-reunion).

3. Create a temporary plaintext secret YAML in /tmp.

4. Seal for both clusters:
   - `kubeseal --format yaml --cert /tmp/homelab-sealed-secrets-cert.pem < /tmp/<name>-secret.yaml > k8s/sealed-<name>-secret.yaml`
   - `kubeseal --format yaml --cert /tmp/oci-sealed-secrets-cert.pem < /tmp/<name>-secret.yaml > k8s/oci/sealed-<name>-secret.yaml`

5. Delete the plaintext file: `rm /tmp/<name>-secret.yaml`

6. Apply to both clusters:
   - First delete old secret: `kubectl delete secret <name> -n family-reunion --ignore-not-found`
   - Then apply sealed secret: `kubectl apply -f sealed-<name>-secret.yaml`

7. Report results.
