-- Søknadsbehandlinger uten valgt resultat lagres nå eksplisitt som IKKE_VALGT, slik som OMGJØRING_IKKE_VALGT for omgjøringer.
UPDATE behandling
SET resultat = 'IKKE_VALGT'
WHERE resultat IS NULL;

ALTER TABLE behandling
    ALTER COLUMN resultat SET NOT NULL;
