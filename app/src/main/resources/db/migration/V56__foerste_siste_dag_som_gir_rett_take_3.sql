ALTER TABLE sak DROP COLUMN IF EXISTS første_dag_som_gir_rett;
ALTER TABLE sak DROP COLUMN IF EXISTS siste_dag_som_gir_rett;

ALTER TABLE sak ADD COLUMN første_dag_som_gir_rett DATE;
ALTER TABLE sak ADD COLUMN siste_dag_som_gir_rett DATE;

UPDATE sak
SET
    første_dag_som_gir_rett = v.fra_og_med,
    siste_dag_som_gir_rett = v.til_og_med
FROM
    rammevedtak v
WHERE
    sak.id = v.sak_id
    AND v.sak_id IN (
        SELECT
            sak_id
        FROM
            rammevedtak
        GROUP BY
            sak_id
        HAVING
            COUNT(*) = 1
    );