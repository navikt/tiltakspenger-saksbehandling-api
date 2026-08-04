package no.nav.tiltakspenger.saksbehandling.meldekort.service

import arrow.core.Either
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.httpklient.loggFeil
import no.nav.tiltakspenger.saksbehandling.behandling.domene.SakRepo
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortApiKlient

/**
 * Sender meldeperioder som er klare for utfylling til meldekort-api, som serverer videre til bruker
 */
class SendTilMeldekortApiService(
    private val sakRepo: SakRepo,
    private val meldekortApiHttpClient: MeldekortApiKlient,
) {
    private val logger = KotlinLogging.logger { }

    suspend fun sendSaker() {
        Either.catch {
            val sakIder = sakRepo.hentSakIderForSendingTilMeldekortApi(limit = 100)

            logger.debug { "Fant ${sakIder.count()} saker for sending til meldekort-api" }

            sakIder.forEach { sendSak(it) }
        }.onLeft {
            logger.error(it) { "Uventet feil ved sending av saker til meldekort-api!" }
        }
    }

    suspend fun sendSak(sakId: SakId) {
        val sak = sakRepo.hentForSakId(sakId) ?: return
        meldekortApiHttpClient.sendSak(sak).onRight {
            logger.info { "Sendte sak til meldekort-api med id $sakId" }
            val erMarkertSendt = sakRepo.markerErSendtTilMeldekortApi(sakId, sak.nyesteRammeEllerMeldekortvedtakOpprettet)
            if (!erMarkertSendt) {
                logger.warn { "Sak $sakId ble sendt til meldekort-api, men det er nye vedtak på saken - sendes igjen ved neste kjøring" }
            }
        }.onLeft {
            it.loggFeil(logger, "sending av sak til meldekort-api", "Sak $sakId")
        }
    }
}
