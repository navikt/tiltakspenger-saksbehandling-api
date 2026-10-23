package no.nav.tiltakspenger.saksbehandling.søknad.infra.http

import arrow.core.Either
import no.nav.tiltakspenger.libs.httpklient.HttpKlientError
import no.nav.tiltakspenger.libs.httpklient.infra.HttpKlient
import no.nav.tiltakspenger.libs.httpklient.infra.HttpKlientConfig
import no.nav.tiltakspenger.libs.httpklient.infra.kall.AuthTokenProvider
import no.nav.tiltakspenger.libs.httpklient.infra.kall.KlientAuth
import no.nav.tiltakspenger.libs.httpklient.infra.kall.SerialisertJson
import no.nav.tiltakspenger.libs.httpklient.infra.transport.HttpTransport
import no.nav.tiltakspenger.libs.httpklient.infra.transport.JavaHttpTransport
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.SøknadsbehandlingsresultatType
import no.nav.tiltakspenger.saksbehandling.søknad.domene.KunneIkkeSendeSøknadsutfall
import no.nav.tiltakspenger.saksbehandling.søknad.domene.SøknadApiKlient
import no.nav.tiltakspenger.saksbehandling.søknad.domene.Søknadsutfall
import java.net.URI
import java.time.Clock
import java.time.LocalDateTime
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Klient mot tiltakspenger-soknad-api, bygget på den felles [HttpKlient]-modulen i tiltakspenger-libs.
 *
 * Kildekode: https://github.com/navikt/tiltakspenger-soknad-api
 * Dokumentasjon: README-en i kildekode-repoet
 * API-spec: -
 * Slack: #tiltakspenger-værsågod (eget team)
 * Teamkatalog: https://teamkatalogen.nav.no/team/15bca3d2-2584-4167-85ba-faab1f1cfb53
 *
 * Klienten logger bevisst ikke selv.
 * Feilloggingen gjøres én gang av konsumenten ([no.nav.tiltakspenger.saksbehandling.søknad.service.SendSøknadsutfallTilSøknadApiJobb]), som i tillegg har domenekonteksten.
 *
 * @param transport Transporten som gjør nettverkskallet; tester sender inn `FakeHttpTransport`.
 */
class SøknadApiHttpClient(
    baseUrl: String,
    clock: Clock,
    authTokenProvider: AuthTokenProvider,
    connectTimeout: Duration = 1.seconds,
    timeout: Duration = 1.seconds,
    transport: HttpTransport = JavaHttpTransport(connectTimeout = connectTimeout),
) : SøknadApiKlient {
    private val httpKlient: HttpKlient = HttpKlient(
        clock = clock,
        config = HttpKlientConfig(
            timeout = timeout,
            auth = KlientAuth.System(authTokenProvider),
        ),
        transport = transport,
    )

    private val søknadsutfallUri = URI.create("$baseUrl/saksbehandling/soknad/utfall")

    override suspend fun sendSøknadsutfall(søknadsutfall: Søknadsutfall): Either<KunneIkkeSendeSøknadsutfall, Unit> {
        val payload = serialize(søknadsutfall.tilDTO())
        return httpKlient.postJsonUtenSvar(søknadsutfallUri, SerialisertJson(payload))
            .map { }
            .mapLeft { it.tilFeil() }
    }

    private fun HttpKlientError.tilFeil(): KunneIkkeSendeSøknadsutfall =
        if (this is HttpKlientError.UventetStatus && statusCode == 404) {
            KunneIkkeSendeSøknadsutfall.SøknadFinnesIkke(this)
        } else {
            KunneIkkeSendeSøknadsutfall.Teknisk(this)
        }
}

private data class SøknadsutfallDTO(
    val søknadId: String,
    val vedtakId: String,
    val utfall: UtfallDTO,
    val vedtakstidspunkt: LocalDateTime,
)

private enum class UtfallDTO {
    INNVILGELSE,
    AVSLAG,
}

private fun Søknadsutfall.tilDTO() = SøknadsutfallDTO(
    søknadId = søknadId.toString(),
    vedtakId = vedtakId.toString(),
    utfall = when (resultat) {
        SøknadsbehandlingsresultatType.INNVILGELSE -> UtfallDTO.INNVILGELSE
        SøknadsbehandlingsresultatType.AVSLAG -> UtfallDTO.AVSLAG
    },
    vedtakstidspunkt = vedtakstidspunkt,
)
