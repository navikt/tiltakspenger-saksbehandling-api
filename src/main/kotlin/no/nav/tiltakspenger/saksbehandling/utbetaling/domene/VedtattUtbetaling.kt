package no.nav.tiltakspenger.saksbehandling.utbetaling.domene

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Ulid
import no.nav.tiltakspenger.libs.common.UlidBase
import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.Periodiserbar
import no.nav.tiltakspenger.libs.periodisering.Periodisering
import no.nav.tiltakspenger.saksbehandling.beregning.Beregning
import no.nav.tiltakspenger.saksbehandling.beregning.BeregningKilde
import no.nav.tiltakspenger.saksbehandling.beregning.MeldeperiodeBeregningDag
import no.nav.tiltakspenger.saksbehandling.felles.Forsøkshistorikk
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.Navkontor
import no.nav.tiltakspenger.saksbehandling.sak.Saksnummer
import no.nav.utsjekk.kontrakter.felles.Satstype
import ulid.ULID
import java.time.LocalDate
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
 * @property forrigeUtbetalingId er null for første utbetaling i en sak.
 * @property opprettet tidspunktet der vedtaket/utbetalingen ble opprettet
 */
data class VedtattUtbetaling(
    val id: UtbetalingId,
    val vedtakId: VedtakId,
    val sakId: SakId,
    val saksnummer: Saksnummer,
    val fnr: Fnr,
    val brukerNavkontor: Navkontor,
    override val opprettet: LocalDateTime,
    val saksbehandler: String,
    val beslutter: String,
    val beregning: Beregning,
    val forrigeUtbetalingId: UtbetalingId?,
    val statusMetadata: Forsøkshistorikk,
    val sendtTilUtbetaling: SendtTilUtbetaling?,
) : Periodiserbar {
    override val periode: Periode = beregning.periode

    val beregningKilde: BeregningKilde = beregning.beregningKilde

    val ordinærBeløp: Int = beregning.ordinærBeløp
    val barnetilleggBeløp: Int = beregning.barnetilleggBeløp
    val totalBeløp: Int = beregning.totalBeløp

    val status: Utbetalingsstatus? by lazy { sendtTilUtbetaling?.status }

    fun hentBeregningsdagForDato(dato: LocalDate): MeldeperiodeBeregningDag? {
        return beregning.hentDag(dato)
    }

    data class SendtTilUtbetaling(
        val sendtTidspunkt: LocalDateTime,
        val satstype: Periodisering<Satstype>,
        val status: Utbetalingsstatus?,
    )

    init {
        if (sendtTilUtbetaling != null) {
            val satstypePeriode = sendtTilUtbetaling.satstype.totalPeriode

            require(periode.inneholderHele(satstypePeriode)) {
                "Periodisering av satstype kan ikke gå utenfor utbetalingsperioden - $satstypePeriode - $periode"
            }
        }
    }
}
