package no.nav.tiltakspenger.vedtak.distribusjon.domene

@JvmInline
value class DistribusjonId(
    private val value: String,
) {
    override fun toString() = value
}
