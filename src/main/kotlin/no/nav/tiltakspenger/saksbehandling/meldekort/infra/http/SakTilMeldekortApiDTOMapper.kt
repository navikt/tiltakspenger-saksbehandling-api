package no.nav.tiltakspenger.saksbehandling.meldekort.infra.http

import no.nav.tiltakspenger.libs.meldekort.SakTilMeldekortApiDTO
import no.nav.tiltakspenger.libs.meldekort.SakTilMeldekortApiDTO.MeldekortvedtakDTO.Reduksjon
import no.nav.tiltakspenger.libs.meldekort.SakTilMeldekortApiDTO.MeldekortvedtakDTO.Status
import no.nav.tiltakspenger.libs.periode.toDTO
import no.nav.tiltakspenger.saksbehandling.beregning.MeldeperiodeBeregningDag
import no.nav.tiltakspenger.saksbehandling.beregning.MeldeperiodeBeregningDag.Deltatt.DeltattMedLønnITiltaket
import no.nav.tiltakspenger.saksbehandling.beregning.MeldeperiodeBeregningDag.Deltatt.DeltattUtenLønnITiltaket
import no.nav.tiltakspenger.saksbehandling.beregning.MeldeperiodeBeregningDag.Fravær.Syk.SykBruker
import no.nav.tiltakspenger.saksbehandling.beregning.MeldeperiodeBeregningDag.Fravær.Syk.SyktBarn
import no.nav.tiltakspenger.saksbehandling.beregning.MeldeperiodeBeregningDag.Fravær.Velferd.FraværAnnet
import no.nav.tiltakspenger.saksbehandling.beregning.MeldeperiodeBeregningDag.Fravær.Velferd.FraværGodkjentAvNav
import no.nav.tiltakspenger.saksbehandling.beregning.MeldeperiodeBeregningDag.Fravær.Velferd.FraværSterkeVelferdsgrunnerEllerJobbintervju
import no.nav.tiltakspenger.saksbehandling.beregning.MeldeperiodeBeregningDag.IkkeBesvart
import no.nav.tiltakspenger.saksbehandling.beregning.MeldeperiodeBeregningDag.IkkeDeltatt
import no.nav.tiltakspenger.saksbehandling.beregning.MeldeperiodeBeregningDag.IkkeRettTilTiltakspenger
import no.nav.tiltakspenger.saksbehandling.beregning.ReduksjonAvYtelsePåGrunnAvFravær
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortvedtak.Meldekortvedtak
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldeperiode.Meldeperiode
import no.nav.tiltakspenger.saksbehandling.sak.Sak

fun Sak.tilMeldekortApiDTO(): SakTilMeldekortApiDTO {
    return SakTilMeldekortApiDTO(
        fnr = this.fnr.verdi,
        sakId = this.id.toString(),
        saksnummer = this.saksnummer.toString(),
        meldeperioder = this.meldeperiodeKjeder.sisteMeldeperiodePerKjede.map { it.tilMeldekortApiDTO() },
        harSoknadUnderBehandling = this.harSoknadUnderBehandling(),
        kanSendeInnHelgForMeldekort = this.kanSendeInnHelgForMeldekort,
        meldekortvedtak = this.meldekortvedtaksliste.map { it.tilMeldekortApiDTO() },
    )
}

private fun Meldeperiode.tilMeldekortApiDTO(): SakTilMeldekortApiDTO.MeldeperiodeDTO {
    return SakTilMeldekortApiDTO.MeldeperiodeDTO(
        id = this.id.toString(),
        kjedeId = this.kjedeId.toString(),
        versjon = this.versjon.value,
        opprettet = this.opprettet,
        periodeDTO = this.periode.toDTO(),
        antallDagerForPeriode = this.maksAntallDagerForMeldeperiode,
        girRett = this.girRett,
    )
}

private fun Meldekortvedtak.tilMeldekortApiDTO(): SakTilMeldekortApiDTO.MeldekortvedtakDTO {
    return SakTilMeldekortApiDTO.MeldekortvedtakDTO(
        id = this.id.toString(),
        opprettet = this.opprettet,
        erKorrigering = this.erKorrigering,
        erAutomatiskBehandlet = this.erAutomatiskBehandlet,
        meldeperiodebehandlinger = meldeperiodeberegninger.map { (behandling, beregning) ->
            SakTilMeldekortApiDTO.MeldekortvedtakDTO.MeldeperiodebehandlingDTO(
                meldeperiodeId = behandling.meldeperiodeId.toString(),
                meldeperiodeKjedeId = behandling.kjedeId.toString(),
                brukersMeldekortId = behandling.brukersMeldekort?.id?.toString(),
                periodeDTO = behandling.periode.toDTO(),
                dager = beregning.dager.map { it.tilMeldekortApiDTO() },
            )
        },
    )
}

private fun MeldeperiodeBeregningDag.tilMeldekortApiDTO(): SakTilMeldekortApiDTO.MeldekortvedtakDTO.MeldeperiodebehandlingDTO.DagDTO {
    return SakTilMeldekortApiDTO.MeldekortvedtakDTO.MeldeperiodebehandlingDTO.DagDTO(
        dato = this.dato,
        status = this.tilMeldekortApiStatus(),
        reduksjon = this.reduksjon.tilMeldekortApiDTO(),
        beløp = this.beløp,
        beløpBarnetillegg = this.beløpBarnetillegg,
    )
}

private fun MeldeperiodeBeregningDag.tilMeldekortApiStatus(): Status {
    return when (this) {
        is DeltattUtenLønnITiltaket -> Status.DELTATT_UTEN_LØNN_I_TILTAKET
        is DeltattMedLønnITiltaket -> Status.DELTATT_MED_LØNN_I_TILTAKET
        is SykBruker -> Status.FRAVÆR_SYK
        is SyktBarn -> Status.FRAVÆR_SYKT_BARN
        is FraværGodkjentAvNav -> Status.FRAVÆR_GODKJENT_AV_NAV
        is FraværSterkeVelferdsgrunnerEllerJobbintervju -> Status.FRAVÆR_STERKE_VELFERDSGRUNNER_ELLER_JOBBINTERVJU
        is FraværAnnet -> Status.FRAVÆR_ANNET
        is IkkeBesvart -> Status.IKKE_BESVART
        is IkkeDeltatt -> Status.IKKE_TILTAKSDAG
        is IkkeRettTilTiltakspenger -> Status.IKKE_RETT_TIL_TILTAKSPENGER
    }
}

private fun ReduksjonAvYtelsePåGrunnAvFravær.tilMeldekortApiDTO(): Reduksjon {
    return when (this) {
        ReduksjonAvYtelsePåGrunnAvFravær.IngenReduksjon -> Reduksjon.INGEN_REDUKSJON
        ReduksjonAvYtelsePåGrunnAvFravær.Reduksjon -> Reduksjon.REDUKSJON
        ReduksjonAvYtelsePåGrunnAvFravær.YtelsenFallerBort -> Reduksjon.YTELSEN_FALLER_BORT
    }
}
