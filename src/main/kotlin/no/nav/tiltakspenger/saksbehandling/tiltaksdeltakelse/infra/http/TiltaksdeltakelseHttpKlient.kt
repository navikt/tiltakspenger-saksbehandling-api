package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.http

import arrow.core.Either
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.httpklient.AuthTokenProvider
import no.nav.tiltakspenger.libs.httpklient.HttpKlient
import no.nav.tiltakspenger.libs.httpklient.HttpKlientError
import no.nav.tiltakspenger.libs.httpklient.post
import no.nav.tiltakspenger.libs.tiltak.TiltakshistorikkDTO
import no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger.TiltaksdeltakelserDetErSøktTiltakspengerFor
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakelseMedArrangørnavn
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.TiltaksdeltakelseKlient
import java.net.URI
import java.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Klient mot tiltakspenger-tiltak for å hente tiltaksdeltakelser, bygget på den felles [HttpKlient]-modulen i tiltakspenger-libs.
 *
 * Kildekode: https://github.com/navikt/tiltakspenger-tiltak
 * Dokumentasjon: README-en i kildekode-repoet
 * API-spec: -
 * Slack: #tiltakspenger-værsågod (eget team)
 * Teamkatalog: https://teamkatalogen.nav.no/team/15bca3d2-2584-4167-85ba-faab1f1cfb53
 *
 * Klienten logger bevisst ikke selv: den returnerer [HttpKlientError] uendret, og feillogging gjøres én gang i kallende service ([no.nav.tiltakspenger.saksbehandling.behandling.service.behandling.HentSaksopplysingerService], [no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.service.TiltaksdeltakelseService]), som i tillegg har domenekonteksten.
 *
 * [httpKlient] bygges som default ut fra parametrene over ([clock], [authTokenProvider], [connectTimeout], [defaultTimeout]) slik at hele klientoppsettet kan leses ett sted.
 * Sender man inn en egen [httpKlient] (typisk `HttpKlientFake` i test), **ignoreres** de parametrene som kun brukes til å bygge default-klienten.
 *
 * @param clock Klokke som sendes videre til [HttpKlient]. Ignoreres hvis [httpKlient] sendes inn.
 * @param authTokenProvider Henter system-token mot tiltakspenger-tiltak. Ignoreres hvis [httpKlient] sendes inn.
 * @param connectTimeout Connect-timeout for default-klienten. Ignoreres hvis [httpKlient] sendes inn.
 * @param defaultTimeout Per-request timeout. tiltakspenger-tiltak gjør oppslag mot flere kilder, derav den høye defaulten (matcher den gamle ktor-klientens 60s).
 */
class TiltaksdeltakelseHttpKlient(
    baseUrl: String,
    clock: Clock,
    authTokenProvider: AuthTokenProvider,
    connectTimeout: Duration = 5.seconds,
    defaultTimeout: Duration = 60.seconds,
    private val httpKlient: HttpKlient = HttpKlient(clock = clock) {
        this.connectTimeout = connectTimeout
        this.defaultTimeout = defaultTimeout
        this.successStatus = { it == 200 }
        this.authTokenProvider = authTokenProvider
    },
) : TiltaksdeltakelseKlient {

    companion object {
        const val NAV_CALL_ID_HEADER = "Nav-Call-Id"
    }

    private val tiltakshistorikkUri = URI.create("$baseUrl/azure/tiltakshistorikk")

    override suspend fun hentTiltaksdeltakelser(
        fnr: Fnr,
        tiltaksdeltakelserDetErSøktTiltakspengerFor: TiltaksdeltakelserDetErSøktTiltakspengerFor,
        correlationId: CorrelationId,
    ): Either<HttpKlientError, TiltaksdeltakelserFraRegister> {
        return hentTiltakshistorikk(fnr, tiltaksdeltakelserDetErSøktTiltakspengerFor, correlationId)
            .map { mapTiltak(it) }
    }

    override suspend fun hentTiltaksdeltakelserMedArrangørnavn(
        fnr: Fnr,
        harAdressebeskyttelse: Boolean,
        correlationId: CorrelationId,
    ): Either<HttpKlientError, List<TiltaksdeltakelseMedArrangørnavn>> {
        return hentTiltakshistorikk(fnr, TiltaksdeltakelserDetErSøktTiltakspengerFor.empty(), correlationId)
            .map {
                mapTiltakMedArrangørnavn(
                    harAdressebeskyttelse = harAdressebeskyttelse,
                    tiltakDTOListe = it,
                )
            }
    }

    private suspend fun hentTiltakshistorikk(
        fnr: Fnr,
        tiltaksdeltakelserDetErSøktTiltakspengerFor: TiltaksdeltakelserDetErSøktTiltakspengerFor,
        correlationId: CorrelationId,
    ): Either<HttpKlientError, List<TiltakshistorikkDTO>> {
        return httpKlient.post<List<TiltakshistorikkDTO>>(tiltakshistorikkUri, TiltakRequestDTO(fnr.verdi)) {
            header(NAV_CALL_ID_HEADER, correlationId.value)
        }.map { response ->
            response.body
                .filter { it.harFomOgTomEllerRelevantStatus(tiltaksdeltakelserDetErSøktTiltakspengerFor) }
                .filter { it.rettPaTiltakspenger() }
        }
    }
}
