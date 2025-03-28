package no.nav.tiltakspenger.saksbehandling.distribusjon

@JvmInline
value class DistribusjonId(
    private val value: String,
) {
    override fun toString() = value
}
