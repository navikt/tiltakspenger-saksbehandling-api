-- Gjenoppretter logisk replikering til BigQuery (Datastream/Team Sak DVH) i miljøer der replication slot
-- og/eller publication har forsvunnet, f.eks. fordi Cloud SQL har invalidert slotten på grunn av at WAL har
-- vokst seg for stor (typisk i dev når ingen konsumerer slotten kontinuerlig).
--
-- Bakgrunn: `SELECT * FROM pg_replication_slots;` returnerte tom resultatmengde i dev.
-- Se også docs/database/gjenopprett_replication_slot.sql for samme script til manuell kjøring,
-- og V3, V4, V5, V7 og V59 som satte opp det samme oppsettet opprinnelig.
--
-- Scriptet er idempotent, så det er trygt at det står igjen i migreringshistorikken selv om det
-- ikke gjør noe i miljøer der slotten/publication allerede finnes (f.eks. prod).

DO
$$
    BEGIN
        IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'tiltakspenger-saksbehandling-api') THEN
            ALTER USER "tiltakspenger-saksbehandling-api" WITH REPLICATION;
        END IF;
    END
$$;

DO
$$
    BEGIN
        IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'tpts_ds') THEN
            ALTER USER "tpts_ds" WITH REPLICATION;
            ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT ON TABLES TO "tpts_ds";
            GRANT USAGE ON SCHEMA public TO "tpts_ds";
            GRANT SELECT ON ALL TABLES IN SCHEMA public TO "tpts_ds";
        END IF;
    END
$$;

DO
$$
    BEGIN
        IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'tpts_datastream') THEN
            ALTER USER "tpts_datastream" WITH REPLICATION;
            ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT ON TABLES TO "tpts_datastream";
            GRANT USAGE ON SCHEMA public TO "tpts_datastream";
            GRANT SELECT ON ALL TABLES IN SCHEMA public TO "tpts_datastream";
        END IF;
    END
$$;

DO
$$
    BEGIN
        IF NOT EXISTS (SELECT 1 FROM pg_publication WHERE pubname = 'ds_publication') THEN
            CREATE PUBLICATION ds_publication FOR ALL TABLES;
        END IF;
    END
$$;

DO
$$
    BEGIN
        IF NOT EXISTS (SELECT 1 FROM pg_replication_slots WHERE slot_name = 'ds_replication') THEN
            PERFORM pg_create_logical_replication_slot('ds_replication', 'pgoutput');
        END IF;
    END
$$;
