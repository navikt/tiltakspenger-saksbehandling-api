package no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.infra.http

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.left
import arrow.core.right
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.httpklient.HttpKlientError
import no.nav.tiltakspenger.libs.httpklient.infra.HttpKlient
import no.nav.tiltakspenger.libs.httpklient.infra.HttpKlientConfig
import no.nav.tiltakspenger.libs.httpklient.infra.kall.AuthTokenProvider
import no.nav.tiltakspenger.libs.httpklient.infra.kall.KlientAuth
import no.nav.tiltakspenger.libs.httpklient.infra.kall.NavHeadere
import no.nav.tiltakspenger.libs.httpklient.infra.kall.Statusregel
import no.nav.tiltakspenger.libs.httpklient.infra.transport.HttpTransport
import no.nav.tiltakspenger.libs.httpklient.infra.transport.JavaHttpTransport
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.BruktNavkontorKlient
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.KanIkkeHenteNavkontor
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.Navkontor
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.NavkontorMedMetadata
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.tilKlientkall
import java.net.URI
import java.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Gammel navkontor-klient (veilarboppfolging) som er på vei ut - erstattes av [KontorhistorikkHttpklient] når vi har verifisert at den nye tjenesten leverer riktige data.
 *
 * Kildekode: https://github.com/navikt/veilarboppfolging
 * Dokumentasjon: -
 * API-spec: -
 * Slack: #team_dab_arbeidsoppfølging
 * Teamkatalog: https://teamkatalogen.nav.no/team/1ad2c9ea-3221-4666-93f3-fe6f7cae94ef
 *
 * Brukes kun av [SammenligningVeilarboppfolgingKlient], som er eneste konsument av begge klientene.
 *
 * Feillogging skjer ikke her, men i [SammenligningVeilarboppfolgingKlient], som har domenekonteksten (loggkontekst med sakId/saksnummer/...).
 * Klienten bærer derfor httpklient sine rå typer videre til domenet: [HttpKlientError] på feilstiene ([KanIkkeHenteNavkontor.httpKlientError]) og [no.nav.tiltakspenger.libs.httpklient.HttpKlientMetadata] på suksess ([NavkontorMedMetadata.httpKlientMetadata]).
 */
class VeilarboppfolgingHttpClient(
    baseUrl: String,
    authTokenProvider: AuthTokenProvider,
    connectTimeout: Duration = 1.seconds,
    timeout: Duration = 1.seconds,
    clock: Clock,
    transport: HttpTransport = JavaHttpTransport(connectTimeout = connectTimeout),
) {
    private val httpKlient: HttpKlient = HttpKlient(
        clock = clock,
        config = HttpKlientConfig(
            timeout = timeout,
            auth = KlientAuth.System(authTokenProvider),
        ),
        transport = transport,
    )

    private val uri = URI.create("$baseUrl/veilarboppfolging/api/v2/person/system/hent-oppfolgingsstatus")

    suspend fun hentOppfolgingsenhet(
        fnr: Fnr,
    ): Either<KanIkkeHenteNavkontor, NavkontorMedMetadata> {
        // Veilarboppfolging svarer alltid 200 ved suksess; alt annet skal være feil (paritet med gammel successStatus).
        return httpKlient.postJson<Response>(
            uri = uri,
            body = Request(fnr.verdi),
            headere = listOf(NavHeadere.navConsumerId("tiltakspenger-saksbehandling-api")),
            godta = Statusregel.Eksakt(200),
        ).mapLeft { error ->
            when (error) {
                is HttpKlientError.UventetStatus -> KanIkkeHenteNavkontor.UventetHttpStatus(error)

                is HttpKlientError.RequestIkkeSendt,
                is HttpKlientError.IngenRespons,
                is HttpKlientError.DeserializationError,
                -> KanIkkeHenteNavkontor.KallFeilet(error)
            }
        }.flatMap { response ->
            val oppfolgingsenhet = response.body.oppfolgingsenhet
            if (oppfolgingsenhet == null) {
                KanIkkeHenteNavkontor.ManglerOppfolgingsenhet(httpKlientMetadata = response.metadata).left()
            } else {
                NavkontorMedMetadata(
                    navkontor = oppfolgingsenhet.toNavkontor(),
                    brukteKlient = BruktNavkontorKlient.VEILARBOPPFOLGING,
                    veilarboppfolgingKall = response.metadata.tilKlientkall(),
                    httpKlientMetadata = response.metadata,
                ).right()
            }
        }
    }
}

private data class Request(
    val fnr: String,
)

/** Kun ment brukt av testene utenfor denne fila (konstrueres direkte i `HttpKlientFake.enqueueResponse`). */
data class Response(
    val oppfolgingsenhet: Oppfolgingsenhet?,
    val veilederId: String?,
    val formidlingsgruppe: String?,
    val servicegruppe: String?,
    val hovedmaalkode: String?,
)

/** Kun ment brukt av testene utenfor denne fila. */
data class Oppfolgingsenhet(
    val navn: String,
    val enhetId: String,
) {
    fun toNavkontor(): Navkontor =
        Navkontor(
            kontornummer = enhetId,
            kontornavn = navn,
        )
}
