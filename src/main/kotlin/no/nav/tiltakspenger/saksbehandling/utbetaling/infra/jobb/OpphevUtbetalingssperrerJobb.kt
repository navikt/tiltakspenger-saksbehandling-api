package no.nav.tiltakspenger.saksbehandling.utbetaling.infra.jobb

import arrow.core.Either
import arrow.core.getOrElse
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.jobber.TaskResultat
import no.nav.tiltakspenger.libs.jobber.tilSamletResultat
import no.nav.tiltakspenger.libs.ktor.common.oppstart.Task
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.KanIkkeOppheveUtbetalingssperre
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.UtbetalingRepo
import no.nav.tiltakspenger.saksbehandling.utbetaling.service.MigrerTilOppdragsversjon3Service

/** Prøver å oppheve utbetalingssperren på alle saker Hel Ved har migrert til oppdragsversjon 3. */
class OpphevUtbetalingssperrerJobb(
    private val utbetalingRepo: UtbetalingRepo,
    private val migrerTilOppdragsversjon3Service: MigrerTilOppdragsversjon3Service,
) {
    private val logger = KotlinLogging.logger { }

    /** Svarer [TaskResultat.Feilet] når minst én sak feilet. */
    suspend fun opphevUtbetalingssperrer(): TaskResultat {
        return Either.catch {
            val saker = utbetalingRepo.hentSakerMigrertHosHelVed()
            if (saker.isEmpty()) TaskResultat.IngenArbeid else saker.map { opphevUtbetalingssperre(it) }.tilSamletResultat()
        }.getOrElse {
            logger.error(it) { "Ukjent feil ved oppheving av utbetalingssperrer" }
            TaskResultat.Feilet
        }
    }

    /** En sperre som blir stående fordi simuleringen viser endring, er ikke en feil; en simulering som feiler, er det. */
    suspend fun opphevUtbetalingssperre(sakId: SakId): TaskResultat {
        return Either.catch { migrerTilOppdragsversjon3Service.opphevUtbetalingssperre(sakId) }.fold(
            ifLeft = {
                logger.error(it) { "Ukjent feil ved oppheving av utbetalingssperren. sakId: $sakId" }
                TaskResultat.Feilet
            },
            ifRight = { utfall ->
                if (utfall.leftOrNull() is KanIkkeOppheveUtbetalingssperre.SimuleringFeilet) TaskResultat.Feilet else TaskResultat.Ferdig
            },
        )
    }
}

fun (() -> OpphevUtbetalingssperrerJobb).tasks(): List<Task> {
    return listOf(
        Task(
            navn = "saksbehandling-jobb-opphev-utbetalingssperrer",
            utfør = { _ ->
                this().opphevUtbetalingssperrer()
            },
        ),
    )
}
