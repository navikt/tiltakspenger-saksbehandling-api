package no.nav.tiltakspenger.saksbehandling.statistikk.behandling.jobb

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.saksbehandling.behandling.ports.StatistikkSakRepo
import java.time.LocalDateTime

class OppdaterEndretTidspunktJobb(
    private val statistikkSakRepo: StatistikkSakRepo,
) {
    private val log = KotlinLogging.logger { }

    fun oppdaterEndrettidspunkt() {
        val raderMedFeilEndrettidspunkt = statistikkSakRepo.hentAlleMedFeilEndrettidspunkt()
        log.info { "Fant ${raderMedFeilEndrettidspunkt.size} statistikk-sak-rader med avvik i endrettidspunkt" }
        raderMedFeilEndrettidspunkt.forEach {
            statistikkSakRepo.oppdaterEndretTidspunkt(it)
            log.info { "Oppdatert endrettidspunkt for statistikk-sak-rad med id ${it.id}, sakId ${it.sakId}, behandlingId ${it.behandlingId}" }
        }
    }
}

data class MinimalStatistikkSakDTO(
    val id: Int,
    val sakId: String,
    val behandlingId: String,
    val endretTidspunkt: LocalDateTime,
    val tekniskTidspunkt: LocalDateTime,
)
