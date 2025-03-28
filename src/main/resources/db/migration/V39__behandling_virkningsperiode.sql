ALTER TABLE behandling ADD COLUMN virkningsperiode_fra_og_med DATE;
COMMENT ON COLUMN behandling.virkningsperiode_fra_og_med IS 'Virkningsperioden er enten innvilgelsesperioden ved rett til tiltakspenger eller stansperiode/opphørsperiode ved ikke rett til tiltakspenger';
UPDATE behandling
SET virkningsperiode_fra_og_med =
    CASE
        WHEN behandlingstype = 'REVURDERING' THEN fra_og_med
        WHEN behandlingstype = 'FØRSTEGANGSBEHANDLING' THEN innvilgelsesperiode_fra_og_med
    END;

ALTER TABLE behandling ADD COLUMN virkningsperiode_til_og_med DATE;
COMMENT ON COLUMN behandling.virkningsperiode_til_og_med IS 'Virkningsperioden er enten innvilgelsesperioden ved rett til tiltakspenger eller stansperiode/opphørsperiode ved ikke rett til tiltakspenger';
UPDATE behandling
SET virkningsperiode_til_og_med =
    CASE
        WHEN behandlingstype = 'REVURDERING' THEN til_og_med
        WHEN behandlingstype = 'FØRSTEGANGSBEHANDLING' THEN innvilgelsesperiode_til_og_med
    END;
