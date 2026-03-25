UPDATE klagebehandling kb
SET formkrav = jsonb_set(
        kb.formkrav,
        '{behandlingDetKlagesPå}',
        to_jsonb(rv.behandling_id),
        true
) FROM rammevedtak rv
WHERE kb.formkrav IS NOT NULL
  AND kb.formkrav->>'vedtakDetKlagesPå' IS NOT NULL
  AND (kb.formkrav->>'vedtakDetKlagesPå')::varchar = rv.id
  AND NOT (kb.formkrav ? 'behandlingDetKlagesPå');