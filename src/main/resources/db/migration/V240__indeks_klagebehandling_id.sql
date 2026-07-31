-- Indekserer de to FK-kolonnene som peker tilbake på klagebehandlingen.
--
-- Kolonnene er de normaliserte inverse kantene av `klagebehandling.resultat->'behandlingId'`,
-- som er et JSONB-array med både rammebehandling- og meldekortbehandling-IDer.
-- Ingen av dem har hatt indeks. Det har ikke kostet noe så langt, fordi begge leses av rader
-- som allerede er hentet på primærnøkkel - men uten indeks må både et oppslag den andre veien
-- ("hvilke behandlinger hører til denne klagen") og FK-sjekken Postgres gjør ved delete/update
-- på klagebehandling gå til full tabellskann.
--
-- Indeksene er bevisst IKKE unike: en klagebehandling kan skape flere behandlinger.
-- Det er hele poenget med at `resultat->'behandlingId'` er en liste, jf. V212:
-- "Her kan man knytte uendelig mange behandlinger på klagen".
-- Unikheten som finnes her går andre veien - en behandling har maks én klagebehandling - og den
-- er allerede håndhevet av at kolonnen er skalar på en tabell med primærnøkkel.
--
-- Partielle fordi kolonnene er nullable og de aller fleste behandlinger ikke stammer fra en klage.
-- Postgres utleder at `klagebehandling_id = :x` impliserer `is not null`, så et partielt indeks
-- brukes for oppslaget - verifisert med `explain` mot Postgres 17.
-- Samme mønster som de partielle indeksene i V192__soknad_indekser.sql.
--
-- Tabellene er små (pilotskala), så indeksbyggingen låser bare kortvarig.

CREATE INDEX idx_behandling_klagebehandling_id
    ON behandling (klagebehandling_id)
    WHERE klagebehandling_id IS NOT NULL;

CREATE INDEX idx_meldekortbehandling_klagebehandling_id
    ON meldekortbehandling (klagebehandling_id)
    WHERE klagebehandling_id IS NOT NULL;
