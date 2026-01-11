package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.jobb

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.saksbehandling.behandling.ports.SøknadRepo
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.repo.TiltaksdeltakerRepo

class TiltaksdeltakerIdJobb(
    private val søknadRepo: SøknadRepo,
    private val tiltaksdeltakerRepo: TiltaksdeltakerRepo,
) {
    private val log = KotlinLogging.logger {}

    fun opprettInternTiltaksdeltakerIdForSoknadstiltak() {
        val soknadstiltak = søknadRepo.hentSoknadstiltakUtenInternId()
        log.debug { "Fant ${soknadstiltak.size} søknadstiltak som mangler intern deltakerid" }
        soknadstiltak.forEach {
            val correlationId = CorrelationId.generate()
            try {
                val internId = tiltaksdeltakerRepo.hentEllerLagre(it.id)
                søknadRepo.oppdaterInternId(it.id, internId)
                log.info { "Lagret $internId for søknadstiltak med eksternid ${it.id}, correlationId $correlationId" }
            } catch (e: Exception) {
                log.error(e) { "Noe gikk galt ved oppretting av internId for søknadstiltak med eksternId ${it.id}, correlationId $correlationId" }
            }
        }
    }
}
