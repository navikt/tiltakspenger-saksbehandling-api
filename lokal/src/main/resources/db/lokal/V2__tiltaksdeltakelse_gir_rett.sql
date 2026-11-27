-- Kun for lokal kjøring (LokalMain), se LokalFlywayMigrate.kt.
-- Skiller deltakelser som gir rett (TiltaksdeltakelseDb) fra lønnstilskudd (TiltaksdeltakelseSomIkkeGirRettDbJson), som lagres med hvert sitt json-format.
alter table lokal.tiltaksdeltakelse
    add column gir_rett boolean not null default true;
