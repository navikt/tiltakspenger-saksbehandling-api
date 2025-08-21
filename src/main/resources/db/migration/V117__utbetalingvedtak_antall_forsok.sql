-- Indeks for å sortere på antall forsøk når man skal velge ut hvilke utbetalinger som skal behandles i en batch.
CREATE INDEX idx_utbetalingsvedtak_antall_forsok
    ON utbetalingsvedtak (((status_metadata ->> 'antall_forsøk')::int));
