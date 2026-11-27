-- Kun for lokal kjøring (LokalMain), se LokalFlywayMigrate.kt.
-- Tittelen lagres ikke lenger i saksopplysningene, men slås opp hos kilden ved behov, så den lokale kilden må ha den i en egen kolonne.
-- Titler satt før dette flyttes ut av json-en.
alter table lokal.tiltaksdeltakelse
    add column tittel text;

update lokal.tiltaksdeltakelse
set tittel   = deltakelse ->> 'tittel',
    deltakelse = deltakelse - 'tittel';
