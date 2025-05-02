package no.nav.tiltakspenger.saksbehandling.meldekort.service

import arrow.core.Either
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.saksbehandling.behandling.ports.SakRepo
import no.nav.tiltakspenger.saksbehandling.felles.sikkerlogg
import no.nav.tiltakspenger.saksbehandling.meldekort.ports.MeldekortApiHttpClientGateway
import no.nav.tiltakspenger.saksbehandling.meldekort.ports.MeldeperiodeRepo
import java.time.Clock

/**
 * Sender meldeperioder som er klare for utfylling til meldekort-api, som serverer videre til bruker
 */
class SendTilMeldekortApiService(
    private val meldeperiodeRepo: MeldeperiodeRepo,
    private val sakRepo: SakRepo,
    private val meldekortApiHttpClient: MeldekortApiHttpClientGateway,
    private val clock: Clock,
) {
    private val logger = KotlinLogging.logger { }

    suspend fun send() {
        sendMeldeperioder()
    }

    private suspend fun sendMeldeperioder() {
        Either.catch {
            val usendteMeldeperioder = meldeperiodeRepo.hentUsendteTilBruker()

            logger.debug { "Fant ${usendteMeldeperioder.count()} meldekort for sending til meldekort-api" }

            usendteMeldeperioder.forEach { meldeperiode ->
                meldekortApiHttpClient.sendMeldeperiode(meldeperiode).onRight {
                    logger.info { "Sendte meldekort til meldekort-api med id ${meldeperiode.id}" }
                    meldeperiodeRepo.markerSomSendtTilBruker(meldeperiode.id, nå(clock))
                }.onLeft {
                    logger.error { "Kunne ikke sende meldekort til meldekort-api med id ${meldeperiode.id}" }
                }
            }
        }.onLeft {
            with("Uventet feil ved sending av meldekort til meldekort-api!") {
                logger.error(RuntimeException("Uventet feil!")) { this }
                sikkerlogg.error(it) { this }
            }
        }
    }

    private suspend fun sendSaker() {
        TODO()
    }
}
