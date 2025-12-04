/*
  Denne kolonnen var ikke i bruk. Alle felter sjekket til å være null i prod 4.desember 2025.
 */
ALTER TABLE behandling
    DROP COLUMN IF EXISTS tilleggstekst_brev;


