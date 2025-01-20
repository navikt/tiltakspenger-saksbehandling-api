package no.nav.tiltakspenger.vedtak.routes.meldekort.dto

import no.nav.tiltakspenger.felles.Navkontor
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.meldekort.domene.MeldekortBehandling
import no.nav.tiltakspenger.meldekort.domene.MeldekortBehandlinger
import no.nav.tiltakspenger.vedtak.routes.dto.PeriodeDTO
import no.nav.tiltakspenger.vedtak.routes.dto.toDTO

data class MeldekortBehandlingDTO(
    val id: String,
    val sakId: String,
    val saksnummer: String,
    val periode: PeriodeDTO,
    val meldekortDager: List<MeldekortDagDTO>,
    val tiltaksnavn: String?,
    val saksbehandler: String?,
    val beslutter: String?,
    val status: MeldekortstatusDTO,
    val totalbeløpTilUtbetaling: Int?,
    val vedtaksPeriode: PeriodeDTO?,
    val antallDager: Int?,
    val navkontor: String?,
    val forrigeNavkontor: String?,
    val meldeperiode: MeldeperiodeDTO?,
)

fun MeldekortBehandlinger.toDTO(
    vedtaksPeriode: Periode?,
    tiltaksnavn: String?,
    antallDager: Int?,
    forrigeNavkontor: (MeldekortId) -> Navkontor?,
): List<MeldekortBehandlingDTO> {
    return this.map {
        it.toDTO(
            vedtaksPeriode = vedtaksPeriode,
            tiltaksnavn = tiltaksnavn,
            antallDager = antallDager,
            forrigeNavkontor = forrigeNavkontor(it.id),
        )
    }
}

fun MeldekortBehandling.toDTO(
    vedtaksPeriode: Periode?,
    tiltaksnavn: String?,
    antallDager: Int?,
    forrigeNavkontor: Navkontor?,
): MeldekortBehandlingDTO {
    return MeldekortBehandlingDTO(
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
        vedtaksPeriode = vedtaksPeriode?.toDTO(),
        antallDager = antallDager,
        navkontor = navkontor?.kontornummer,
        forrigeNavkontor = forrigeNavkontor?.kontornummer,
        meldeperiode = meldeperiode.toDTO(),
    )
}
