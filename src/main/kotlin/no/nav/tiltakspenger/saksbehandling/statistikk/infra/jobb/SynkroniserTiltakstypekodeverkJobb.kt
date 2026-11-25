package no.nav.tiltakspenger.saksbehandling.statistikk.infra.jobb

import no.nav.tiltakspenger.libs.jobber.TaskResultat
import no.nav.tiltakspenger.libs.ktor.common.oppstart.Task
import no.nav.tiltakspenger.saksbehandling.statistikk.StatistikkRepo
import no.nav.tiltakspenger.saksbehandling.statistikk.kodeverk.gjeldendeTiltakstypekodeverk

/**
 * Skriver `statistikk_kodeverk_tiltakstype` fra koden én gang per pod.
 * Kodeverket endres bare ved deploy, og en gammel pod som fortsatt er leder under utrullingen, skal ikke skrive tilbake den gamle mappingen.
 */
class SynkroniserTiltakstypekodeverkJobb(
    private val statistikkRepo: StatistikkRepo,
) {
    @Volatile
    private var erSynkronisert = false

    fun synkroniser(): TaskResultat {
        if (erSynkronisert) return TaskResultat.IngenArbeid
        statistikkRepo.synkroniserTiltakstypekodeverk(gjeldendeTiltakstypekodeverk())
        erSynkronisert = true
        return TaskResultat.Ferdig
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
