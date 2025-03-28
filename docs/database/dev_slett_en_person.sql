WITH
sakz AS (DELETE FROM sak WHERE ident = '21506021062' RETURNING id),
behandlingz AS (DELETE FROM behandling WHERE sak_id IN (SELECT id FROM sakz) returning id),
søknadz AS (DELETE FROM søknad WHERE sak_id IN (SELECT id FROM sakz) OR behandling_id in (select id from behandlingz) returning id),
søknad_barnetilleggz AS (DELETE FROM søknad_barnetillegg WHERE søknad_id IN (SELECT id FROM søknadz)),
søknadstiltakz AS (DELETE FROM søknadstiltak WHERE søknad_id IN (SELECT id FROM søknadz)),
rammevedtakz AS (DELETE FROM rammevedtak WHERE sak_id IN (SELECT id FROM sakz)),
meldekortz AS (DELETE FROM meldekortbehandling WHERE sak_id IN (SELECT id FROM sakz)),
meldekort_brukerz as (delete from meldekort_bruker mb where sak_id  IN (SELECT id FROM sakz)),
utbetalingsvedtakz AS (DELETE FROM utbetalingsvedtak WHERE sak_id IN (SELECT id FROM sakz)),
statistikk_utbetalingz AS (DELETE FROM statistikk_utbetaling WHERE sak_id IN (SELECT id FROM sakz)),
statistikk_stonadz AS (DELETE FROM statistikk_stonad WHERE sak_id IN (SELECT id FROM sakz)),
statistikk_sakz AS (DELETE FROM statistikk_sak WHERE sak_id IN (SELECT id FROM sakz) returning id),
SELECT id FROM sakz;