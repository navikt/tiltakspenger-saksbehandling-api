DO
$$
    BEGIN
        IF EXISTS
            (SELECT * FROM pg_roles WHERE rolname = 'tpts_datastream')
        THEN
           IF NOT EXISTS
                (SELECT * FROM pg_replication_slots WHERE slot_name = 'tpts_replication')
            THEN
                PERFORM PG_CREATE_LOGICAL_REPLICATION_SLOT('tpts_replication', 'pgoutput');
            END IF;
        END IF;
    END
$$;
