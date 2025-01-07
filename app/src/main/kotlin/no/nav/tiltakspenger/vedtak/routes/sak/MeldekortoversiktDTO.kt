package no.nav.tiltakspenger.vedtak.routes.sak

import no.nav.tiltakspenger.meldekort.domene.MeldekortBehandling
import no.nav.tiltakspenger.vedtak.routes.dto.PeriodeDTO
import no.nav.tiltakspenger.vedtak.routes.dto.toDTO
import no.nav.tiltakspenger.vedtak.routes.meldekort.dto.MeldekortstatusDTO
import no.nav.tiltakspenger.vedtak.routes.meldekort.dto.toMeldekortstatusDTO

data class MeldekortoversiktDTO(
    val meldekortId: String,
    val periode: PeriodeDTO,
    val status: MeldekortstatusDTO,
    val saksbehandler: String?,
    val beslutter: String?,
)

fun List<MeldekortBehandling>.toMeldekortoversiktDTO(): List<MeldekortoversiktDTO> =
    this.map { it.toOversiktDTO() }

fun MeldekortBehandling.toOversiktDTO(): MeldekortoversiktDTO {
    return MeldekortoversiktDTO(
        meldekortId = id.toString(),
        periode = periode.toDTO(),
        status = this.toMeldekortstatusDTO(),
        saksbehandler = saksbehandler,
        beslutter = beslutter,
    )
}
