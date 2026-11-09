package no.nav.tiltakspenger.saksbehandling.behandling.domene.reversertOpphør

import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.libs.periodisering.PeriodeMedVerdi
import no.nav.tiltakspenger.libs.periodisering.Periodisering
import no.nav.tiltakspenger.libs.periodisering.tilPeriodisering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.Rammebehandlingsresultat
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakelseIntern
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakerId
import no.nav.tiltakspenger.saksbehandling.vedtak.Rammevedtaksliste

/**
 * Den opprinnelige innvilgelsen av en periode som kan innvilges som [ReversertOpphør].
 *
 * @param vedtakId Det siste vedtaket som innvilget perioden med gyldig tiltaksdeltakelse.
 * @param tiltaksdeltakelse Tiltaksdeltakelsen slik den var registrert i [vedtakId].
 */
data class OpprinneligInnvilgelse(
    val vedtakId: VedtakId,
    val tiltaksdeltakelse: TiltaksdeltakelseIntern,
) {
    val internDeltakelseId: TiltaksdeltakerId get() = tiltaksdeltakelse.internDeltakelseId
}

/**
 * Periodene som kan innvilges som [ReversertOpphør], med den opprinnelige innvilgelsen for hver dag.
 *
 * Det er dagene som er innvilget med gyldig tiltaksdeltakelse i et tidligere vedtak, men som ikke lenger er det.
 * Dagene er altså opphørt, stanset, eller innvilget som reversert opphør i et senere vedtak.
 * Er en dag innvilget i flere vedtak, er det det sist opprettede vedtaket som er den opprinnelige innvilgelsen.
 * Perioder som er innvilget som reversert opphør regnes aldri som en opprinnelig innvilgelse.
 *
 * Periodene kan overlappe med periodene som kan innvilges for gyldige tiltaksdeltakelser, se [no.nav.tiltakspenger.saksbehandling.behandling.domene.innvilgbarePerioder].
 */
fun Rammevedtaksliste.perioderForReversertOpphør(): Periodisering<OpprinneligInnvilgelse> {
    val sistInnvilgetMedGyldigTiltaksdeltakelse: Periodisering<OpprinneligInnvilgelse> = this
        .filter { it.rammebehandlingsresultat is Rammebehandlingsresultat.Innvilgelse }
        .flatMap { vedtak ->
            vedtak.innvilgelsesperioder!!.periodisering.perioderMedVerdi
                .filterNot { it.verdi.erReversertOpphør }
                .map { PeriodeMedVerdi(OpprinneligInnvilgelse(vedtak.id, it.verdi.valgtTiltaksdeltakelse), it.periode) }
        }
        // Vedtakslisten er sortert på opprettet, så det sist opprettede vedtaket vinner der periodene overlapper.
        .fold(Periodisering.empty<OpprinneligInnvilgelse>() as Periodisering<OpprinneligInnvilgelse>) { tidslinje, (opprinnelig, periode) ->
            tidslinje.setVerdiForDelperiode(opprinnelig, periode)
        }

    val gjeldendeInnvilgetMedGyldigTiltaksdeltakelse = innvilgelsesperioder
        .filter { verdi, _ -> !verdi.erReversertOpphør }
        .perioder

    return sistInnvilgetMedGyldigTiltaksdeltakelse.perioderMedVerdi.flatMap { (opprinnelig, periode) ->
        periode.trekkFra(gjeldendeInnvilgetMedGyldigTiltaksdeltakelse).map { PeriodeMedVerdi(opprinnelig, it) }
    }.tilPeriodisering()
}
