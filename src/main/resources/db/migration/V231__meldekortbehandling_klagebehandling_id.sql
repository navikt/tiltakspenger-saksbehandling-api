alter table meldekortbehandling
    add column if not exists klagebehandling_id varchar references klagebehandling(id);

update klagebehandling
set resultat = jsonb_set(
    jsonb_set(
        resultat - 'rammebehandlingId' - 'åpenRammebehandlingId',
        '{behandlingId}',
        coalesce(resultat -> 'behandlingId', resultat -> 'rammebehandlingId', '[]'::jsonb),
        true
    ),
    '{åpenBehandlingId}',
    coalesce(resultat -> 'åpenBehandlingId', resultat -> 'åpenRammebehandlingId', 'null'::jsonb),
    true
)
where resultat is not null
  and (resultat ? 'rammebehandlingId' or resultat ? 'åpenRammebehandlingId');
