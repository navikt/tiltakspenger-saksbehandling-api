ALTER TABLE behandling
    ADD COLUMN IF NOT EXISTS avslagsgrunner jsonb DEFAULT '[]' NOT NULL;

ALTER TABLE behandling
    ADD COLUMN IF NOT EXISTS utfall TEXT;

-- Alle behandlinger er implisitt innvilget dersom virkningsperioden er sendt inn (altså sendt til beslutning fordi man ikke har mulighet for et annet resultat)
-- TODO raq: Hvordan vil dette fungere for revurderinger/stans. I det minste burde vi bruke en where.
UPDATE behandling
SET utfall = CASE
                 WHEN virkningsperiode_fra_og_med IS NOT NULL THEN 'INNVILGELSE'
    END;