package no.nav.tiltakspenger.saksbehandling.statistikk.stønadsstatistikk

import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.saksbehandling.beregning.MeldeperiodeBeregning
import no.nav.tiltakspenger.saksbehandling.statistikk.GenererUtbetalingsstatistikk
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.VedtattUtbetaling
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Én rad i `statistikk_utbetaling` per utbetaling, lest av Team Spenn (DVH) som beskrevet på [StatistikkStønadDTO].
 */
data class StatistikkUtbetalingDTO(
    // id for utbetalingsvedtaket
    val id: String,
    val sakId: String,
    val saksnummer: String,
    val ordinærBeløp: Int,
    val barnetilleggBeløp: Int,
    val totalBeløp: Int,
    val posteringDato: LocalDate,
    val gyldigFraDatoPostering: LocalDate,
    val gyldigTilDatoPostering: LocalDate,
    // UUID-delen av [id], som vi sender til helved som behandlingId og som havner i OS/UR i feltet «henvisning».
    val utbetalingId: String,
    // Rammevedtaket/vedtakene som gjelder i perioden for utbetalingen
    val vedtakId: List<String>?,
    val opprettet: LocalDateTime?,
    val sistEndret: LocalDateTime?,
    val brukerId: String,
    val meldeperioder: List<StatistikkMeldeperiode>,
) {
    data class StatistikkMeldeperiode(
        val meldeperiodeKjedeId: String,
        val meldekortbehandlingId: String,
        val ordinarBelop: Int,
        val barnetilleggBelop: Int,
        val totalBelop: Int,
    )
}

/**
 * @param rammevedtakIder Rammevedtakene som er bakgrunnen for utbetalingen.
 * For et rammevedtak er det vedtaket selv, og for et meldekortvedtak er det rammevedtakene meldekortbehandlingen bygger på.
 */
fun VedtattUtbetaling.tilStatistikk(
    rammevedtakIder: List<VedtakId>,
    clock: Clock,
): GenererUtbetalingsstatistikk {
    return GenererUtbetalingsstatistikk {
        StatistikkUtbetalingDTO(
            id = this.id.toString(),
            sakId = this.sakId.toString(),
            saksnummer = this.saksnummer.toString(),
            ordinærBeløp = this.ordinærBeløp,
            barnetilleggBeløp = this.barnetilleggBeløp,
            totalBeløp = this.totalBeløp,
            posteringDato = this.opprettet.toLocalDate(),
            gyldigFraDatoPostering = this.periode.fraOgMed,
            gyldigTilDatoPostering = this.periode.tilOgMed,
            utbetalingId = this.id.uuidPart(),
            vedtakId = rammevedtakIder.map { it.toString() },
            opprettet = nå(clock),
            sistEndret = nå(clock),
            brukerId = this.fnr.verdi,
            meldeperioder = this.beregning.beregninger.toList().map {
                it.toStatistikkMeldeperiode()
            },
        )
    }
}

private fun MeldeperiodeBeregning.toStatistikkMeldeperiode() =
    StatistikkUtbetalingDTO.StatistikkMeldeperiode(
        meldeperiodeKjedeId = this.kjedeId.toString(),
        meldekortbehandlingId = this.meldekortId.toString(),
        ordinarBelop = this.ordinærBeløp,
        barnetilleggBelop = this.barnetilleggBeløp,
        totalBelop = this.totalBeløp,
    )
