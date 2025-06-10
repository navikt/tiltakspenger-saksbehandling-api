UPDATE behandling SET behandlingstype =
    CASE
        WHEN behandlingstype = 'FØRSTEGANGSBEHANDLING' THEN 'SØKNADSBEHANDLING'
        ELSE behandlingstype
    END;
