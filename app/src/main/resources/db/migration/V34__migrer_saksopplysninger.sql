-- Tar kun førstegangsbehandling i førsteomgang
WITH extracted_data AS (
  SELECT
    id,
    (vilkårssett->'tiltakDeltagelseVilkår'->'registerSaksopplysning') AS tiltak,
    (vilkårssett->'alderVilkår'->'avklartSaksopplysning'->>'fødselsdato') AS fødselsdato
  FROM behandling
  WHERE vilkårssett IS NOT NULL
    AND (vilkårssett->>'tiltakDeltagelseVilkår')::jsonb->'registerSaksopplysning' IS NOT NULL
    AND saksopplysninger IS NULL
    AND behandlingstype = 'FØRSTEGANGSBEHANDLING'
)
UPDATE behandling b
SET saksopplysninger = jsonb_build_object(
  'fødselsdato', e.fødselsdato,
  'tiltaksdeltagelse', jsonb_build_object(
    'id', tiltak->>'eksternTiltakId',
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
