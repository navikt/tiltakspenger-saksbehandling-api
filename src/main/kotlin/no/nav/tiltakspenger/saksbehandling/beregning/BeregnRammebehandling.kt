package no.nav.tiltakspenger.saksbehandling.beregning

import arrow.core.NonEmptyList
import arrow.core.toNonEmptyListOrNull
import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.saksbehandling.barnetillegg.AntallBarn
import no.nav.tiltakspenger.saksbehandling.barnetillegg.Barnetillegg
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
import java.time.LocalDate

/**
 * @param behandlingId Søknadsbehandling eller revurdering.
 * @param virkningsperiode innvilgelseperiode ved innvilgelse (søknadsbehandling/endrings-revurdering). Stansperiode ved stans.
 * @param barnetillegg Kan være null dersom det ikke er noe barnetillegg.
 */
fun Sak.beregnInnvilgelse(
    behandlingId: BehandlingId,
    virkningsperiode: Periode,
    barnetillegg: Barnetillegg,
): Beregning? {
    return beregnMeldeperioderPåNytt(
        behandlingId = behandlingId,
        virkningsperiode = virkningsperiode,
        barnetillegg = barnetillegg,
    )?.let {
        Beregning(beregninger = it)
    }
}

private fun Sak.beregnMeldeperioderPåNytt(
    behandlingId: BehandlingId,
    virkningsperiode: Periode,
    barnetillegg: Barnetillegg?,
): NonEmptyList<MeldeperiodeBeregning>? {
    if (!utbetalinger.harUtbetalingIPeriode(virkningsperiode)) {
        // Hvis vi ikke har utbetalt noe i denne perioden, trenger vi ikke beregne/simulere.
        return null
    }

    val tidligereBeregninger = this.meldeperiodeBeregninger.sisteBeregningerForPeriode(virkningsperiode)

    val antallBarnForDato: (dato: LocalDate) -> AntallBarn =
        barnetillegg?.periodisering?.let {
            { dato -> it.hentVerdiForDag(dato) ?: AntallBarn.ZERO }
        } ?: { AntallBarn.ZERO }

    val nyeBeregninger =
        tidligereBeregninger.fold(emptyList<MeldeperiodeBeregning>()) { acc, beregning ->
            val eksisterendeDager = beregning.dager

            val nyeDager = eksisterendeDager.map { dag ->
                val dato = dag.dato

                if (!virkningsperiode.inneholder(dato)) {
                    return@map dag
                }

                val reduksjon = dag.reduksjon

                val antallBarn = antallBarnForDato(dato)

                // Vi bruker den tidligere beregningen sin tiltakstype (OS har uansett ikke støtte for å endre den for utbetalte dager) og helved sender den ikke videre i disse tilfellen.
                // Husk og oppdater denne dersom OS + helved legger in støtte for det.
                when (dag) {
                    is DeltattMedLønnITiltaket -> DeltattMedLønnITiltaket.create(dato, dag.tiltakstype, antallBarn)
                    is DeltattUtenLønnITiltaket -> DeltattUtenLønnITiltaket.create(
                        dato,
                        dag.tiltakstype,
                        antallBarn,
                    )

                    is SykBruker -> SykBruker.create(dato, reduksjon, dag.tiltakstype, antallBarn)
                    is SyktBarn -> SyktBarn.create(dato, reduksjon, dag.tiltakstype, antallBarn)
                    is FraværAnnet -> FraværAnnet.create(dato, dag.tiltakstype, antallBarn)
                    is FraværGodkjentAvNav -> FraværGodkjentAvNav.create(dato, dag.tiltakstype, antallBarn)
                    is IkkeBesvart -> IkkeBesvart.create(dato, dag.tiltakstype, antallBarn)
                    is IkkeDeltatt -> IkkeDeltatt.create(dato, dag.tiltakstype, antallBarn)
                    is IkkeRettTilTiltakspenger -> IkkeRettTilTiltakspenger(dato)
                }
            }

            if (nyeDager == eksisterendeDager) {
                return@fold acc
            }

            val nyBeregning = MeldeperiodeBeregning(
                id = BeregningId.random(),
                dager = nyeDager,
                meldekortId = beregning.meldekortId,
                kjedeId = beregning.kjedeId,
                beregningKilde = BeregningKilde.BeregningKildeBehandling(behandlingId),
            )

            acc.plus(nyBeregning)
        }
    return nyeBeregninger.toNonEmptyListOrNull()
}
