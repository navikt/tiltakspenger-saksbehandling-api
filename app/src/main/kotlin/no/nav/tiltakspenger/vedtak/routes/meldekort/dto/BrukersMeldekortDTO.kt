package no.nav.tiltakspenger.vedtak.routes.meldekort.dto

import no.nav.tiltakspenger.meldekort.domene.BrukersMeldekort
import no.nav.tiltakspenger.meldekort.domene.InnmeldtStatus
import java.time.LocalDate
import java.time.LocalDateTime

data class BrukersMeldekortDTO(
    val id: String,
    val mottatt: LocalDateTime,
    val dager: List<DagDTO>,
) {
    data class DagDTO(
        val status: String,
        val dato: LocalDate,
    )
}

fun BrukersMeldekort.toDTO(): BrukersMeldekortDTO {
    return BrukersMeldekortDTO(
        id = id.toString(),
        mottatt = mottatt,
        dager = dager.map {
            BrukersMeldekortDTO.DagDTO(
                status = it.status.toDTO(),
                dato = it.dato,
            )
        },
    )
}

fun InnmeldtStatus.toDTO(): String = when (this) {
    InnmeldtStatus.DELTATT -> "DELTATT"
    InnmeldtStatus.FRAVÆR_SYK -> "FRAVÆR_SYK"
    InnmeldtStatus.FRAVÆR_SYKT_BARN -> "FRAVÆR_SYKT_BARN"
    InnmeldtStatus.FRAVÆR_ANNET -> "FRAVÆR_ANNET"
    InnmeldtStatus.IKKE_REGISTRERT -> "IKKE_REGISTRERT"
    InnmeldtStatus.IKKE_DELTATT -> "IKKE_DELTATT"
    InnmeldtStatus.IKKE_RETT_TIL_TILTAKSPENGER -> "IKKE_RETT_TIL_TILTAKSPENGER"
}
