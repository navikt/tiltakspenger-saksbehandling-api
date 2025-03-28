-- Del 2 av migrering, migrerer saksopplysninger for revurdering
WITH extracted_data AS (
  SELECT
    id,
    (vilkårssett->'tiltakDeltagelseVilkår'->'avklartSaksopplysning') AS tiltak,
    (vilkårssett->'alderVilkår'->'avklartSaksopplysning'->>'fødselsdato') AS fødselsdato
  FROM behandling
  WHERE vilkårssett IS NOT NULL
    AND (vilkårssett->>'tiltakDeltagelseVilkår')::jsonb->'avklartSaksopplysning' IS NOT NULL
    AND saksopplysninger IS NULL
    AND behandlingstype = 'REVURDERING'
)
UPDATE behandling b
SET saksopplysninger = jsonb_build_object(
  'fødselsdato', e.fødselsdato,
  'tiltaksdeltagelse', jsonb_build_object(
    'eksternDeltagelseId', tiltak->>'eksternTiltakId',
    'gjennomføringId', tiltak->>'gjennomføringId',
    'typeNavn', tiltak->>'tiltakNavn',
    'typeKode', tiltak->>'tiltakstype',
    'deltagelseFraOgMed', (tiltak->'deltagelsePeriode'->>'fraOgMed')::date,
    'deltagelseTilOgMed', (tiltak->'deltagelsePeriode'->>'tilOgMed')::date,
    'deltakelseStatus', tiltak->>'status',
    'deltakelseProsent', 100.0,
    'antallDagerPerUke', 5.0,
    'kilde', tiltak->>'kilde',
    'rettPåTiltakspenger', tiltak->>'girRett'
  )
)
FROM extracted_data e
WHERE b.id = e.id;
