package no.nav.tiltakspenger.saksbehandling.behandling.service.statistikk.stønad

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
    // vedtaket som er bakgrunnen for utbetalingen
    val vedtakId: String?,
    val opprettet: LocalDateTime?,
    val sistEndret: LocalDateTime?,
)
