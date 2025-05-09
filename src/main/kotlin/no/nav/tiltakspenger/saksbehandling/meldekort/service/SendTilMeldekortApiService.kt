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
        sendSaker()
    }

    private suspend fun sendMeldeperioder() {
        Either.catch {
            val usendteMeldeperioder = meldeperiodeRepo.hentUsendteTilBruker()

            logger.debug { "Fant ${usendteMeldeperioder.count()} meldeperioder for sending til meldekort-api" }

            usendteMeldeperioder.forEach { meldeperiode ->
                meldekortApiHttpClient.sendMeldeperiode(meldeperiode).onRight {
                    logger.info { "Sendte meldeperiode til meldekort-api med id ${meldeperiode.id}" }
                    meldeperiodeRepo.markerSomSendtTilBruker(meldeperiode.id, nå(clock))
                }.onLeft {
                    logger.error { "Kunne ikke sende meldeperiode til meldekort-api med id ${meldeperiode.id}" }
                }
            }
        }.onLeft {
            with("Uventet feil ved sending av meldeperiode til meldekort-api!") {
                logger.error(RuntimeException("Uventet feil!")) { this }
                sikkerlogg.error(it) { this }
            }
        }
    }

    private suspend fun sendSaker() {
        Either.catch {
            val saker = sakRepo.hentForSendingTilMeldekortApi()

            logger.debug { "Fant ${saker.count()} saker for sending til meldekort-api" }

            saker.forEach { sak ->
                val id = sak.id
                meldekortApiHttpClient.sendSak(sak).onRight {
                    logger.info { "Sendte sak til meldekort-api med id $id" }
                    sakRepo.oppdaterSkalSendesTilMeldekortApi(id, false)
                }.onLeft {
                    logger.error { "Kunne ikke sende sak til meldekort-api med id $id" }
                }
            }
        }.onLeft {
            with("Uventet feil ved sending av saker til meldekort-api!") {
                logger.error(RuntimeException("Uventet feil!")) { this }
                sikkerlogg.error(it) { this }
            }
        }
    }
}
