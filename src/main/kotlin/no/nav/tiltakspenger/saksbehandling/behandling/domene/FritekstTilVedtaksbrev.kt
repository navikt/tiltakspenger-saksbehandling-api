package no.nav.tiltakspenger.saksbehandling.behandling.domene

@JvmInline
value class FritekstTilVedtaksbrev(
    val verdi: String,
) {
    /** Kan inneholde sensitiv informasjon, så vi ønsker ikke at denne havner tilfeldigvis i loggene. */
    override fun toString(): String {
        return "*****"
    }
}
