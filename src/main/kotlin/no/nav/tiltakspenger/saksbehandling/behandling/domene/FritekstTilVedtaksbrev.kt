package no.nav.tiltakspenger.saksbehandling.behandling.domene

import no.nav.tiltakspenger.libs.common.NonBlankString
import no.nav.tiltakspenger.libs.common.SaniterStringForPdfgen.saniterBeholdNewline
import org.jetbrains.annotations.TestOnly

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
        /** @return null dersom strengen er blank */
        fun create(verdi: String): FritekstTilVedtaksbrev? {
            if (verdi.isBlank()) return null
            return FritekstTilVedtaksbrev(NonBlankString.create(saniterBeholdNewline(verdi)))
        }

        @TestOnly
        fun createOrThrow(verdi: String): FritekstTilVedtaksbrev = create(verdi)!!

        /** @return null dersom strengen er blank */
        fun String.toFritekstTilVedtaksbrev(): FritekstTilVedtaksbrev? = create(this)

        @TestOnly
        fun String.toFritekstTilVedtaksbrevOrThrow(): FritekstTilVedtaksbrev = create(this)!!
    }
}
