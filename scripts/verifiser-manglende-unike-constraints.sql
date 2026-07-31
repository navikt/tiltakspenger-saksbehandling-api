-- Verifiserer at dataene tåler de unike constraintene i V238__unike_constraints_som_manglet.sql.
--
-- Kjør mot dev og prod FØR migreringen deployes.
-- Tom resultatmengde betyr at constraintene kan legges på; hver rad som kommer ut er et brudd migreringen ville feilet på.
--
-- Kjørt mot dev og prod 2026-07-31: ingen rader i noen av miljøene.
-- Spørringen er beholdt fordi den er den samme sjekken du trenger hvis en tilsvarende constraint skal legges på senere, eller hvis migreringen mot formodning feiler i et miljø.
--
-- Kandidatene er kolonner der prodkoden allerede antar unikhet, men databasen ikke håndhever den:
--   sak.fnr                                         -- SakPostgresRepo.hentForFnr bruker asSingle mot en strict-sesjon
--   rammevedtak.behandling_id                       -- Rammevedtaksliste.hentVedtakForBehandlingId bruker single {}
--   klagevedtak.klagebehandling_id                  -- Klagevedtaksliste.hentForKlagebehandlingId bruker single {}
--   tilbakekreving_behandling.tilbake_behandling_id -- TilbakekrevingBehandlingPostgresRepo slår opp med asSingle
--
-- NULL teller ikke som duplikat i en unik constraint, så nullable kolonner filtreres på `is not null`.
--
-- Merk: spørringen skriver aldri ut selve verdien som er duplisert.
-- For `sak.fnr` ville det vært et fødselsnummer på skjermen, og resultatet skal kunne limes inn i en issue eller en melding uten vasking.
-- Kolonnen `berørte_rader` gir id-ene du trenger for å grave videre — de er ikke personopplysninger.

select 'sak.fnr' as kandidat,
       count(*) as antall_rader,
       string_agg(id::text, ', ' order by id) as berørte_rader
from sak
where fnr in (select fnr from sak group by fnr having count(*) > 1)
having count(*) > 0

union all

select 'rammevedtak.behandling_id',
       count(*),
       string_agg(id::text, ', ' order by id)
from rammevedtak
where behandling_id is not null
  and behandling_id in (select behandling_id from rammevedtak where behandling_id is not null group by behandling_id having count(*) > 1)
having count(*) > 0

union all

select 'klagevedtak.klagebehandling_id',
       count(*),
       string_agg(id::text, ', ' order by id)
from klagevedtak
where klagebehandling_id in (select klagebehandling_id from klagevedtak group by klagebehandling_id having count(*) > 1)
having count(*) > 0

union all

select 'tilbakekreving_behandling.tilbake_behandling_id',
       count(*),
       string_agg(id::text, ', ' order by id)
from tilbakekreving_behandling
where tilbake_behandling_id in (select tilbake_behandling_id from tilbakekreving_behandling group by tilbake_behandling_id having count(*) > 1)
having count(*) > 0

order by kandidat;
