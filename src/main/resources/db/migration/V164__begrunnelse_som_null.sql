/*
 Det er ønskelig at vi lagrer null for begrunnelse istedenfor tom streng.
 */

UPDATE behandling
set begrunnelse_vilkårsvurdering = null
WHERE begrunnelse_vilkårsvurdering = '';

UPDATE behandling
SET barnetillegg = jsonb_set(
        barnetillegg,
        '{begrunnelse}',
        'null'::jsonb
                   )
WHERE barnetillegg IS NOT NULL
  AND barnetillegg ->> 'begrunnelse' = '';

UPDATE meldekortbehandling
set begrunnelse = null
WHERE begrunnelse = '';


