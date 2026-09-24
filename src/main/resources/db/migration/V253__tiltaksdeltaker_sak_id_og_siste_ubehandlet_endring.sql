-- DDL-en er idempotent, slik at TiltaksdeltakerKømigreringAggregatTest kan kjøre hele fila på nytt mot historiske data.
alter table tiltaksdeltaker
    add column if not exists sak_id varchar references sak (id),
    add column if not exists siste_ubehandlet_endring timestamptz;

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

-- Jobben som plukker ubehandlede endringer spør på denne kolonnen.
create index if not exists idx_tiltaksdeltaker_siste_ubehandlet_endring
    on tiltaksdeltaker (siste_ubehandlet_endring)
    where siste_ubehandlet_endring is not null;

-- Tar med ubehandlede hendelser ved overgangen fra hendelseskø til markør på deltakeren.
-- Historikken beholdes, og en nyere markør fra consumerne skal ikke flyttes bakover.
UPDATE tiltaksdeltaker t
SET siste_ubehandlet_endring = h.siste_endring
FROM (
    SELECT tiltaksdeltaker_id, MAX(sist_oppdatert) AS siste_endring
    FROM tiltaksdeltaker_kafka
    WHERE behandlet_tidspunkt IS NULL
    GROUP BY tiltaksdeltaker_id
) h
WHERE t.id = h.tiltaksdeltaker_id
  AND (t.siste_ubehandlet_endring IS NULL OR t.siste_ubehandlet_endring < h.siste_endring);
