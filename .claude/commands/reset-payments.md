Reset all payments to zero in the production database. Run the following command to delete all payment_line_items and payments from the PostgreSQL database on Oracle Cloud:

```
ssh -i ~/.ssh/oci_family_reunion opc@146.235.195.248 "sudo /usr/local/bin/kubectl exec -n family-reunion postgres-0 -- psql -U reunion -d reuniondb -c 'DELETE FROM payment_line_items; DELETE FROM payments;'"
```

Report the result to the user.
