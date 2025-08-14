WITH
sakz AS (DELETE FROM sak WHERE id = 'sak_01JQVBMZ7WX2VKEEK8XKV00WJ4' RETURNING id),
behandlingz AS (DELETE FROM behandling WHERE sak_id IN (SELECT id FROM sakz) returning soknad_id),
søknadz AS (DELETE FROM søknad WHERE sak_id IN (SELECT id FROM sakz) OR id in (select soknad_id from behandlingz) returning id),
søknad_barnetilleggz AS (DELETE FROM søknad_barnetillegg WHERE søknad_id IN (SELECT id FROM søknadz)),
søknadstiltakz AS (DELETE FROM søknadstiltak WHERE søknad_id IN (SELECT id FROM søknadz)),
rammevedtakz AS (DELETE FROM rammevedtak WHERE sak_id IN (SELECT id FROM sakz)),
meldekortz AS (DELETE FROM meldekortbehandling WHERE sak_id IN (SELECT id FROM sakz)),
meldekort_brukerz AS (DELETE from meldekort_bruker mb WHERE sak_id IN (SELECT id FROM sakz)),
utbetalingsvedtakz AS (DELETE FROM utbetalingsvedtak WHERE sak_id IN (SELECT id FROM sakz)),
meldekortbehandlingz AS (DELETE from meldekortbehandling mb where sak_id IN (SELECT id FROM sakz)),
meldeperiode AS (delete from meldeperiode m WHERE sak_id IN (SELECT id FROM sakz)),
statistikk_utbetalingz AS (DELETE FROM statistikk_utbetaling WHERE sak_id IN (SELECT id FROM sakz)),
statistikk_stonadz AS (DELETE FROM statistikk_stonad WHERE sak_id IN (SELECT id FROM sakz)),
statistikk_sakz AS (DELETE FROM statistikk_sak WHERE sak_id IN (SELECT id FROM sakz) returning id),
tiltaksdeltaker_kafkaz AS (DELETE FROM tiltaksdeltaker_kafka WHERE sak_id IN (SELECT id FROM sakz))
SELECT id FROM sakz;
