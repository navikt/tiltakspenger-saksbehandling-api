package no.nav.tiltakspenger.vedtak.barnetillegg

@JvmInline
value class AntallBarn(val value: Int) {
    init {
        require(value in 0..99) { "Antall barn må være et tall mellom 0 og 99" }
    }

    companion object {
        val ZERO = AntallBarn(0)
    }
}
