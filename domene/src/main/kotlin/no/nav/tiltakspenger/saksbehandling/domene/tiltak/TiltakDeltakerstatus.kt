package no.nav.tiltakspenger.saksbehandling.domene.tiltak

/**
 * Skal ikke serialiseres direkte.
 * Hvis vi ønsker denne i domenet på tvers av appene våre kan denne flyttes til libs.
 * Doc: https://confluence.adeo.no/pages/viewpage.action?pageId=573710206
 */
enum class TiltakDeltakerstatus {
    /**
     * Brukes både ved løpende inntak og kurs.
     * Brukeren er tildelt plass (vedtak fattet), men ennå ikke startet
     */
    VenterPåOppstart(),

    /**
     * Brukes både ved løpende inntak og kurs.
     * Brukeren er tildelt plass (vedtak fattet), deltar nå.
     */
    Deltar(),

    /**
     * Brukes ved løpende inntak.
     * Brukeren har deltatt på tiltaket (minst en dag), og så sluttet.
     */
    HarSluttet(),

    /**
     * Brukes ved kurs.
     * Deltakere som har deltatt på kurs, men sluttet før kurset er ferdig. (deltakers sluttdato er før kursets sluttdato)
     */
    Avbrutt(),

    /**
     * Brukes ved kurs.
     * Deltakere som har deltatt og fullfører kurs (deltakers sluttdato er lik kursets sluttdato).
     */
    Fullført(),

    /**
     * Brukes både ved løpende inntak og kurs.
     * Når brukeren har vært vurdert for tiltaket, men aldri startet og skal ikke delta.
     */
    IkkeAktuell(),

    /**
     * Brukes både ved løpende inntak og kurs.
     * Feilregistrering. Brukeren skal ikke delta på tiltaket.
     */
    Feilregistrert(),

    /**
     * Brukes ved løpende inntak.
     * Når veileder har startet prosessen med å melde på og sendt et forslag til påmelding til bruker.
     * Skal utfases.
     */
    PåbegyntRegistrering(),

    /**
     * Arenastatus: Aktuell.
     * Tror den har blitt navngitt Søkt om plass.
     *
     * Når veileder har meldt en bruker som interessert, men prosessen med utvelgelse ikke er startet på.
     */
    SøktInn(),

    /**
     * Brukes både ved løpende inntak og kurs.
     * Når deltakeren er vurdert til å være kvalifisert, men ikke er blitt tildelt plass
     */
    Venteliste(),

    /**
     * Brukes både ved løpende inntak og kurs.
     * Når tiltaksarrangør skal vurdere hvilke deltakere som er kvalifisert
     */
    Vurderes(),
}
