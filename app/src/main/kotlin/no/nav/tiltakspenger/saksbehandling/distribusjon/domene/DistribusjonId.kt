package no.nav.tiltakspenger.saksbehandling.distribusjon.domene

@JvmInline
value class DistribusjonId(
    private val value: String,
) {
    override fun toString() = value
}
