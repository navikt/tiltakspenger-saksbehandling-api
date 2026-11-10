package no.nav.tiltakspenger.saksbehandling.infra.metrikker

import io.github.oshai.kotlinlogging.KLogger
import no.nav.tiltakspenger.saksbehandling.vedtak.Rammevedtak

/**
 * Varsler når et rammevedtak med reversert opphør er iverksatt.
 *
 * Reversert opphør innvilger perioder på nytt uten gyldig tiltaksdeltakelse i saksopplysningene.
 * Det er lov, men skal skje sjelden, så hvert tilfelle gir en warning-alarm.
 * Se [no.nav.tiltakspenger.saksbehandling.behandling.domene.reversertOpphør.ReversertOpphør].
 *
 * @param kontekst Identifiserende felter for vedtaket, brukt i loggmeldingen.
 */
fun varsleHvisReversertOpphørErIverksatt(
    log: KLogger,
    rammevedtak: Rammevedtak,
    kontekst: () -> String,
) {
    if (rammevedtak.innvilgelsesperioder?.harReversertOpphør != true) {
        return
    }
    MetricRegister.REVERSERT_OPPHØR_IVERKSATT.inc()
    log.warn { "Reversert opphør er iverksatt. Perioder er innvilget på nytt uten gyldig tiltaksdeltakelse i saksopplysningene. ${kontekst()}" }
}
