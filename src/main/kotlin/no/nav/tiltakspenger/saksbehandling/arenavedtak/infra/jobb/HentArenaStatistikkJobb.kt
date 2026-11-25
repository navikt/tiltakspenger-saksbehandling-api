package no.nav.tiltakspenger.saksbehandling.arenavedtak.infra.jobb

import arrow.core.getOrElse
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.jobber.TaskResultat
import no.nav.tiltakspenger.libs.ktor.common.oppstart.Miljøverdi
import no.nav.tiltakspenger.libs.ktor.common.oppstart.Task
import no.nav.tiltakspenger.saksbehandling.arenavedtak.ArenaStatistikkRepo
import no.nav.tiltakspenger.saksbehandling.arenavedtak.infra.TiltakspengerArenaClient
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import kotlin.time.Duration.Companion.hours

/**
 * Henter statistikken over saker og vedtak i Arena for i år og i fjor, og lagrer den i `arena_statistikk_per_maned`.
 * Henter på nytt når siste henting er mer enn et døgn gammel, fordi vedtak kan registreres i Arena med virkning bakover i tid.
 * Kan fjernes når de siste sakene i Arena er avsluttet og tallene har sluttet å endre seg.
 */
class HentArenaStatistikkJobb(
    private val tiltakspengerArenaClient: TiltakspengerArenaClient,
    private val arenaStatistikkRepo: ArenaStatistikkRepo,
    private val clock: Clock,
) {
    private val logger = KotlinLogging.logger { }

    suspend fun hentOgLagre(): TaskResultat {
        val nå = Instant.now(clock)
        val sistHentet = arenaStatistikkRepo.sistHentet()
        if (sistHentet != null && sistHentet.plusSeconds(24 * 60 * 60) > nå) return TaskResultat.IngenArbeid

        val iÅr = LocalDate.now(clock).year
        val statistikk = listOf(iÅr - 1, iÅr).map { år ->
            tiltakspengerArenaClient.hentStatistikkPerMåned(år).getOrElse {
                logger.error { "Kunne ikke hente statistikk for $år fra tiltakspenger-arena: $it" }
                return TaskResultat.Feilet
            }
        }.flatten()
        arenaStatistikkRepo.lagre(statistikk, nå)
        logger.info { "Lagret statistikk fra Arena for ${statistikk.size} måneder" }
        return TaskResultat.Ferdig
    }
}

fun (() -> HentArenaStatistikkJobb).tasks(): List<Task> {
    return listOf(
        Task(
            navn = "saksbehandling-jobb-hent-arena-statistikk",
            intervall = Miljøverdi.lik(1.hours),
            utfør = { _ -> this().hentOgLagre() },
        ),
    )
}
