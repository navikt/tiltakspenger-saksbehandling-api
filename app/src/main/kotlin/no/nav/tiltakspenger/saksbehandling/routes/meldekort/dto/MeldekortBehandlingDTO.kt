package no.nav.tiltakspenger.saksbehandling.routes.meldekort.dto

import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandlinger

data class MeldekortBehandlingDTO(
    val id: String,
    val saksbehandler: String,
    val beslutter: String?,
    val status: MeldekortBehandlingStatusDTO,
    val totalbeløpTilUtbetaling: Int?,
    val totalOrdinærBeløpTilUtbetaling: Int?,
    val totalBarnetilleggTilUtbetaling: Int?,
    val navkontor: String,
    val navkontorNavn: String?,
    val dager: List<MeldekortDagDTO>,
    val brukersMeldekortId: String?,
    val type: MeldekortBehandlingTypeDTO,
)

fun MeldekortBehandlinger.toDTO(): List<MeldekortBehandlingDTO> {
    return this.map {
        it.toDTO()
    }
}

fun MeldekortBehandling.toDTO(): MeldekortBehandlingDTO {
    return MeldekortBehandlingDTO(
        id = id.toString(),
        saksbehandler = saksbehandler,
        beslutter = beslutter,
        status = this.toStatusDTO(),
        totalbeløpTilUtbetaling = this.beløpTotal,
        totalOrdinærBeløpTilUtbetaling = this.ordinærBeløp,
        totalBarnetilleggTilUtbetaling = this.barnetilleggBeløp,
        navkontor = navkontor.kontornummer,
        navkontorNavn = navkontor.kontornavn,
        dager = beregning.toDTO(),
        brukersMeldekortId = brukersMeldekort?.id.toString(),
        type = type.tilDTO(),
    )
}
