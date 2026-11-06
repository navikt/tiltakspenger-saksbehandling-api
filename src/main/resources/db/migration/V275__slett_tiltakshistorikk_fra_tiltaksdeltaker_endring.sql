-- Tiltakshistorikken ble kopiert til tabellen tiltakshistorikk i V274, og skrives ikke lenger til tiltaksdeltaker_endring.
-- Rader uten verdi ble ikke kopiert, siden de ikke har noe innslag å vise.
DELETE
FROM tiltaksdeltaker_endring
WHERE kilde = 'Tiltakshistorikk';
