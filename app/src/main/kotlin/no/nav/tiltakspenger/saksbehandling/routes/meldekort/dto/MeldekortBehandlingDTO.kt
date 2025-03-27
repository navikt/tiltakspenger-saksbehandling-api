package no.nav.tiltakspenger.saksbehandling.routes.meldekort.dto

import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandlinger
import no.nav.tiltakspenger.saksbehandling.routes.behandling.dto.AttesteringDTO
import no.nav.tiltakspenger.saksbehandling.routes.behandling.dto.toDTO

data class MeldekortBehandlingDTO(
    val id: String,
    val meldeperiodeId: String,
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
    val begrunnelse: String?,
    val attesteringer: List<AttesteringDTO>,
)

fun MeldekortBehandlinger.toDTO(): List<MeldekortBehandlingDTO> {
    return this.map {
        it.toDTO()
    }
}

fun MeldekortBehandling.toDTO(): MeldekortBehandlingDTO {
    return MeldekortBehandlingDTO(
        id = id.toString(),
        meldeperiodeId = meldeperiode.id.toString(),
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
        begrunnelse = begrunnelse?.verdi,
        attesteringer = attesteringer.toDTO(),
    )
}
