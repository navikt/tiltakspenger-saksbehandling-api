package no.nav.tiltakspenger.saksbehandling.behandling.domene

import no.nav.tiltakspenger.libs.common.NonBlankString
import no.nav.tiltakspenger.libs.common.SaniterStringForPdfgen.saniterBeholdNewline

@JvmInline
value class FritekstTilVedtaksbrev private constructor(
    private val nonBlankString: NonBlankString,
) {
    val verdi: String get() = nonBlankString.value

    /** Kan inneholde sensitiv informasjon, så vi ønsker ikke at denne havner tilfeldigvis i loggene. */
    override fun toString(): String {
        return "*****"
    }

    companion object {
        fun create(verdi: String): FritekstTilVedtaksbrev {
            return FritekstTilVedtaksbrev(NonBlankString.create(saniterBeholdNewline(verdi)))
        }

        fun String.toFritekstTilVedtaksbrev(): FritekstTilVedtaksbrev = create(this)
    }
}
