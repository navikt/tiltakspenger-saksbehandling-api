-- Patch data til å ha frist i ventestatushendelser. Settes til null da det er valgfritt å oppgi en frist.

UPDATE behandling
SET ventestatus = jsonb_set(
    ventestatus,
    '{ventestatusHendelser}',
    (
        SELECT COALESCE(jsonb_agg(
            CASE
                WHEN hendelse ? 'frist' THEN hendelse
                ELSE jsonb_set(hendelse, '{frist}', 'null'::jsonb, true)
            END
            ORDER BY ord
        ), '[]'::jsonb)
        FROM jsonb_array_elements(COALESCE(ventestatus->'ventestatusHendelser', '[]'::jsonb)) WITH ORDINALITY AS t(hendelse, ord)
    ),
    true
)
WHERE ventestatus ? 'ventestatusHendelser'
  AND EXISTS (
      SELECT 1
      FROM jsonb_array_elements(COALESCE(ventestatus->'ventestatusHendelser', '[]'::jsonb)) AS e(hendelse)
      WHERE NOT (hendelse ? 'frist')
  );

UPDATE klagebehandling
SET ventestatus = jsonb_set(
    ventestatus,
    '{ventestatusHendelser}',
    (
        SELECT COALESCE(jsonb_agg(
            CASE
                WHEN hendelse ? 'frist' THEN hendelse
                ELSE jsonb_set(hendelse, '{frist}', 'null'::jsonb, true)
            END
            ORDER BY ord
        ), '[]'::jsonb)
        FROM jsonb_array_elements(COALESCE(ventestatus->'ventestatusHendelser', '[]'::jsonb)) WITH ORDINALITY AS t(hendelse, ord)
    ),
    true
)
WHERE ventestatus ? 'ventestatusHendelser'
  AND EXISTS (
      SELECT 1
      FROM jsonb_array_elements(COALESCE(ventestatus->'ventestatusHendelser', '[]'::jsonb)) AS e(hendelse)
      WHERE NOT (hendelse ? 'frist')
  );
