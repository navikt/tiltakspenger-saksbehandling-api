DO
$$
    BEGIN
        IF EXISTS
                (SELECT 1 from pg_roles where rolname = 'tiltakspenger-saksbehandling-api')
        THEN
            ALTER USER "tiltakspenger-saksbehandling-api" WITH REPLICATION;
        END IF;
    END
$$;
END;

DO
$$
    BEGIN
        IF EXISTS
                (SELECT 1 from pg_roles where rolname = 'tpts_ds')
        THEN
            ALTER USER "tpts_ds" WITH REPLICATION;
            ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT ON TABLES TO "tpts_ds";
            GRANT USAGE ON SCHEMA public TO "tpts_ds";
            GRANT SELECT ON ALL TABLES IN SCHEMA public TO "tpts_ds";
        END IF;
    END
$$;
END;

DO
$$
    BEGIN
        if not exists
                (select 1 from pg_publication where pubname = 'ds_publication')
        then
            CREATE PUBLICATION ds_publication for ALL TABLES;
        end if;
    end;
$$;
END;

DO
$$
    BEGIN
        if not exists
            (select 1 from pg_replication_slots where slot_name = 'ds_replication')
        then
            PERFORM PG_CREATE_LOGICAL_REPLICATION_SLOT('ds_replication', 'pgoutput');
        end if;
    end;
$$;
END;
