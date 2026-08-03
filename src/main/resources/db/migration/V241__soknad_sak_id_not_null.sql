-- `søknad.sak_id` har vært nullable siden V1, men prodkoden kan ikke skrive NULL:
-- eneste skrivesti setter kolonnen fra `Søknad.sakId`, som er non-null i domenemodellen.
--
-- Nullbarheten var likevel ikke gratis. `SøknadDAO.finnSakIdForTiltaksdeltakelse` leste kolonnen
-- som nullable, og en NULL ga samme svar som "fant ingen søknad for denne deltakeren".
-- `TiltaksdeltakerService` forkaster da hendelsen fra Arena, Komet eller TeamTiltak med kun en
-- info-linje i loggen. En rad uten sak ville altså blitt en stille datamangel, ikke en feil.
--
-- Med constrainten på plass leses kolonnen som non-null, og de to tilstandene kan ikke lenger
-- forveksles.
--
-- Verifisert før migreringen ble skrevet: `select count(*) from søknad where sak_id is null`
-- ga 0 i både dev og prod (2026-08-02).
--
-- Skulle det likevel finnes slike rader, feiler migreringen og stopper deployen framfor å
-- endre noe. Det er den ønskede oppførselen.
ALTER TABLE søknad
    ALTER COLUMN sak_id SET NOT NULL;
