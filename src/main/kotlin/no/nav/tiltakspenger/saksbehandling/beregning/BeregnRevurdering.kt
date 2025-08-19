package no.nav.tiltakspenger.saksbehandling.beregning

import arrow.core.toNonEmptyListOrThrow
import no.nav.tiltakspenger.libs.periodisering.tilPeriodisering
import no.nav.tiltakspenger.libs.tiltak.TiltakstypeSomGirRett
import no.nav.tiltakspenger.saksbehandling.barnetillegg.AntallBarn
import no.nav.tiltakspenger.saksbehandling.behandling.domene.OppdaterRevurderingKommando
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
    kommando: OppdaterRevurderingKommando.Innvilgelse,
): BehandlingBeregning? {
    val nyeBeregninger: List<MeldeperiodeBeregning> = beregnMeldeperioderPåNytt(kommando)

    if (nyeBeregninger.isEmpty()) {
        return null
    }

    return BehandlingBeregning(
        beregninger = nyeBeregninger.toNonEmptyListOrThrow(),
    )
}

private fun Sak.beregnMeldeperioderPåNytt(
    kommando: OppdaterRevurderingKommando.Innvilgelse,
): List<MeldeperiodeBeregning> {
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

        nyBeregning
    }
}
