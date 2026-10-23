package no.nav.tiltakspenger.saksbehandling.søknad.service

import arrow.core.Either
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.httpklient.loggFeil
import no.nav.tiltakspenger.saksbehandling.behandling.domene.RammevedtakRepo
import no.nav.tiltakspenger.saksbehandling.behandling.domene.SøknadRepo
import no.nav.tiltakspenger.saksbehandling.søknad.domene.KunneIkkeSendeSøknadsutfall
import no.nav.tiltakspenger.saksbehandling.søknad.domene.SøknadApiKlient
import no.nav.tiltakspenger.saksbehandling.søknad.domene.Søknadsutfall
import java.time.Clock

/**
 * Sender utfallet av behandlingen av digitale søknader til søknad-api, slik at bruker kan se om søknaden er ferdig behandlet.
 * Søknad-api lar også bruker sende en lik søknad på nytt når den forrige er ferdig behandlet.
 *
 * Jobben har to køer.
 * Vedtak i søknadsbehandlinger ligger i [RammevedtakRepo.hentSøknadsutfallTilSøknadApi].
 * Avbrudd og gjenåpninger av søknader, som ikke har vedtak, ligger i [SøknadRepo.hentSøknadshendelserTilSøknadApi].
 * Søknad-api beholder utfallet med nyest tidspunkt, så rekkefølgen mellom køene spiller ingen rolle.
 */
class SendSøknadsutfallTilSøknadApiJobb(
    private val rammevedtakRepo: RammevedtakRepo,
    private val søknadRepo: SøknadRepo,
    private val søknadApiKlient: SøknadApiKlient,
    private val clock: Clock,
) {
    private val logger = KotlinLogging.logger { }

    suspend fun send() {
        Either.catch {
            rammevedtakRepo.hentSøknadsutfallTilSøknadApi().forEach { søknadsutfall ->
                val vedtakId = requireNotNull(søknadsutfall.vedtakId) { "Søknadsutfall fra et vedtak mangler vedtakId" }
                send(søknadsutfall) { rammevedtakRepo.markerSøknadsutfallSendtTilSøknadApi(vedtakId, nå(clock)) }
            }
            søknadRepo.hentSøknadshendelserTilSøknadApi().forEach { (søknadsutfall, antallHendelser) ->
                send(søknadsutfall) {
                    søknadRepo.markerSøknadshendelserSendtTilSøknadApi(søknadsutfall.søknadId, antallHendelser)
                }
            }
        }.onLeft {
            logger.error(it) { "Uventet feil ved sending av søknadsutfall til søknad-api" }
        }
    }

    private suspend fun send(søknadsutfall: Søknadsutfall, markerSomSendt: () -> Unit) {
        val kontekst = "Søknad ${søknadsutfall.søknadId}, vedtak ${søknadsutfall.vedtakId}"
        søknadApiKlient.sendSøknadsutfall(søknadsutfall).onRight {
            markerSomSendt()
            logger.info { "Sendte søknadsutfall ${søknadsutfall.resultat} til søknad-api. $kontekst" }
        }.onLeft {
            when (it) {
                is KunneIkkeSendeSøknadsutfall.SøknadFinnesIkke -> {
                    // Et nytt forsøk vil gi samme svar, så vi markerer utfallet som sendt for å ikke blokkere køen.
                    markerSomSendt()
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
