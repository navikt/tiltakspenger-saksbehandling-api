-- Fiks feil fra V122
update utbetaling set opprettet = r.opprettet from rammevedtak r where rammevedtak_id = r.id;