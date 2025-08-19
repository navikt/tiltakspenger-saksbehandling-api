package no.nav.tiltakspenger.saksbehandling.utbetaling.domene

import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.saksbehandling.beregning.BeregningKilde
import no.nav.tiltakspenger.saksbehandling.beregning.UtbetalingBeregning
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.Navkontor
import java.time.LocalDateTime

/**
 * @property forrigeUtbetalingVedtakId er null for første utbetaling i en sak.
 */
data class Utbetaling(
    val beregning: UtbetalingBeregning,
    val brukerNavkontor: Navkontor,
    val vedtakId: VedtakId,
    val forrigeUtbetalingVedtakId: VedtakId?,
    val sendtTilUtbetaling: LocalDateTime?,
    val status: Utbetalingsstatus?,
) {
    val periode: Periode = beregning.periode

    val beregningKilde: BeregningKilde = beregning.beregningKilde

    val ordinærBeløp: Int = beregning.ordinærBeløp
    val barnetilleggBeløp: Int = beregning.barnetilleggBeløp
    val totalBeløp: Int = beregning.totalBeløp

    fun oppdaterStatus(status: Utbetalingsstatus?): Utbetaling {
        return this.copy(status = status)
    }
}
