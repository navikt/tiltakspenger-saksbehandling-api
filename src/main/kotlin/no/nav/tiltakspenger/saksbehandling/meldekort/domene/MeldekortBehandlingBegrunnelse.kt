package no.nav.tiltakspenger.saksbehandling.meldekort.domene

import no.nav.tiltakspenger.libs.common.SaniterStringForPdfgen.saniterBeholdNewline

@JvmInline
value class MeldekortBehandlingBegrunnelse(
    val verdi: String,
) {
    override fun toString(): String {
        return "*****"
    }

    companion object {
        fun saniter(verdi: String): MeldekortBehandlingBegrunnelse {
            return MeldekortBehandlingBegrunnelse(saniterBeholdNewline(verdi))
        }
    }
}
