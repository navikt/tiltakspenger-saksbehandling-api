WITH extracted_data AS (
  SELECT
    id,
    virkningsperiode_fra_og_med,
    virkningsperiode_til_og_med,
    (saksopplysninger->'tiltaksdeltagelse'->'eksternDeltagelseId') AS tiltakid
  FROM behandling
  WHERE saksopplysninger IS NOT NULL
    AND virkningsperiode_fra_og_med IS NOT NULL
    AND virkningsperiode_til_og_med IS NOT NULL
    AND (saksopplysninger->>'tiltaksdeltagelse')::jsonb->'eksternDeltagelseId' IS NOT NULL
    AND valgte_tiltaksdeltakelser IS NULL
    AND status != 'KLAR_TIL_BEHANDLING'
)
UPDATE behandling b
SET valgte_tiltaksdeltakelser = jsonb_build_object(
  'value', jsonb_build_array(
    jsonb_build_object(
      'verdi', e.tiltakid,
      'periode', jsonb_build_object(
        'fraOgMed', e.virkningsperiode_fra_og_med,
        'tilOgMed', e.virkningsperiode_til_og_med
      )
    )
  )
)
FROM extracted_data e
WHERE b.id = e.id;
