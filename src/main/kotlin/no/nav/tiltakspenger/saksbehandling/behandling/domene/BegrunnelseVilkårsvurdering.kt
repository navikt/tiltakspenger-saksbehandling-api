package no.nav.tiltakspenger.saksbehandling.behandling.domene

import no.nav.tiltakspenger.libs.common.SaniterStringForPdfgen.saniterBeholdNewline

@JvmInline
value class BegrunnelseVilkårsvurdering(
    val verdi: String,
) {
    /** Kan inneholde sensitiv informasjon, så vi ønsker ikke at denne havner tilfeldigvis i loggene. */
    override fun toString(): String {
        return "*****"
    }

    companion object {
        fun saniter(verdi: String): BegrunnelseVilkårsvurdering {
            return BegrunnelseVilkårsvurdering(saniterBeholdNewline(verdi))
        }
    }
}
