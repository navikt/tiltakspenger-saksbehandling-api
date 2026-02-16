package no.nav.tiltakspenger.saksbehandling.statistikk.behandling

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.saksbehandling.behandling.ports.StatistikkSakRepo
import java.time.LocalDateTime

/**
 * DVH/Team Sak fanger kun opp nye rader i tabellen, ikke oppdateringer, så endringer som man ønsker at DVH skal få med seg må
 * komme som nye rader. DVH bruker kombinasjonen behandlingid + endrettidspunkt for å identifisere en hendelse, så
 * ved f.eks. teknisk patching av data må man inserte en ny rad med samme behandlingid + endrettidspunkt og endringene
 * man ønsker å gjøre pr rad som skal patches.
 *
 * Dokumentasjon: https://confluence.adeo.no/spaces/DVH/pages/459904637/Funksjonell+tid+teknisk+tid+og+lastet+tids+rolle+i+modell
 */
class ResendStatistikkJobb(
    private val statistikkSakRepo: StatistikkSakRepo,
) {
    private val log = KotlinLogging.logger {}

    fun resend() {
        val behandlingId = "beh_01KBZ0N5YVWZS093H24BC09N59"
        val raderSomSkalPatchesOgResendes = statistikkSakRepo.hentRaderSomSkalPatchesOgResendes(behandlingId)
        log.info { "Fant ${raderSomSkalPatchesOgResendes.size} rader som skal patches/resendes" }
        raderSomSkalPatchesOgResendes.forEach {
            val oppdatertStatistikkDTO = it.copy(
                tekniskTidspunkt = LocalDateTime.now(),
                behandlingType = StatistikkBehandlingType.SØKNADSBEHANDLING,
                søknadsformat = StatistikkFormat.PAPIR_SKJEMA.name,
            )
            statistikkSakRepo.lagre(oppdatertStatistikkDTO)
            log.info { "Resendte rad med sakId ${oppdatertStatistikkDTO.sakId}, behandlingId ${oppdatertStatistikkDTO.behandlingId}, funksjonell tid ${oppdatertStatistikkDTO.endretTidspunkt}" }
        }
    }
}
