package no.nav.tiltakspenger.saksbehandling.statistikk.vedtak

import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.VedtattUtbetaling
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
)

fun VedtattUtbetaling.tilStatistikk(): StatistikkUtbetalingDTO =
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
        opprettet = LocalDateTime.now(),
        sistEndret = LocalDateTime.now(),
        brukerId = this.fnr.verdi,
    )
