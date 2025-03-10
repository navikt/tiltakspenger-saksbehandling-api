package no.nav.tiltakspenger.vedtak.meldekort.domene

import java.time.LocalDate

data class Satsdag(
    val sats: Int,
    val satsRedusert: Int,
    val satsBarnetillegg: Int,
    val satsBarnetilleggRedusert: Int,
    val dato: LocalDate,
) {
    init {
        require(sats > 0)
        require(satsRedusert > 0)
        require(satsBarnetillegg > 0)
        require(satsBarnetilleggRedusert > 0)
    }
}
