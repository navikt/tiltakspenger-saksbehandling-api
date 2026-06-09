-- Migrerer type fra meldekortbehandling.type-kolonnen inn i hvert element i meldeperioder-json-arrayen.
-- Vi antar at alle meldeperioder i en meldekortbehandling har samme type.
update meldekortbehandling as m
set meldeperioder = (
    select jsonb_agg(elem || jsonb_build_object('type', m.type))
    from jsonb_array_elements(m.meldeperioder) as elem
)
where m.type is not null
  and jsonb_typeof(m.meldeperioder) = 'array'
  and jsonb_array_length(m.meldeperioder) > 0;
