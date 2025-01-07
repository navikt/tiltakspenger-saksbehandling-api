package no.nav.tiltakspenger.vedtak.routes.meldekort.dto

import no.nav.tiltakspenger.felles.Navkontor
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.meldekort.domene.MeldekortBehandling
import no.nav.tiltakspenger.vedtak.routes.dto.PeriodeDTO
import no.nav.tiltakspenger.vedtak.routes.dto.toDTO

data class MeldekortDTO(
    val id: String,
    val sakId: String,
    val saksnummer: String,
    val periode: PeriodeDTO,
    val meldekortDager: List<MeldekortDagDTO>,
    val tiltaksnavn: String,
    val saksbehandler: String?,
    val beslutter: String?,
    val status: MeldekortstatusDTO,
    val totalbeløpTilUtbetaling: Int?,
    val vedtaksPeriode: PeriodeDTO,
    val antallDager: Int,
    val navkontor: String?,
    val forrigeNavkontor: String?,
)

fun MeldekortBehandling.toDTO(
    vedtaksPeriode: Periode,
    tiltaksnavn: String,
    antallDager: Int,
    forrigeNavkontor: Navkontor?,
): MeldekortDTO {
    return MeldekortDTO(
        id = id.toString(),
        sakId = sakId.toString(),
        saksnummer = saksnummer.toString(),
        periode = periode.toDTO(),
        saksbehandler = saksbehandler,
        beslutter = beslutter,
        tiltaksnavn = tiltaksnavn,
        status = this.toMeldekortstatusDTO(),
        meldekortDager = beregning.toDTO(),
        totalbeløpTilUtbetaling = this.beløpTotal,
        vedtaksPeriode = vedtaksPeriode.toDTO(),
        antallDager = antallDager,
        navkontor = navkontor?.kontornummer,
        forrigeNavkontor = forrigeNavkontor?.kontornummer,
    )
}
