DO
$$
    BEGIN
        IF
            EXISTS
                (SELECT 1 from pg_roles where rolname = 'tpts_datastream')
        THEN
            ALTER DEFAULT PRIVILEGES IN SCHEMA PUBLIC GRANT SELECT ON TABLES TO "tpts_datastream";
            GRANT SELECT ON ALL TABLES IN SCHEMA PUBLIC TO "tpts_datastream";

            ALTER USER "tpts_datastream" WITH REPLICATION;
        END IF;
    END
$$;
