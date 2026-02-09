-- Patch data til å ha frist i ventestatus, settes til null da det er valgfritt å oppgi en frist.
UPDATE behandling
SET ventestatus = jsonb_set(
        ventestatus,
        '{frist}',
        'null'::jsonb,
        true
                      )
WHERE NOT ventestatus ? 'frist';


UPDATE klagebehandling
SET ventestatus = jsonb_set(
        ventestatus,
        '{frist}',
        'null'::jsonb,
        true
                  )
WHERE NOT ventestatus ? 'frist';
