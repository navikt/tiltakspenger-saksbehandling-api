package no.nav.tiltakspenger.saksbehandling.statistikk.infra.jobb

import no.nav.tiltakspenger.libs.jobber.TaskResultat
import no.nav.tiltakspenger.libs.ktor.common.oppstart.Task
import no.nav.tiltakspenger.saksbehandling.statistikk.StatistikkService

class SynkroniserTiltakstypekodeverkJobb(
    private val statistikkService: StatistikkService,
) {
    fun synkroniser(): TaskResultat {
        if (statistikkService.synkroniserTiltakstypekodeverk()) {
            return TaskResultat.Ferdig
        }
        return TaskResultat.IngenArbeid
    }
}

fun (() -> SynkroniserTiltakstypekodeverkJobb).tasks(): List<Task> {
    return listOf(
        Task(
            navn = "saksbehandling-jobb-synkroniser-statistikk-kodeverk-tiltakstype",
            utfør = { _ ->
                this().synkroniser()
            },
        ),
    )
}
