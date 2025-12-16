CREATE INDEX CONCURRENTLY idx_utbetaling_sak_status
ON utbetaling (sak_id, status);