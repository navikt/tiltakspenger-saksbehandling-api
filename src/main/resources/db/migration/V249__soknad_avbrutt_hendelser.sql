-- Backfiller historikken for søknader som allerede er avbrutt, slik at begrunnelsen for avbruddet
-- overlever en senere gjenåpning (som nullstiller `avbrutt`).
update søknad
set avbrutt = jsonb_build_array(
        jsonb_build_object(
                'type', 'AVBRUTT',
                'tidspunkt', avbrutt ->> 'avbruttTidspunkt',
                'utførtAv', avbrutt ->> 'avbruttAv',
                'begrunnelse', avbrutt ->> 'begrunnelse'
        )
              )
where avbrutt is not null;

update søknad
set avbrutt = jsonb_build_array()
where avbrutt is null;

-- En søknad har alltid en (eventuelt tom) hendelsesliste, så kolonnen er ikke lenger nullbar.
-- Defaulten gjør at insert-en i SøknadDAO slipper å sette den eksplisitt.
alter table søknad
    alter column avbrutt set default jsonb_build_array(),
    alter column avbrutt set not null;

-- De partielle indeksene fra V192 var bygget på at `avbrutt is null` betydde «ikke avbrutt».
-- Nå er kolonnen aldri null, så de indekserer enten alle eller ingen rader.
-- Navnene er autogenererte, så vi slår dem opp i stedet for å gjette.
do
$$
    declare
        idx record;
    begin
        for idx in
            select indexrelid::regclass as navn
            from pg_index
            where indrelid = 'søknad'::regclass
              and indpred is not null
              and pg_get_expr(indpred, indrelid) like '%avbrutt IS%NULL%'
            loop
                execute format('drop index %s', idx.navn);
            end loop;
    end
$$;

-- Predikatet er `avbrutt = '[]'` og ikke `jsonb_array_length(avbrutt) = 0`.
-- Flyway kjører hele migreringen i én transaksjon, og en indeksbygging evaluerer predikatet også på de gamle
-- radversjonene backfillen over nettopp erstattet — de inneholder fortsatt objektet, så `jsonb_array_length`
-- feiler med «cannot get array length of a non-array».
-- Likhet er definert for alle jsonb-verdier og er ekvivalent med lengde 0 nå som kolonnen alltid er en array.
create index on søknad (soknadstype, opprettet) where avbrutt = '[]'::jsonb;
