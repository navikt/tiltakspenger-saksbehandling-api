package no.nav.tiltakspenger.saksbehandling.meldekort.service

import arrow.core.Either
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.saksbehandling.behandling.ports.SakRepo
import no.nav.tiltakspenger.saksbehandling.meldekort.ports.MeldekortApiHttpClientGateway

/**
 * Sender meldeperioder som er klare for utfylling til meldekort-api, som serverer videre til bruker
 */
class SendTilMeldekortApiService(
    private val sakRepo: SakRepo,
    private val meldekortApiHttpClient: MeldekortApiHttpClientGateway,
) {
    private val logger = KotlinLogging.logger { }

    suspend fun sendSaker() {
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
            logger.error(it) { "Uventet feil ved sending av saker til meldekort-api!" }
        }
    }
}
