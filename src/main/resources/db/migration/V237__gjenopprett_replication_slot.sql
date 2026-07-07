-- flyway:executeInTransaction=false
--
-- Gjenoppretter selve replication slotten for logisk replikering til BigQuery (Datastream/Team Sak DVH),
-- dersom den mangler. Se V236 for gjenoppretting av roller og publication som denne migreringen
-- forutsetter er på plass.
--
-- Denne migreringen kjøres uten transaksjon (`executeInTransaction=false`) fordi
-- `pg_create_logical_replication_slot` ikke kan kjøres i en transaksjon som allerede har utført
-- skriveoperasjoner, og Flyway kjører migreringer i transaksjon som standard.
--
-- Scriptet er idempotent, så det er trygt at det står igjen i migreringshistorikken selv om det
-- ikke gjør noe i miljøer der slotten allerede finnes (f.eks. prod).

DO
$$
    BEGIN
        IF NOT EXISTS (SELECT 1 FROM pg_replication_slots WHERE slot_name = 'ds_replication') THEN
            PERFORM pg_create_logical_replication_slot('ds_replication', 'pgoutput');
        END IF;
    END
$$;
