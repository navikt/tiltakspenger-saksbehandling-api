-- Midlertidig markering av meldekort som ble liggende ubehandlet på grunn av en feil i den gamle benk-regelen.
-- Når en korrigering kom inn før den automatiske behandlingen av en tidligere innsending i samme kjede ble iverksatt, regnet benken korrigeringen som behandlet.
-- Korrigeringen ble aldri vist i benken, selv om ingen behandling tok den med.
-- Etter V287 og V288 regnes slike meldekort som ubehandlede, og benken viser dem i en egen kategori slik at de kan kontrolleres.
-- Markeringen settes bare her, så meldekort som kommer inn senere, får den aldri.
-- Kolonnen og kategorien fjernes når alle de markerte meldekortene er kontrollert.
ALTER TABLE meldekort_bruker
    ADD COLUMN IF NOT EXISTS til_etterkontroll boolean NOT NULL DEFAULT false;

-- Meldekort som er knyttet til en åpen behandling, blir behandlet av den og markeres ikke.
UPDATE meldekort_bruker mb
SET til_etterkontroll = true
WHERE mb.behandling_id IS NULL
  AND NOT EXISTS (
      SELECT 1
      FROM meldekortbehandling m
      WHERE m.sak_id = mb.sak_id
        AND m.meldeperioder @> jsonb_build_array(
            jsonb_build_object('kjedeId', mb.meldeperiode_kjede_id, 'brukersMeldekortIder', jsonb_build_array(mb.id))
        )
  )
  AND EXISTS (
      SELECT 1
      FROM meldekort_bruker tidligere
      JOIN meldekortbehandling automatisk ON automatisk.id = tidligere.behandling_id
      WHERE tidligere.sak_id = mb.sak_id
        AND tidligere.meldeperiode_kjede_id = mb.meldeperiode_kjede_id
        AND tidligere.mottatt < mb.mottatt
        AND automatisk.status = 'AUTOMATISK_BEHANDLET'
        AND automatisk.iverksatt_tidspunkt > mb.mottatt
  );
