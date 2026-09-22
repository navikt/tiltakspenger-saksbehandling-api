alter table tiltaksdeltaker
    add column sak_id varchar references sak (id),
    add column siste_ubehandlet_endring timestamptz;

-- Backfill: knytter eksisterende deltakere til saken via søknadstiltaket.
-- En deltaker kan ha flere søknader, men alle ligger på samme sak.
update tiltaksdeltaker t
set sak_id = sub.sak_id
from (
         select distinct on (st.tiltaksdeltaker_id) st.tiltaksdeltaker_id, s.sak_id
         from søknadstiltak st
                  join søknad s on s.id = st.søknad_id
         order by st.tiltaksdeltaker_id, s.opprettet desc
     ) sub
where t.id = sub.tiltaksdeltaker_id;

-- Backfill av deltakere uten søknadstiltak (opprettet ifm saksopplysningsinnhenting av overlappende deltakelser).
-- De refereres fra behandlingens saksopplysninger-json, så saken finnes der.
update tiltaksdeltaker t
set sak_id = b.sak_id
from behandling b
where t.sak_id is null
  and b.saksopplysninger::text like '%' || t.id || '%';

-- Alle rader skal være dekket av backfillene over.
-- Feiler høylytt dersom det finnes en deltaker verken søknadstiltak eller en behandling refererer — det skal ikke finnes i prod.
alter table tiltaksdeltaker alter column sak_id set not null;

-- Jobben som plukker ubehandlede endringer spør på denne kolonnen.
create index idx_tiltaksdeltaker_siste_ubehandlet_endring
    on tiltaksdeltaker (siste_ubehandlet_endring)
    where siste_ubehandlet_endring is not null;
