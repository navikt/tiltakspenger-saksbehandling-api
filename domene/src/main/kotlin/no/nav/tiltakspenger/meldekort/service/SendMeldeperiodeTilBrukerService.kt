package no.nav.tiltakspenger.meldekort.service

import arrow.core.Either
import mu.KotlinLogging
import no.nav.tiltakspenger.felles.sikkerlogg
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.meldekort.ports.MeldekortApiHttpClientGateway
import no.nav.tiltakspenger.meldekort.ports.MeldeperiodeRepo

/**
 * Sender meldekort som er klare for utfylling til meldekort-api, som serverer videre til bruker
 */
class SendMeldeperiodeTilBrukerService(
    private val meldeperiodeRepo: MeldeperiodeRepo,
    private val meldekortApiHttpClient: MeldekortApiHttpClientGateway,
) {
    private val logger = KotlinLogging.logger { }

    suspend fun send() {
        Either.catch {
            val usendteMeldeperioder = meldeperiodeRepo.hentUsendteTilBruker()

            logger.debug("Fant ${usendteMeldeperioder.count()} meldekort for sending til meldekort-api")

            usendteMeldeperioder.forEach { meldeperiode ->
                meldekortApiHttpClient.sendMeldeperiode(meldeperiode).onRight {
                    logger.info { "Sendte meldekort til meldekort-api med id ${meldeperiode.hendelseId}" }
                    meldeperiodeRepo.markerSomSendtTilBruker(meldeperiode.hendelseId, nå())
                }.onLeft {
                    logger.error { "Kunne ikke sende meldekort til meldekort-api med id ${meldeperiode.hendelseId}" }
                }
            }
        }.onLeft {
            with("Uventet feil ved sending av meldekort til meldekort-api!") {
                logger.error(RuntimeException("Uventet feil!")) { this }
                sikkerlogg.error(it) { this }
            }
        }
    }
}