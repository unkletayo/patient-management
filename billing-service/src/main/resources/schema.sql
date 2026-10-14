-- Databases created before SUSPENDED and CLOSED existed carry a CHECK constraint that only lists
-- the old statuses; ddl-auto=update never changes it. The status column is validated by the
-- application (BillingAccountStatusConverter), so the constraint is dropped. Safe to re-run.
ALTER TABLE IF EXISTS billing_accounts DROP CONSTRAINT IF EXISTS billing_accounts_status_check;
