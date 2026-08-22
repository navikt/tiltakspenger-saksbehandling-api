-- flyway:executeInTransaction=false
--
-- Gjenoppretter replication slotten `ds_replication` for logisk replikering til BigQuery (Datastream/Team Sak DVH), dersom den mangler.
-- Slotten lever i `pg_replslot/` på disk, utenfor både backup og databasedumpen, og forsvinner derfor ved hver Cloud SQL-restore.
-- `flyway_schema_history` restaureres derimot sammen med dataene og påstår at V237 er kjørt, så en versjonert migrering kan ikke reparere dette.
-- Det er grunnen til at vi har måttet lage en ny hver gang: V4, V7, V59, V236 og V237.
-- Et `afterMigrate`-callback kjører ved hver oppstart og reparerer seg selv ved neste deploy, uten et nytt versjonsnummer.
--
-- Kjøres uten transaksjon (`executeInTransaction=false`) fordi `pg_create_logical_replication_slot` ikke kan kjøres i en transaksjon som allerede har skrevet.
--
-- Callbacket er idempotent og skal aldri stoppe oppstart.
-- Mangler forutsetningene, logger vi og går videre.
-- `EXCEPTION`-blokka dekker i tillegg kappløpet mellom de to podene i prod.
--
-- Appen kobler til som `tiltakspenger-saksbehandling-api`, som har `REPLICATION` fra V59 og V236.
-- Det er den rettigheten `pg_create_logical_replication_slot` krever, og derfor trengs ingen superuser her.
--
-- Merk at testsuiten ikke dekker denne fila: libs' `TestDatabaseManager` kjører sin egen Flyway mot default location og ser aldri `db/callback`.

DO
$$
    BEGIN
        IF current_setting('wal_level') <> 'logical' THEN
            RAISE NOTICE 'Hopper over gjenoppretting av ds_replication: wal_level er %, ikke logical.', current_setting('wal_level');
            RETURN;
        END IF;

        IF NOT EXISTS (SELECT 1 FROM pg_publication WHERE pubname = 'ds_publication') THEN
            RAISE NOTICE 'Hopper over gjenoppretting av ds_replication: publication ds_publication finnes ikke.';
            RETURN;
        END IF;

        IF EXISTS (SELECT 1 FROM pg_replication_slots WHERE slot_name = 'ds_replication') THEN
            RETURN;
        END IF;

        PERFORM pg_create_logical_replication_slot('ds_replication', 'pgoutput');
        RAISE WARNING 'Opprettet replication slot ds_replication på nytt. Datastream trenger sannsynligvis en full backfill.';
    EXCEPTION
        WHEN OTHERS THEN
            RAISE WARNING 'Klarte ikke å gjenopprette replication slot ds_replication: %', SQLERRM;
    END
$$;
