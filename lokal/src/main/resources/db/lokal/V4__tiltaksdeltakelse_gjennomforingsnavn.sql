-- Kun for lokal kjøring (LokalMain), se LokalFlywayMigrate.kt.
-- Gjennomføringsnavnet lagres ikke i saksopplysningene, men slås opp hos kilden ved behov, så den lokale kilden må ha det i en egen kolonne.
alter table lokal.tiltaksdeltakelse
    add column gjennomforingsnavn text;
