-- Legger på unike constraints der prodkoden allerede antar unikhet, men databasen ikke håndhevet den.
--
-- Uten dem kan koden plukke en vilkårlig rad (eller kaste et uforståelig unntak) hvis duplikater først oppstår,
-- og de tilhørende `single`/`asSingle`-antakelsene i domenet og repoene har ingen garanti bak seg.
--
-- Verifisert med scripts/verifiser-manglende-unike-constraints.sql mot både dev og prod 2026-07-31: ingen brudd i dataene.
-- Tabellene er små (pilotskala), så indeksbyggingen låser bare kortvarig.

-- En person har maks én sak.
-- Håndheves i dag kun av SakPostgresRepo.hentForFnr, som bruker asSingle mot en strict-sesjon.
--
-- Merk at dette ikke bare dokumenterer en invariant: SakService.hentEllerOpprettSak leser først og oppretter etterpå,
-- uten transaksjon eller lås, så to samtidige kall for samme person lager i dag to saker.
-- Constrainten er det eneste som stopper den racen, og gjør taperen til en synlig feil i stedet for stille dobbeltregistrering.
ALTER TABLE sak
    ADD CONSTRAINT sak_fnr_unique UNIQUE (fnr);

-- En rammebehandling gir maks ett rammevedtak.
-- Rammevedtaksliste.hentVedtakForBehandlingId bruker single {}.
-- Kolonnen er nullable, og NULL teller ikke som duplikat i en unik constraint.
ALTER TABLE rammevedtak
    ADD CONSTRAINT rammevedtak_behandling_id_unique UNIQUE (behandling_id);

-- En klagebehandling gir maks ett klagevedtak.
-- Klagevedtaksliste.hentForKlagebehandlingId bruker single {}.
ALTER TABLE klagevedtak
    ADD CONSTRAINT klagevedtak_klagebehandling_id_unique UNIQUE (klagebehandling_id);

-- Referansen til behandlingen i tilbakekrevingsløsningen er ekstern og skal peke på én rad.
-- TilbakekrevingBehandlingPostgresRepo slår opp på den med asSingle.
ALTER TABLE tilbakekreving_behandling
    ADD CONSTRAINT tilbakekreving_behandling_tilbake_behandling_id_unique UNIQUE (tilbake_behandling_id);
