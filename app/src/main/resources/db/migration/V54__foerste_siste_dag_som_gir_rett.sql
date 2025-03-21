ALTER TABLE sak
    ADD COLUMN IF NOT EXISTS første_dag_som_gir_rett DATE;

UPDATE sak s
SET første_dag_som_gir_rett = (SELECT MAX(m.fra_og_med)
                              FROM meldeperiode m
                              WHERE m.sak_id = s.id)
WHERE s.første_dag_som_gir_rett IS NULL;
