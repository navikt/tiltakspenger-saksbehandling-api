package no.nav.tiltakspenger.saksbehandling.routes.meldekort.dto

import no.nav.tiltakspenger.saksbehandling.meldekort.domene.BrukersMeldekort
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.InnmeldtStatus
import java.time.LocalDate
import java.time.LocalDateTime

enum class InnmeldtStatusDTO {
    DELTATT,
    FRAVÆR_SYK,
    FRAVÆR_SYKT_BARN,
    FRAVÆR_ANNET,
    IKKE_REGISTRERT,
    IKKE_DELTATT,
    IKKE_RETT_TIL_TILTAKSPENGER,
}

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
    InnmeldtStatus.DELTATT -> InnmeldtStatusDTO.DELTATT
    InnmeldtStatus.FRAVÆR_SYK -> InnmeldtStatusDTO.FRAVÆR_SYK
    InnmeldtStatus.FRAVÆR_SYKT_BARN -> InnmeldtStatusDTO.FRAVÆR_SYKT_BARN
    InnmeldtStatus.FRAVÆR_ANNET -> InnmeldtStatusDTO.FRAVÆR_ANNET
    InnmeldtStatus.IKKE_REGISTRERT -> InnmeldtStatusDTO.IKKE_REGISTRERT
    InnmeldtStatus.IKKE_DELTATT -> InnmeldtStatusDTO.IKKE_DELTATT
    InnmeldtStatus.IKKE_RETT_TIL_TILTAKSPENGER -> InnmeldtStatusDTO.IKKE_RETT_TIL_TILTAKSPENGER
}.toString()
