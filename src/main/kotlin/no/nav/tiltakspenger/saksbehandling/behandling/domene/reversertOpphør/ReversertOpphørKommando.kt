package no.nav.tiltakspenger.saksbehandling.behandling.domene.reversertOpphør

import arrow.core.toNonEmptyListOrNull
import no.nav.tiltakspenger.libs.periodisering.IkkeTomPeriodisering
import no.nav.tiltakspenger.libs.periodisering.PeriodeMedVerdi
import no.nav.tiltakspenger.libs.periodisering.Periodisering
import no.nav.tiltakspenger.libs.periodisering.tilIkkeTomPeriodisering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.InnvilgelsesperiodeKommando
import no.nav.tiltakspenger.saksbehandling.behandling.domene.InnvilgelsesperiodeVerdi
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Innvilgelsesperioder
import no.nav.tiltakspenger.saksbehandling.behandling.domene.InnvilgetTiltaksdeltakelse
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.harReversertOpphør
import no.nav.tiltakspenger.saksbehandling.behandling.domene.oppdater.KanIkkeReversereOpphør
import no.nav.tiltakspenger.saksbehandling.behandling.domene.oppdater.OppdaterBehandlingKommando
import no.nav.tiltakspenger.saksbehandling.behandling.domene.oppdater.OppdaterOmgjøringKommando
import no.nav.tiltakspenger.saksbehandling.behandling.domene.tilInnvilgelsesperiodeVerdi

/**
 * Validerer periodene som er markert som reversert opphør.
 * De kan bare innvilges ved omgjøring, og hver tiltaksdeltakelse i perioden må ligge innenfor [perioderForReversertOpphør] for samme tiltaksdeltakelse.
 * De andre periodene valideres mot tiltaksdeltakelsene, se [no.nav.tiltakspenger.saksbehandling.behandling.domene.ugyldigePerioder].
 *
 * @return null dersom ingen perioder er markert som reversert opphør, eller alle kan reverseres.
 */
fun OppdaterBehandlingKommando.Innvilgelse.validerReversertOpphør(
    perioderForReversertOpphør: Periodisering<OpprinneligInnvilgelse>,
): KanIkkeReversereOpphør? {
    if (!innvilgelsesperioder.harReversertOpphør) {
        return null
    }

    if (this !is OppdaterOmgjøringKommando.OmgjøringInnvilgelse) {
        return KanIkkeReversereOpphør.KunVedOmgjøring
    }

    return innvilgelsesperioder.perioderMedVerdi
        .filter { it.verdi.erReversertOpphør }
        .flatMap { (kommando, periode) ->
            kommando.internDeltakelseIder
                .filterNot { internDeltakelseId ->
                    val perioderForDeltakelsen = perioderForReversertOpphør
                        .filter { opprinnelig, _ -> opprinnelig.innvilgerTiltaksdeltakelse(internDeltakelseId) }
                        .perioder
                    periode.trekkFra(perioderForDeltakelsen).isEmpty()
                }
                .map { KanIkkeReversereOpphør.PeriodeSomIkkeKanReverseres(periode, it) }
        }
        .toNonEmptyListOrNull()
        ?.let { KanIkkeReversereOpphør.PeriodeneKanIkkeReverseres(it) }
}

/**
 * Konverterer innvilgelsesperioder der noen kan være markert som reversert opphør.
 * Periodene for gyldige tiltaksdeltakelser får tiltaksdeltakelsen fra saksopplysningene til [behandling].
 * Periodene som er reversert opphør får tiltaksdeltakelsene fra den opprinnelige innvilgelsen, og deles opp der den opprinnelige innvilgelsen skifter vedtak.
 * Periodene må være validert med [validerReversertOpphør] først.
 */
fun IkkeTomPeriodisering<InnvilgelsesperiodeKommando>.tilInnvilgelsesperioderMedReversertOpphør(
    behandling: Rammebehandling,
    perioderForReversertOpphør: Periodisering<OpprinneligInnvilgelse>,
): Innvilgelsesperioder {
    return Innvilgelsesperioder(
        perioderMedVerdi.toList().flatMap { (kommando, periode) ->
            if (!kommando.erReversertOpphør) {
                return@flatMap listOf(PeriodeMedVerdi(kommando.tilInnvilgelsesperiodeVerdi(behandling), periode))
            }

            val opprinneligeInnvilgelser = perioderForReversertOpphør.krymp(periode).perioderMedVerdi

            require(
                opprinneligeInnvilgelser.all { opprinnelig -> kommando.internDeltakelseIder.all { opprinnelig.verdi.innvilgerTiltaksdeltakelse(it) } } &&
                    periode.trekkFra(opprinneligeInnvilgelser.map { it.periode }).isEmpty(),
            ) {
                "Perioden $periode for tiltaksdeltakelsene ${kommando.internDeltakelseIder} kan ikke innvilges som reversert opphør for ${behandling.id}"
            }

            opprinneligeInnvilgelser.map { (opprinnelig, delperiode) ->
                PeriodeMedVerdi(
                    InnvilgelsesperiodeVerdi(
                        tiltaksdeltakelser = kommando.tiltaksdeltakelser.map {
                            InnvilgetTiltaksdeltakelse(
                                tiltaksdeltakelse = opprinnelig.hentTiltaksdeltakelse(it.internDeltakelseId)!!,
                                antallDagerPerMeldeperiode = it.antallDagerPerMeldeperiode,
                            )
                        },
                        reversertOpphør = ReversertOpphør(opprinneligVedtakId = opprinnelig.vedtakId),
                    ),
                    delperiode,
                )
            }
        }.tilIkkeTomPeriodisering(),
    )
}
