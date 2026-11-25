package no.nav.tiltakspenger.saksbehandling.arenavedtak.infra.jobb

import arrow.core.getOrElse
import arrow.core.nonEmptyListOf
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.jobber.Kjøremodus
import no.nav.tiltakspenger.libs.jobber.TaskGruppe
import no.nav.tiltakspenger.libs.jobber.TaskResultat
import no.nav.tiltakspenger.saksbehandling.arenavedtak.ArenaStatistikkRepo
import no.nav.tiltakspenger.saksbehandling.arenavedtak.domene.ArenaSisteDagMedRettEtter
import no.nav.tiltakspenger.saksbehandling.arenavedtak.infra.TiltakspengerArenaClient
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import kotlin.time.Duration.Companion.hours

/**
 * Henter statistikken over saker og vedtak i Arena for i år og i fjor, og lagrer den i `arena_statistikk_per_maned`.
 * Henter også saker og personer i Arena fordelt på siste dag med rett etter [ArenaSisteDagMedRettEtter.startdato], og lagrer dem i `arena_siste_dag_med_rett`.
 * Henter også utbetalingene i Arena for i år og i fjor, og lagrer dem i `arena_utbetalinger_per_maned`.
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

    suspend fun hentOgLagreSisteDagMedRett(): TaskResultat {
        val nå = Instant.now(clock)
        val sistHentet = arenaStatistikkRepo.sisteDagMedRettSistHentet()
        if (sistHentet != null && sistHentet.plusSeconds(24 * 60 * 60) > nå) return TaskResultat.IngenArbeid

        val etter = ArenaSisteDagMedRettEtter.startdato
        val fordeling = tiltakspengerArenaClient.hentSisteDagMedRettEtter(etter).getOrElse {
            logger.error { "Kunne ikke hente siste dag med rett etter $etter fra tiltakspenger-arena: $it" }
            return TaskResultat.Feilet
        }
        arenaStatistikkRepo.lagreSisteDagMedRett(fordeling, nå)
        logger.info { "Lagret siste dag med rett fra Arena for ${fordeling.perSisteDagMedRett.size} datoer og ${fordeling.perSisteDagMedRett.sumOf { it.saker }} saker" }
        return TaskResultat.Ferdig
    }

    suspend fun hentOgLagreUtbetalinger(): TaskResultat {
        val nå = Instant.now(clock)
        val sistHentet = arenaStatistikkRepo.utbetalingerSistHentet()
        if (sistHentet != null && sistHentet.plusSeconds(24 * 60 * 60) > nå) return TaskResultat.IngenArbeid

        val iÅr = LocalDate.now(clock).year
        val statistikk = listOf(iÅr - 1, iÅr).map { år ->
            tiltakspengerArenaClient.hentUtbetalingerPerMåned(år).getOrElse {
                logger.error { "Kunne ikke hente utbetalinger for $år fra tiltakspenger-arena: $it" }
                return TaskResultat.Feilet
            }
        }.flatten()
        arenaStatistikkRepo.lagreUtbetalinger(statistikk, nå)
        logger.info { "Lagret utbetalinger fra Arena for ${statistikk.size} måneder" }
        return TaskResultat.Ferdig
    }
}

/**
 * Egen parallell gruppe, fordi hvert statistikkall kan vente i opptil 180 sekunder og ellers ville holdt igjen de serielle jobbene.
 * De tre taskene ligger i samme gruppe og kjører etter hverandre, så de ikke belaster Arena samtidig.
 */
fun (() -> HentArenaStatistikkJobb).taskGruppe(): TaskGruppe {
    return TaskGruppe(
        navn = "saksbehandling-jobb-hent-arena-statistikk",
        intervall = 1.hours,
        tasks = nonEmptyListOf(
            { _ -> this().hentOgLagre() },
            { _ -> this().hentOgLagreSisteDagMedRett() },
            { _ -> this().hentOgLagreUtbetalinger() },
        ),
        kjøremodus = Kjøremodus.PARALLELT,
    )
}
