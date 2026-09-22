-- Tar med ubehandlede hendelser ved overgangen fra hendelseskø til markør på deltakeren.
-- Historikken beholdes, og en nyere markør fra consumerne skal ikke flyttes bakover.
UPDATE tiltaksdeltaker t
SET siste_ubehandlet_endring = h.siste_endring
FROM (
    SELECT tiltaksdeltaker_id, MAX(sist_oppdatert) AS siste_endring
    FROM tiltaksdeltaker_kafka
    WHERE behandlet_tidspunkt IS NULL
    GROUP BY tiltaksdeltaker_id
) h
WHERE t.id = h.tiltaksdeltaker_id
  AND (t.siste_ubehandlet_endring IS NULL OR t.siste_ubehandlet_endring < h.siste_endring);
