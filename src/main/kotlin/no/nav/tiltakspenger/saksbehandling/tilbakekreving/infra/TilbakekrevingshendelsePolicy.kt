package no.nav.tiltakspenger.saksbehandling.tilbakekreving.infra

/**
 * Hva appen gjør med tilbakekrevingshendelser den ikke kan behandle.
 *
 * @property testsaksnummerPrefiks hendelser med ugyldig saksnummer som starter slik, slettes, og null betyr at ingen slettes på grunn av prefikset.
 * @property slettHendelserMedUkjentSaksnummer sletter hendelser med gyldig saksnummer som vi ikke har noen sak for, i stedet for å markere dem med feil.
 * @property slettUleseligeHendelser sletter ukjente hendelser som fortsatt ikke kan deserialiseres, i stedet for å beholde dem til DTO-en er rettet.
 * @property loggDeserialiseringsfeilSomError logger deserialiseringsfeil som ERROR med stacktrace, ellers som INFO uten stacktrace.
 */
data class TilbakekrevingshendelsePolicy(
    val testsaksnummerPrefiks: String?,
    val slettHendelserMedUkjentSaksnummer: Boolean,
    val slettUleseligeHendelser: Boolean,
    val loggDeserialiseringsfeilSomError: Boolean,
) {
    companion object {
        /** Sletter ingenting, og logger deserialiseringsfeil som ERROR. */
        val BeholdAlle = TilbakekrevingshendelsePolicy(
            testsaksnummerPrefiks = null,
            slettHendelserMedUkjentSaksnummer = false,
            slettUleseligeHendelser = false,
            loggDeserialiseringsfeilSomError = true,
        )
    }
}
