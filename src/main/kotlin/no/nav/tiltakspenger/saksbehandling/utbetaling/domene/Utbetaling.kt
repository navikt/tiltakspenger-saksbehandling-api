package no.nav.tiltakspenger.saksbehandling.utbetaling.domene

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Ulid
import no.nav.tiltakspenger.libs.common.UlidBase
import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.saksbehandling.beregning.BeregningKilde
import no.nav.tiltakspenger.saksbehandling.beregning.UtbetalingBeregning
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.Navkontor
import no.nav.tiltakspenger.saksbehandling.sak.Saksnummer
import ulid.ULID
import java.time.LocalDateTime

data class UtbetalingId private constructor(
    private val ulid: UlidBase,
) : Ulid by ulid {
    companion object {
        private const val PREFIX = "utbetaling"

        fun random() = UtbetalingId(ulid = UlidBase("${PREFIX}_${ULID.randomULID()}"))

        fun fromString(stringValue: String): UtbetalingId {
            require(stringValue.startsWith(PREFIX)) {
                "Prefix må starte med $PREFIX. Dette er nok ikke en UtbetalingId ($stringValue)"
            }
            return UtbetalingId(ulid = UlidBase(stringValue))
        }
    }
}

/**
 * @property forrigeUtbetalingVedtakId er null for første utbetaling i en sak.
 */
data class Utbetaling(
    val id: UtbetalingId,
    val vedtakId: VedtakId,
    val sakId: SakId,
    val saksnummer: Saksnummer,
    val fnr: Fnr,
    val brukerNavkontor: Navkontor,
    val opprettet: LocalDateTime,
    val saksbehandler: String,
    val beslutter: String,
    val beregning: UtbetalingBeregning,
    val forrigeUtbetalingVedtakId: VedtakId?,
    val sendtTilUtbetaling: LocalDateTime?,
    val status: Utbetalingsstatus?,
) {
    val periode: Periode = beregning.periode

    val beregningKilde: BeregningKilde = beregning.beregningKilde

    val ordinærBeløp: Int = beregning.ordinærBeløp
    val barnetilleggBeløp: Int = beregning.barnetilleggBeløp
    val totalBeløp: Int = beregning.totalBeløp
}
