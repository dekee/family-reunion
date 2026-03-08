#!/bin/bash
# Reset all payments to zero in production
echo "Resetting all payments in production..."
ssh -i ~/.ssh/oci_family_reunion opc@146.235.195.248 \
  "sudo /usr/local/bin/kubectl exec -n family-reunion postgres-0 -- psql -U reunion -d reuniondb -c 'DELETE FROM payment_line_items; DELETE FROM payments;'"
echo "Done! All payments have been reset to zero."
