ALTER TABLE statistikk_stonad
    ADD COLUMN IF NOT EXISTS omgjor_rammevedtak jsonb NOT NULL DEFAULT '[]'::jsonb;

-- Henter vedtak-ider på nytt for å fikse opp mangler i db pga tidligere bugs
UPDATE statistikk_stonad s
SET omgjor_rammevedtak = (
    SELECT COALESCE(
                   jsonb_agg(elem->>'vedtakId'),
                   '[]':: jsonb
           )
    FROM rammevedtak r join behandling b on r.behandling_id = b.id,
         jsonb_array_elements(b.omgjør_rammevedtak->'omgjørRammevedtak') AS elem
    WHERE r.id = s.vedtak_id
)
WHERE EXISTS (
    SELECT 1 FROM rammevedtak r WHERE r.id = s.vedtak_id
);

-- Ikke lengre i bruk
ALTER TABLE statistikk_stonad DROP COLUMN IF EXISTS tiltaksdeltakelser;
