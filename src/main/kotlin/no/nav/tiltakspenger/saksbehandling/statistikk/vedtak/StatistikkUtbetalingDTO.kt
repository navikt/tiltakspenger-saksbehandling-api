package no.nav.tiltakspenger.saksbehandling.statistikk.vedtak

import no.nav.tiltakspenger.saksbehandling.beregning.MeldeperiodeBeregning
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.VedtattUtbetaling
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime

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
    // id-en som vi sender til helved
    val utbetalingId: String,
    // vedtaket/vedtakene som er bakgrunnen for utbetalingen
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

fun VedtattUtbetaling.tilStatistikk(clock: Clock): StatistikkUtbetalingDTO =
    StatistikkUtbetalingDTO(
        // TODO post-mvp jah: Vi sender uuid-delen av denne til helved som behandlingId som mappes videre til OS/UR i feltet 'henvisning'.
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
        vedtakId = listOf(this.vedtakId.toString()),
        opprettet = LocalDateTime.now(clock),
        sistEndret = LocalDateTime.now(clock),
        brukerId = this.fnr.verdi,
        meldeperioder = this.beregning.beregninger.toList().map {
            it.toStatistikkMeldeperiode()
        },
    )

private fun MeldeperiodeBeregning.toStatistikkMeldeperiode() =
    StatistikkUtbetalingDTO.StatistikkMeldeperiode(
        meldeperiodeKjedeId = this.kjedeId.toString(),
        meldekortbehandlingId = this.meldekortId.toString(),
        ordinarBelop = this.ordinærBeløp,
        barnetilleggBelop = this.barnetilleggBeløp,
        totalBelop = this.totalBeløp,
    )
