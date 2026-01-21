package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.jobb

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.repo.TiltaksdeltakerRepo

class TiltaksdeltakerJobb(
    private val tiltaksdeltakerRepo: TiltaksdeltakerRepo,
) {
    private val log = KotlinLogging.logger {}

    fun lagreTiltakstypeForTiltaksdeltaker() {
        val idOgTiltakstyper = tiltaksdeltakerRepo.hentIdUtenTiltakstypeOgTiltakstypen()
        log.debug { "Fant ${idOgTiltakstyper.size} tiltaksdeltakelser som mangler tiltakstype" }
        idOgTiltakstyper.forEach {
            try {
                tiltaksdeltakerRepo.lagreTiltakstype(it)
                log.info { "Lagret tiltakstype for tiltak med id ${it.id}" }
            } catch (e: Exception) {
                log.error(e) { "Noe gikk galt ved lgring av tiltakstype for tiltak med id ${it.id}" }
            }
        }
    }
}
