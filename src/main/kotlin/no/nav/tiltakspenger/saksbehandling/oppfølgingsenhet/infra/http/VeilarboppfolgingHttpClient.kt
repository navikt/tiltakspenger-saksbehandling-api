package no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.infra.http

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.left
import arrow.core.right
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.httpklient.AuthTokenProvider
import no.nav.tiltakspenger.libs.httpklient.HttpKlient
import no.nav.tiltakspenger.libs.httpklient.HttpKlientError
import no.nav.tiltakspenger.libs.httpklient.post
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
    private val httpKlient: HttpKlient = HttpKlient(clock = clock) {
        this.connectTimeout = connectTimeout
        this.defaultTimeout = timeout
        this.successStatus = { it == 200 }
        this.authTokenProvider = authTokenProvider
    },
) {
    private val uri = URI.create("$baseUrl/veilarboppfolging/api/v2/person/system/hent-oppfolgingsstatus")

    suspend fun hentOppfolgingsenhet(
        fnr: Fnr,
    ): Either<KanIkkeHenteNavkontor, NavkontorMedMetadata> {
        return httpKlient.post<Response>(uri, Request(fnr.verdi)) {
            header("Nav-Consumer-Id", "tiltakspenger-saksbehandling-api")
        }.mapLeft { error ->
            when (error) {
                is HttpKlientError.UventetStatus -> KanIkkeHenteNavkontor.UventetHttpStatus(error)
                else -> KanIkkeHenteNavkontor.KallFeilet(error)
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
