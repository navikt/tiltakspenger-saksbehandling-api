package no.nav.tiltakspenger.saksbehandling.meldekort.domene

import no.nav.tiltakspenger.libs.common.NonBlankString
import no.nav.tiltakspenger.libs.common.SaniterStringForPdfgen.saniterBeholdNewline

// TODO - denne burde flyttes til libs

@JvmInline
value class Begrunnelse private constructor(
    private val nonBlankString: NonBlankString,
) {
    val verdi: String get() = nonBlankString.value

    /** Kan inneholde sensitiv informasjon, så vi ønsker ikke at denne havner tilfeldigvis i loggene. */
    override fun toString(): String {
        return "*****"
    }

    companion object {
        fun create(verdi: String): Begrunnelse =
            Begrunnelse(NonBlankString.create(saniterBeholdNewline(verdi)))

        fun String.toBegrunnelse(): Begrunnelse = create(this)
    }
}
