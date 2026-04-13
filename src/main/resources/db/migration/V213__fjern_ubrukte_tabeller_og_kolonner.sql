-- Fjerner utbetalingsvedtak-tabellen (erstattet av meldekortvedtak + utbetaling)
DROP TABLE IF EXISTS utbetalingsvedtak;

-- Fjerner ubrukte kolonner fra behandling-tabellen (erstattet av innvilgelsesperioder)
ALTER TABLE behandling DROP COLUMN IF EXISTS valgte_tiltaksdeltakelser;
ALTER TABLE behandling DROP COLUMN IF EXISTS antall_dager_per_meldeperiode;
ALTER TABLE behandling DROP COLUMN IF EXISTS innvilgelsesperiode;

-- Fjerner har_valgt_stans_til_siste_dag_som_gir_rett fra behandling (stans)
ALTER TABLE behandling DROP COLUMN IF EXISTS har_valgt_stans_til_siste_dag_som_gir_rett;

