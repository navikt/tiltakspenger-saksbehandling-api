package no.nav.tiltakspenger.saksbehandling.søknad.service

import arrow.core.Either
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.httpklient.loggFeil
import no.nav.tiltakspenger.saksbehandling.behandling.domene.RammevedtakRepo
import no.nav.tiltakspenger.saksbehandling.søknad.domene.KunneIkkeSendeSøknadsutfall
import no.nav.tiltakspenger.saksbehandling.søknad.domene.SøknadApiKlient
import no.nav.tiltakspenger.saksbehandling.søknad.domene.Søknadsutfall
import java.time.Clock

/**
 * Sender utfallet av iverksatte søknadsbehandlinger til søknad-api, slik at bruker kan se at søknaden er ferdig behandlet.
 * Søknad-api lar også bruker sende en lik søknad på nytt når den forrige er ferdig behandlet.
 *
 * Køen er rammevedtak der utfallet ikke er sendt ennå, se [RammevedtakRepo.hentSøknadsutfallTilSøknadApi].
 */
class SendSøknadsutfallTilSøknadApiJobb(
    private val rammevedtakRepo: RammevedtakRepo,
    private val søknadApiKlient: SøknadApiKlient,
    private val clock: Clock,
) {
    private val logger = KotlinLogging.logger { }

    suspend fun send() {
        Either.catch {
            rammevedtakRepo.hentSøknadsutfallTilSøknadApi().forEach { send(it) }
        }.onLeft {
            logger.error(it) { "Uventet feil ved sending av søknadsutfall til søknad-api" }
        }
    }

    private suspend fun send(søknadsutfall: Søknadsutfall) {
        val kontekst = "Søknad ${søknadsutfall.søknadId}, vedtak ${søknadsutfall.vedtakId}"
        søknadApiKlient.sendSøknadsutfall(søknadsutfall).onRight {
            rammevedtakRepo.markerSøknadsutfallSendtTilSøknadApi(søknadsutfall.vedtakId, nå(clock))
            logger.info { "Sendte søknadsutfall ${søknadsutfall.resultat} til søknad-api. $kontekst" }
        }.onLeft {
            when (it) {
                is KunneIkkeSendeSøknadsutfall.SøknadFinnesIkke -> {
                    // Et nytt forsøk vil gi samme svar, så vi markerer utfallet som sendt for å ikke blokkere køen.
                    rammevedtakRepo.markerSøknadsutfallSendtTilSøknadApi(søknadsutfall.vedtakId, nå(clock))
                    logger.warn { "Søknad-api kjenner ikke søknaden, så søknadsutfallet sendes ikke. $kontekst" }
                }

                is KunneIkkeSendeSøknadsutfall.Teknisk -> it.httpKlientError.loggFeil(
                    logger,
                    "sending av søknadsutfall til søknad-api",
                    kontekst,
                )
            }
        }
    }
}
