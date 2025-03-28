DO
$$
BEGIN
    IF EXISTS(SELECT * FROM pg_roles WHERE rolname = 'tpts_ds') THEN
        PERFORM PG_CREATE_LOGICAL_REPLICATION_SLOT('ds_replication', 'pgoutput');
    END IF;
END
$$ LANGUAGE 'plpgsql';
