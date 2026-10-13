package no.nav.tiltakspenger.saksbehandling.søknad.domene

/**
 * Hvor barnet oppholder seg i tiltaksperioden, slik det er oppgitt i søknaden.
 * Erstatter ja/nei-spørsmålet om barnet oppholder seg i Norge eller et annet EØS-land.
 * Søknader fra før dette ble spurt om har bare ja/nei-svaret, se [BarnetilleggFraSøknad.oppholderSegIEØS].
 */
enum class Oppholdssted {
    NORGE,
    ANNET_EØS_LAND,
    IKKE_EØS_LAND,
    ;

    fun tilOppholderSegIEØS(): Søknad.JaNeiSpm =
        when (this) {
            NORGE, ANNET_EØS_LAND -> Søknad.JaNeiSpm.Ja
            IKKE_EØS_LAND -> Søknad.JaNeiSpm.Nei
        }
}
