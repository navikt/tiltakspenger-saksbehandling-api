package no.nav.tiltakspenger.saksbehandling.felles

import no.nav.tiltakspenger.libs.common.NonBlankString
import no.nav.tiltakspenger.libs.common.SaniterStringForPdfgen
import org.jetbrains.annotations.TestOnly

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
        /** @return null dersom strengen er blank */
        fun create(verdi: String): Begrunnelse? {
            if (verdi.isBlank()) return null
            return Begrunnelse(NonBlankString.create(SaniterStringForPdfgen.saniterBeholdNewline(verdi)))
        }

        @TestOnly
        fun createOrThrow(verdi: String): Begrunnelse = create(verdi)!!

        /** @return null dersom strengen er blank */
        fun String.toBegrunnelse(): Begrunnelse? = create(this)
    }
}
