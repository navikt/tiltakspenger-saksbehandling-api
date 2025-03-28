package no.nav.tiltakspenger.saksbehandling.behandling.domene.behandling

@JvmInline
value class BegrunnelseVilkårsvurdering(
    val verdi: String,
) {
    /** Kan inneholde sensitiv informasjon, så vi ønsker ikke at denne havner tilfeldigvis i loggene. */
    override fun toString(): String {
        return "*****"
    }
}
