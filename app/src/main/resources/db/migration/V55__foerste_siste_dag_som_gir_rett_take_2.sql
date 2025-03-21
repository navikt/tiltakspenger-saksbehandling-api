

UPDATE sak s
SET første_dag_som_gir_rett = (SELECT MIN(m.fra_og_med)
                              FROM meldeperiode m
                              WHERE m.sak_id = s.id);
