ALTER TABLE sak
    ADD COLUMN IF NOT EXISTS siste_dag_som_gir_rett DATE DEFAULT NULL;

UPDATE sak s
SET siste_dag_som_gir_rett = (SELECT MAX(m.til_og_med)
                              FROM meldeperiode m
                              WHERE m.sak_id = s.id)
WHERE s.siste_dag_som_gir_rett IS NULL;

ALTER TABLE sak
    ALTER COLUMN siste_dag_som_gir_rett DROP DEFAULT;