package no.nav.tiltakspenger.vedtak.routes.meldekort.dto

import no.nav.tiltakspenger.meldekort.domene.BrukersMeldekort
import no.nav.tiltakspenger.meldekort.domene.InnmeldtStatus
import java.time.LocalDate
import java.time.LocalDateTime

data class BrukersMeldekortDTO(
    val id: String,
    val mottatt: LocalDateTime,
    val meldeperiode: MeldeperiodeDTO,
    val sakId: String,
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
        meldeperiode = meldeperiode.toDTO(),
        sakId = sakId.toString(),
        dager = dager.map {
            BrukersMeldekortDTO.DagDTO(
                status = when (it.status) {
                    InnmeldtStatus.DELTATT -> "DELTATT"
                    InnmeldtStatus.FRAVÆR_SYK -> "FRAVÆR_SYK"
                    InnmeldtStatus.FRAVÆR_SYKT_BARN -> "FRAVÆR_SYKT_BARN"
                    InnmeldtStatus.FRAVÆR_ANNET -> "FRAVÆR_ANNET"
                    InnmeldtStatus.IKKE_REGISTRERT -> "IKKE_REGISTRERT"
                },
                dato = it.dato,
            )
        },
    )
}
