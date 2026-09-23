-- Rapporteringsviews for klagebehandling, brukt av interne dashboards i Metabase via BigQuery federated queries.
-- Viewene ligger i et afterMigrate-callback framfor en versjonert migrering fordi de er avledet data som skal kunne endres fritt når rapporteringsbehovet endrer seg.
-- Callbacket kjører ved hver oppstart, så et view som forsvinner ved en Cloud SQL-restore er tilbake ved neste deploy uten at noen må gjøre noe manuelt.
-- Det er samme selvreparerende mønster som afterMigrate__gjenopprett_replication_slot.sql, og det koster ingen versjonsnumre når rapporteringsønskene endrer seg.
--
-- DROP og CREATE framfor CREATE OR REPLACE VIEW fordi sistnevnte bare tillater å legge til kolonner på slutten.
-- Vi forventer å endre og fjerne kolonner her, og da må viewet bygges på nytt.
--
-- Viewene eksponerer bevisst verken personopplysninger eller saksbehandlerfritekst.
-- klagebehandling.brevtekst, resultat->>'omgjørBegrunnelse' og resultat->>'begrunnelseFerdigstilling' er fritekst og er derfor utelatt.
-- fnr ligger ikke på klagebehandling i det hele tatt, og hentes bevisst ikke inn fra sak.
-- saksbehandler og beslutter er utelatt av samme grunn, siden rapportene handler om saksflyt og ikke om enkeltansatte.
--
-- BigQuery-brukeren får SELECT kun på viewene, og siden security_invoker ikke er satt kjører de med eierens rettigheter.
-- Det er den mekanismen som gjør viewet til en faktisk tilgangsgrense framfor bare en filtrering, fordi bigquery-brukeren verken trenger eller får lesetilgang på klagebehandling.
--
-- Json-nøklene er Kotlin-navnene verbatim, inkludert æøå, jf. KlagebehandlingsresultatDbJsonTest som pinner den lagrede json-en.
-- Tidspunktene i json-en er LocalDateTime uten sone, og castes derfor til timestamp og ikke timestamptz.
--
-- Merk at testsuiten ikke dekker denne fila: libs' TestDatabaseManager kjører sin egen Flyway mot default location og ser aldri db/callback.

DROP VIEW IF EXISTS klagebehandling_view CASCADE;

-- Én rad per klagebehandling, altså nåtilstanden og ikke historikken.
-- Trenger vi historikk senere, er statistikk_sak fortsatt hendelsesloggen som har den.
CREATE VIEW klagebehandling_view AS
SELECT k.id                                                         AS klagebehandling_id,
       k.sak_id,
       k.opprettet,
       k.sist_endret,
       -- Domenets egen Klagebehandlingsstatus med full granularitet.
       -- Saksstatistikken kollapser flere av disse til felles DVH-verdier, og den detaljen er det vi er ute etter her.
       k.status,
       k.resultat ->> 'type'                                         AS resultat,
       k.resultat ->> 'omgjørÅrsak'                                  AS omgjoer_aarsak,
       (k.resultat ->> 'oversendtKlageinstansenTidspunkt')::timestamp AS oversendt_klageinstansen_tidspunkt,
       (k.resultat ->> 'ferdigstiltTidspunkt')::timestamp             AS ferdigstilt_tidspunkt,
       k.iverksatt_tidspunkt,
       k.avbrutt IS NOT NULL                                         AS er_avbrutt,
       siste_hendelse.utfall,
       siste_hendelse.utfall_tidspunkt
FROM klagebehandling k
         -- Utfallet fra klageinstansen ligger i resultat->'klageinstanshendelser', som kun er utfylt for Opprettholdt.
         -- Klager vi omgjør eller avviser selv går aldri til KA, og får derfor null her.
         -- En klagebehandling kan ha flere hendelser, så vi plukker den siste avsluttede.
         LEFT JOIN LATERAL (
    SELECT COALESCE(h ->> 'avsluttetUtfall', h ->> 'omgjøringskravUtfall') AS utfall,
           (h ->> 'avsluttetTidspunkt')::timestamp                         AS utfall_tidspunkt
    FROM jsonb_array_elements(k.resultat -> 'klageinstanshendelser') AS h
    WHERE h ->> 'avsluttetTidspunkt' IS NOT NULL
    ORDER BY (h ->> 'avsluttetTidspunkt')::timestamp DESC
    LIMIT 1
    ) AS siste_hendelse ON true;

DROP VIEW IF EXISTS klage_omgjoeringsbehandling_view CASCADE;

-- Én rad per (klagebehandling, behandling klagen har skapt).
-- De to FK-kolonnene er de normaliserte inverse kantene av resultat->'behandlingId', jf. V240.
-- En klage kan skape flere behandlinger, så tell DISTINCT klagebehandling_id dersom du aggregerer på klagenivå.
CREATE VIEW klage_omgjoeringsbehandling_view AS
SELECT k.id                  AS klagebehandling_id,
       k.status              AS klage_status,
       'RAMMEBEHANDLING'     AS omgjoeringsbehandling_type,
       b.id                  AS omgjoeringsbehandling_id,
       b.status              AS omgjoeringsbehandling_status,
       b.sist_endret         AS omgjoeringsbehandling_sist_endret,
       b.iverksatt_tidspunkt AS omgjoeringsbehandling_iverksatt_tidspunkt
FROM klagebehandling k
         JOIN behandling b ON b.klagebehandling_id = k.id
UNION ALL
SELECT k.id,
       k.status,
       'MELDEKORTBEHANDLING',
       m.id,
       m.status,
       m.sist_endret,
       m.iverksatt_tidspunkt
FROM klagebehandling k
         JOIN meldekortbehandling m ON m.klagebehandling_id = k.id;

-- BigQuery-brukeren provisjoneres utenfor denne fila, og grantet er derfor betinget.
-- Da stopper ikke callbacket oppstart i miljøer der brukeren ennå ikke finnes, f.eks. lokalt.
-- Samme betingede mønster som rolleblokkene i V236.
-- Grants forsvinner når viewet droppes over, så de settes på nytt her hver gang.
DO
$$
    BEGIN
        IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'tpts_bigquery_views') THEN
            GRANT USAGE ON SCHEMA public TO "tpts_bigquery_views";
            GRANT SELECT ON klagebehandling_view TO "tpts_bigquery_views";
            GRANT SELECT ON klage_omgjoeringsbehandling_view TO "tpts_bigquery_views";
        END IF;
    END
$$;
