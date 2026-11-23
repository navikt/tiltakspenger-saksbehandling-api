-- Hvor langt saken har kommet i migreringen fra oppdragsversjon 2 til 3, se Oppdragsmigrering.kt; null når saken ikke er migrert.
-- Utbetalingene i saken sendes ikke til Hel Ved før migreringen er FERDIG; migrert_hos_hel_ved_tidspunkt viser hvor lenge saken har ventet på at Hel Ved tar opp kjedene.
ALTER TABLE sak
    ADD COLUMN migrert_hos_hel_ved_tidspunkt TIMESTAMPTZ,
    ADD COLUMN oppdragsmigrering TEXT
        CHECK (
            oppdragsmigrering IS NULL
            OR (oppdragsmigrering = 'SPERRET' AND oppdragsversjon = 2)
            OR (oppdragsmigrering IN ('MIGRERT_HOS_HEL_VED', 'FERDIG') AND oppdragsversjon = 3 AND migrert_hos_hel_ved_tidspunkt IS NOT NULL)
        );
