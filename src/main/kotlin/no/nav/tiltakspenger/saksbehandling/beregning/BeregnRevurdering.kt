package no.nav.tiltakspenger.saksbehandling.beregning

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import arrow.core.toNonEmptyListOrThrow
import no.nav.tiltakspenger.libs.periodisering.tilPeriodisering
import no.nav.tiltakspenger.libs.tiltak.TiltakstypeSomGirRett
import no.nav.tiltakspenger.saksbehandling.barnetillegg.AntallBarn
import no.nav.tiltakspenger.saksbehandling.behandling.domene.RevurderingInnvilgelseTilBeslutningKommando
import no.nav.tiltakspenger.saksbehandling.behandling.domene.RevurderingResultat
import no.nav.tiltakspenger.saksbehandling.beregning.MeldeperiodeBeregningDag.Deltatt.DeltattMedLønnITiltaket
import no.nav.tiltakspenger.saksbehandling.beregning.MeldeperiodeBeregningDag.Deltatt.DeltattUtenLønnITiltaket
import no.nav.tiltakspenger.saksbehandling.beregning.MeldeperiodeBeregningDag.Fravær.Syk.SykBruker
import no.nav.tiltakspenger.saksbehandling.beregning.MeldeperiodeBeregningDag.Fravær.Syk.SyktBarn
import no.nav.tiltakspenger.saksbehandling.beregning.MeldeperiodeBeregningDag.Fravær.Velferd.FraværAnnet
import no.nav.tiltakspenger.saksbehandling.beregning.MeldeperiodeBeregningDag.Fravær.Velferd.FraværGodkjentAvNav
import no.nav.tiltakspenger.saksbehandling.beregning.MeldeperiodeBeregningDag.IkkeBesvart
import no.nav.tiltakspenger.saksbehandling.beregning.MeldeperiodeBeregningDag.IkkeDeltatt
import no.nav.tiltakspenger.saksbehandling.beregning.MeldeperiodeBeregningDag.IkkeRettTilTiltakspenger
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import java.lang.IllegalStateException
import java.time.LocalDate

fun Sak.beregnRevurderingInnvilgelse(
    kommando: RevurderingInnvilgelseTilBeslutningKommando,
): Either<RevurderingIkkeBeregnet, BehandlingBeregning> {
    val nyeBeregninger: List<Pair<MeldeperiodeBeregning, Int>> = beregnMeldeperioderPåNytt(kommando)

    if (nyeBeregninger.isEmpty()) {
        return RevurderingIkkeBeregnet.IngenEndring.left()
    }

    val beløpTotalDiff = nyeBeregninger.sumOf { it.second }

    if (beløpTotalDiff < 0) {
        return RevurderingIkkeBeregnet.StøtterIkkeTilbakekreving.left()
    }

    return BehandlingBeregning(
        beregninger = nyeBeregninger.map { it.first }.toNonEmptyListOrThrow(),
    ).right()
}

private fun Sak.beregnMeldeperioderPåNytt(
    kommando: RevurderingInnvilgelseTilBeslutningKommando,
): List<Pair<MeldeperiodeBeregning, Int>> {
    val behandlingId = kommando.behandlingId
    val behandling = hentBehandling(behandlingId)

    require(behandling?.resultat is RevurderingResultat.Innvilgelse) {
        "Behandlingen må være en revurdering av innvilgelse for å kunne beregnes"
    }

    val tidligereBeregninger = meldeperiodeBeregninger.sisteBeregningerForPeriode(kommando.innvilgelsesperiode)

    val antallBarnForDato: (dato: LocalDate) -> AntallBarn =
        kommando.barnetillegg?.periodisering?.let {
            { dato -> it.hentVerdiForDag(dato) ?: AntallBarn.ZERO }
        } ?: { AntallBarn.ZERO }

    val tiltakstypeForDato: (dato: LocalDate) -> TiltakstypeSomGirRett =
        kommando.tiltaksdeltakelser.tilPeriodisering().let { tiltaksdeltakelser ->
            { dato ->
                val deltagelseId = tiltaksdeltakelser.hentVerdiForDag(dato)
                    ?: throw IllegalStateException("Ingen tiltaksdeltagelse var satt for $dato")

                behandling.saksopplysninger.getTiltaksdeltagelse(deltagelseId)?.typeKode
                    ?: throw IllegalStateException("Fant ikke tiltaksdeltagelse i saksopplysninger for $deltagelseId")
            }
        }

    return tidligereBeregninger.mapNotNull { beregning ->
        val eksisterendeDager = beregning.dager

        val nyeDager = eksisterendeDager.map { dag ->
            val dato = dag.dato
            val reduksjon = dag.reduksjon

            val antallBarn = antallBarnForDato(dato)

            val tiltakstype: TiltakstypeSomGirRett by lazy {
                tiltakstypeForDato(dato)
            }

            when (dag) {
                is DeltattMedLønnITiltaket -> DeltattMedLønnITiltaket.create(dato, tiltakstype, antallBarn)
                is DeltattUtenLønnITiltaket -> DeltattUtenLønnITiltaket.create(dato, tiltakstype, antallBarn)
                is SykBruker -> SykBruker.create(dato, reduksjon, tiltakstype, antallBarn)
                is SyktBarn -> SyktBarn.create(dato, reduksjon, tiltakstype, antallBarn)
                is FraværAnnet -> FraværAnnet.create(dato, tiltakstype, antallBarn)
                is FraværGodkjentAvNav -> FraværGodkjentAvNav.create(dato, tiltakstype, antallBarn)
                is IkkeBesvart -> IkkeBesvart.create(dato, tiltakstype, antallBarn)
                is IkkeDeltatt -> IkkeDeltatt.create(dato, tiltakstype, antallBarn)
                is IkkeRettTilTiltakspenger -> IkkeRettTilTiltakspenger(dato)
            }
        }

        if (nyeDager == eksisterendeDager) {
            return@mapNotNull null
        }

        val nyBeregning = MeldeperiodeBeregning(
            id = BeregningId.random(),
            dager = nyeDager,
            meldekortId = beregning.meldekortId,
            kjedeId = beregning.kjedeId,
            beregningKilde = BeregningKilde.Behandling(behandlingId),
        )

        val beløpDiff = nyBeregning.totalBeløp - beregning.totalBeløp

        nyBeregning to beløpDiff
    }
}

sealed interface RevurderingIkkeBeregnet {
    data object IngenEndring : RevurderingIkkeBeregnet
    data object StøtterIkkeTilbakekreving : RevurderingIkkeBeregnet
}
