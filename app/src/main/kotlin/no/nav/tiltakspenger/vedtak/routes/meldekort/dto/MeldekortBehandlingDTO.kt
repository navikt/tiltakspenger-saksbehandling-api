package no.nav.tiltakspenger.vedtak.routes.meldekort.dto

import no.nav.tiltakspenger.felles.Navkontor
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.meldekort.domene.MeldekortBehandling
import no.nav.tiltakspenger.meldekort.domene.MeldekortBehandlinger

data class MeldekortBehandlingDTO(
    val id: String,
    val saksbehandler: String?,
    val beslutter: String?,
    val status: MeldekortBehandlingStatusDTO,
    val totalbeløpTilUtbetaling: Int?,
    val navkontor: String?,
    val forrigeNavkontor: String?,
    val dager: List<MeldekortDagDTO>,
)

fun MeldekortBehandlinger.toDTO(forrigeNavkontor: (MeldekortId) -> Navkontor?): List<MeldekortBehandlingDTO> {
    return this.map {
        it.toDTO(
            forrigeNavkontor = forrigeNavkontor(it.id),
        )
    }
}

fun MeldekortBehandling.toDTO(forrigeNavkontor: Navkontor?): MeldekortBehandlingDTO {
    return MeldekortBehandlingDTO(
        id = id.toString(),
        saksbehandler = saksbehandler,
        beslutter = beslutter,
        status = this.toStatusDTO(),
        totalbeløpTilUtbetaling = this.beløpTotal,
        navkontor = navkontor?.kontornummer,
        forrigeNavkontor = forrigeNavkontor?.kontornummer,
        dager = beregning.toDTO(),
    )
}
