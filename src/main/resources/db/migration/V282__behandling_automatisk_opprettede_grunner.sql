-- Grunnene til en automatisk opprettet revurdering lagres som en liste, slik at en ubestemt revurdering kan få nye grunner.
-- Eksisterende grunner får tidspunktet revurderingen ble opprettet som lagtTil.
-- hendelseId er ikke lest siden grunnen ble knyttet til deltakelsen, og fjernes.
update behandling
set automatisk_opprettet_grunn = jsonb_build_object(
        'grunner',
        jsonb_build_array(
            (automatisk_opprettet_grunn - 'hendelseId') || jsonb_build_object('lagtTil', to_jsonb(opprettet::timestamp))
        )
    )
where automatisk_opprettet_grunn is not null
  and automatisk_opprettet_grunn -> 'grunner' is null;
