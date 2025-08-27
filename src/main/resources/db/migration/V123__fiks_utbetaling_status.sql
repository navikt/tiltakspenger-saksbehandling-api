-- Fikser feil fra V122
UPDATE utbetaling SET status = CASE status
    WHEN 'IkkePåbegynt' THEN 'IKKE_PÅBEGYNT'
    WHEN 'SendtTilOppdrag' THEN 'SENDT_TIL_OPPDRAG'
    WHEN 'FeiletMotOppdrag' THEN 'FEILET_MOT_OPPDRAG'
    WHEN 'Ok' THEN 'OK'
    WHEN 'OkUtenUtbetaling' THEN 'OK_UTEN_UTBETALING'
    WHEN 'Avbrutt' THEN 'AVBRUTT'
    ELSE status
END
