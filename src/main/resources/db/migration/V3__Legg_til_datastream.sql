DO
$$
    BEGIN
        IF
            EXISTS
                (SELECT 1 from pg_roles where rolname = 'tpts_ds')
        THEN
            ALTER DEFAULT PRIVILEGES IN SCHEMA PUBLIC GRANT SELECT ON TABLES TO "tpts_ds";
            GRANT SELECT ON ALL TABLES IN SCHEMA PUBLIC TO "tpts_ds";

            ALTER USER "tiltakspenger-saksbehandling-api" WITH REPLICATION;
            ALTER USER "tpts_ds" WITH REPLICATION;
            CREATE PUBLICATION "ds_publication" FOR ALL TABLES; -- Denne er kommentert ut fordi flyway kjører migrering hver gang V1 endres, og da får vi feil på at 'ds_publication' finnes fra før på deploy.
        END IF;
    END
$$;
