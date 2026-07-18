package no.nav.tiltakspenger.saksbehandling.journalpost.infra

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.left
import arrow.core.right
import no.nav.tiltakspenger.libs.common.AccessToken
import no.nav.tiltakspenger.libs.httpklient.HttpKlientError
import no.nav.tiltakspenger.libs.httpklient.HttpKlientMetadata
import no.nav.tiltakspenger.libs.httpklient.authFeilUtenKall
import no.nav.tiltakspenger.libs.httpklient.infra.HttpKlient
import no.nav.tiltakspenger.libs.httpklient.infra.HttpKlientConfig
import no.nav.tiltakspenger.libs.httpklient.infra.kall.AuthTokenProvider
import no.nav.tiltakspenger.libs.httpklient.infra.kall.Header
import no.nav.tiltakspenger.libs.httpklient.infra.kall.KlientAuth
import no.nav.tiltakspenger.libs.httpklient.infra.kall.NavHeadere
import no.nav.tiltakspenger.libs.httpklient.infra.kall.Statusregel
import no.nav.tiltakspenger.libs.httpklient.infra.retry.Retry
import no.nav.tiltakspenger.libs.httpklient.infra.transport.HttpTransport
import no.nav.tiltakspenger.libs.httpklient.infra.transport.JavaHttpTransport
import no.nav.tiltakspenger.libs.texas.IdentityProvider
import no.nav.tiltakspenger.libs.texas.client.TexasClient
import no.nav.tiltakspenger.saksbehandling.dokument.PdfA
import no.nav.tiltakspenger.saksbehandling.infra.graphql.GraphQLResponse
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId
import no.nav.tiltakspenger.saksbehandling.journalpost.HentDokumentCommand
import java.net.URI
import java.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * Klient mot SAF (sak- og arkivfasade) for å hente journalposter og dokumenter.
 *
 * Kildekode: https://github.com/navikt/saf
 * Dokumentasjon: https://confluence.adeo.no/display/BOA/saf og https://confluence.adeo.no/spaces/BOA/pages/297076302/saf+-+Tjenester
 * API-spec: -
 * Slack: #team-dokumentløsninger
 * Teamkatalog: https://teamkatalogen.nav.no/team/f3388fcd-898e-40da-8d02-0bf1e3a79120
 *
 * [hentJournalpost] bruker systemtoken ([authTokenProvider]), mens [hentDokument] veksler saksbehandlerens token til et OBO-token per kall via [texasClient].
 *
 * Klienten logger ikke selv: feillogging skjer én gang i kallende service ([no.nav.tiltakspenger.saksbehandling.journalpost.ValiderJournalpostService] og [no.nav.tiltakspenger.saksbehandling.klage.service.VisInnstillingsbrevKlagebehandlingService]), som i tillegg har domenekonteksten.
 *
 * Retryen replikerer den gamle ktor-klienten (`httpClientWithRetry`): fire forsøk totalt med konstant 100 ms delay.
 * retryIkkeIdempotente er satt fordi GraphQL-oppslaget går som POST, men er et rent leseoppslag uten sideeffekter.
 *
 * @param transport Nettverks-sømmen til [HttpKlient]; default er produksjonstransporten, tester sender inn `FakeHttpTransport` slik at hele den reelle pipelinen kjører.
 */
class SafJournalpostHttpClient(
    private val baseUrl: String,
    private val safScope: String,
    private val texasClient: TexasClient,
    authTokenProvider: AuthTokenProvider,
    clock: Clock,
    connectTimeout: Duration = 5.seconds,
    timeout: Duration = 60.seconds,
    transport: HttpTransport = JavaHttpTransport(connectTimeout = connectTimeout),
) : SafJournalpostClient {
    private val httpKlient: HttpKlient = HttpKlient(
        clock = clock,
        config = HttpKlientConfig(
            timeout = timeout,
            auth = KlientAuth.System(authTokenProvider),
            retry = Retry.Fast(maksForsøk = 4, delay = 100.milliseconds, retryIkkeIdempotente = true),
        ),
        transport = transport,
    )

    private val graphqlUri = URI.create("$baseUrl/graphql")

    private val journalPostQuery =
        SafJournalpostClient::class
            .java
            .getResource("/saf/hentJournalpost.graphql")!!
            .readText()
            .replace(Regex("[\n\t]"), "")

    override suspend fun hentJournalpost(
        journalpostId: JournalpostId,
    ): Either<KanIkkeHenteJournalpost, Journalpost?> {
        return httpKlient.postJson<GraphQLResponse<HentJournalpostResponse>>(
            uri = graphqlUri,
            body = FindJournalpostRequest(
                query = journalPostQuery,
                variables = Variables(journalpostId.toString()),
            ),
            headere = listOf(Header("X-Correlation-ID", journalpostId.toString())),
            godta = Statusregel.Eksakt(200),
        ).mapLeft {
            KanIkkeHenteJournalpost.KallFeilet(it)
        }.flatMap { respons ->
            respons.body.tilJournalpost(respons.metadata)
        }
    }

    /**
     * GraphQL svarer av design 200 OK på alle svar; funksjonelle feil ligger i errors-lista.
     * `not_found`/`bad_request` betyr «finnes ikke» og gir `Right(null)`; øvrige feilkoder (`forbidden`, `server_error`) er reelle feil og gir Left.
     * https://confluence.adeo.no/spaces/BOA/pages/309563246/saf+-+Utviklerveiledning#safUtviklerveiledning-Feilh%C3%A5ndtering
     *
     * En journalpost uten `datoOpprettet` behandles også som «finnes ikke» — paritet med den gamle klienten, som brukte feltet som guard mot ufullstendige svar.
     */
    private fun GraphQLResponse<HentJournalpostResponse>.tilJournalpost(
        metadata: HttpKlientMetadata,
    ): Either<KanIkkeHenteJournalpost, Journalpost?> {
        val graphQLFeil = errors.orEmpty()
        if (graphQLFeil.isNotEmpty()) {
            return if (graphQLFeil.all { it.extensions?.code == "not_found" || it.extensions?.code == "bad_request" }) {
                null.right()
            } else {
                KanIkkeHenteJournalpost.GraphQLFeil(
                    feilkoder = graphQLFeil.map { it.extensions?.code ?: "ukjent" },
                    httpKlientMetadata = metadata,
                ).left()
            }
        }
        return data?.journalpost?.takeIf { it.datoOpprettet != null }.right()
    }

    override suspend fun hentDokument(
        command: HentDokumentCommand,
    ): Either<HttpKlientError, PdfA> {
        return exchangeOboToken(command.saksbehandlerToken).flatMap { oboToken ->
            httpKlient.getPdf(
                uri = URI.create("$baseUrl/rest/hentdokument/${command.journalpostId}/${command.dokumentInfoId}/ARKIV"),
                headere = listOf(
                    Header("X-Correlation-ID", command.correlationId.value),
                    // SAF bruker «Nav-Callid»-varianten (uten bindestrek før Id); navCallId ville gitt det andre headernavnet «Nav-Call-Id».
                    NavHeadere.navCallid(command.correlationId.value),
                ),
                bearerToken = oboToken,
                godta = Statusregel.Eksakt(200),
            ).map { PdfA(it.body) }
        }
    }

    private suspend fun exchangeOboToken(saksbehandlerToken: String): Either<HttpKlientError, AccessToken> {
        return Either
            .catch {
                texasClient.exchangeToken(
                    userToken = saksbehandlerToken,
                    audienceTarget = safScope,
                    identityProvider = IdentityProvider.AZUREAD,
                )
            }
            // OBO-vekslingen feiler før noe HTTP-kall er gjort; authFeilUtenKall gir samme form som klientens egne auth-feil.
            .mapLeft(::authFeilUtenKall)
    }
}

data class FindJournalpostRequest(val query: String, val variables: Variables)

data class Variables(val id: String)

data class HentJournalpostResponse(
    val journalpost: Journalpost?,
)
