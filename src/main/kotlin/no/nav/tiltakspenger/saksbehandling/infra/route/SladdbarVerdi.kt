package no.nav.tiltakspenger.saksbehandling.infra.route

/**
 * Innpakning av en verdi i en utgående DTO som kan sladdes.
 * Sladdede felter får `{"verdi": null, "erSladdet": true}` i stedet for en erstatningstekst, slik at typen til verdien er den samme uansett om den er sladdet eller ikke.
 * En dato kan dermed være en dato i DTO-en, og frontenden slipper å kjenne igjen en magisk streng for å vite at noe er sladdet.
 * Motstykket i frontenden heter `SladdbarVerdi<T>` og har de samme to variantene.
 *
 * Typeargumentet er nullbart der feltet kan mangle ([SladdbarVerdi] av `String?`), slik at feltet selv aldri er null.
 * Frontenden kan da alltid lese `erSladdet` uten å først sjekke om feltet finnes, og en manglende verdi skiller seg fra en sladdet verdi.
 *
 * Typen er kun for utgående DTO-er og deserialiseres ikke.
 * DTO-er som brukes i begge retninger har en egen inngående variant med rå verdier.
 */
sealed interface SladdbarVerdi<out T> {
    val verdi: T?
    val erSladdet: Boolean
}

data class IkkeSladdetVerdi<out T>(override val verdi: T) : SladdbarVerdi<T> {
    override val erSladdet: Boolean = false
}

data object SladdetVerdi : SladdbarVerdi<Nothing> {
    override val verdi: Nothing? = null
    override val erSladdet: Boolean = true
}

fun <T> T.ikkeSladdet(): SladdbarVerdi<T> = IkkeSladdetVerdi(this)
