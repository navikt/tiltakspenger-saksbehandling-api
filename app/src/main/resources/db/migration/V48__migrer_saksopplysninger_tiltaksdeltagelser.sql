WITH extracted_data AS (
    SELECT
        id,
        (saksopplysninger->'fødselsdato') AS fødselsdato,
        (saksopplysninger->'tiltaksdeltagelse') AS tiltak
    FROM behandling
    WHERE saksopplysninger IS NOT NULL
)
UPDATE behandling b
SET saksopplysninger = jsonb_build_object(
  'fødselsdato', e.fødselsdato,
  'tiltaksdeltagelse', jsonb_build_array(
    jsonb_build_object(
      'id', tiltak->'id',
      'eksternDeltagelseId', tiltak->'eksternDeltagelseId',
      'gjennomføringId', tiltak->'gjennomføringId',
      'typeNavn', tiltak->'typeNavn',
      'typeKode', tiltak->'typeKode',
      'deltagelseFraOgMed', tiltak->'deltagelseFraOgMed',
      'deltagelseTilOgMed', tiltak->'deltagelseTilOgMed',
      'deltakelseStatus', tiltak->'deltakelseStatus',
      'deltakelseProsent', tiltak->'deltakelseProsent',
      'antallDagerPerUke', tiltak->'antallDagerPerUke',
      'kilde', tiltak->'kilde',
      'rettPåTiltakspenger', tiltak->'rettPåTiltakspenger'
    )
  )
)
FROM extracted_data e
WHERE b.id = e.id;
