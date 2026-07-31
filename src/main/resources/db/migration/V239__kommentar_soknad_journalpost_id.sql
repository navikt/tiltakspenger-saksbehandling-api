-- Dokumenterer at søknad.journalpost_id bevisst ikke er unik.
--
-- Kolonnen ser ut som en naturlig nøkkel, og har blitt foreslått som kandidat for en unik
-- constraint mer enn én gang. Den skal ikke ha en: én journalpost kan gi flere søknader,
-- og det er en normal situasjon, ikke en feil.
--
-- To måter det skjer på:
--   * Samme søknad vurderes flere ganger hos oss.
--   * Søknaden registreres manuelt på nytt, f.eks. etter en tastefeil forrige gang.
--     StartBehandlingAvManueltRegistrertSøknadService lar saksbehandler oppgi journalpostId
--     selv, og ValiderJournalpostService sjekker kun at journalposten finnes, har datoOpprettet
--     og gjelder riktig person.
--
-- Avstanden mellom to søknader på samme journalpost kan være ti år eller mer i spesielle
-- tilfeller, så «avbrutt frigir journalposten» holder heller ikke som avgrensning — et partielt
-- unikt indeks `where avbrutt is null` ville vært feil av samme grunn som et fullt.
--
-- Ingen kode antar unikhet her i dag, og ingen skal begynne å gjøre det: et oppslag på
-- journalpost_id må tåle å få flere rader.

COMMENT ON COLUMN søknad.journalpost_id IS
    'Bevisst ikke unik. Én journalpost kan gi flere søknader - samme søknad kan vurderes flere ganger, og en søknad kan registreres manuelt på nytt. Det kan gå ti år eller mer mellom dem. Oppslag på denne kolonnen må tåle flere rader.';
