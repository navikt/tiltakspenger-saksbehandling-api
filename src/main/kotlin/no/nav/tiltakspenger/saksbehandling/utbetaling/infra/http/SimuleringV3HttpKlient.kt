package no.nav.tiltakspenger.saksbehandling.utbetaling.infra.http

import arrow.core.Either
import arrow.core.flatMap
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksnummer
import no.nav.tiltakspenger.libs.common.Ulid
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.common.personopplysning.Fnr
import no.nav.tiltakspenger.libs.httpklient.HttpKlientError
import no.nav.tiltakspenger.libs.httpklient.infra.HttpKlient
import no.nav.tiltakspenger.libs.httpklient.infra.HttpKlientConfig
import no.nav.tiltakspenger.libs.httpklient.infra.kall.AuthTokenProvider
import no.nav.tiltakspenger.libs.httpklient.infra.kall.Header
import no.nav.tiltakspenger.libs.httpklient.infra.kall.KlientAuth
import no.nav.tiltakspenger.libs.httpklient.infra.kall.SerialisertJson
import no.nav.tiltakspenger.libs.httpklient.infra.kall.Statusregel
import no.nav.tiltakspenger.libs.httpklient.infra.transport.HttpTransport
import no.nav.tiltakspenger.libs.httpklient.infra.transport.JavaHttpTransport
import no.nav.tiltakspenger.libs.httpklient.tryMap
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.saksbehandling.beregning.Beregning
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldeperiode.MeldeperiodeKjeder
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.Navkontor
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.KunneIkkeSimulere
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.SimuleringMedMetadata
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.V3Simuleringsklient
import no.nav.tiltakspenger.saksbehandling.utbetaling.infra.kafka.tilHelvedUtbetalingDTO
import tools.jackson.databind.JsonNode
import java.net.URI
import java.time.Clock
import java.util.UUID
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * V3-simulering bruker Hel Veds direkte synkrone `POST /api/simulering` og sender samme sakstilstand som V3-iverksetting med `dryrun = true`.
 *
 * Kildekode: https://github.com/navikt/helved-utbetaling/blob/main/apps/simulering/main/simulering/DryrunRoutes.kt
 * Svarmodeller: https://github.com/navikt/helved-utbetaling/blob/main/models/main/models/Simulering.kt
 * Dokumentasjon: https://github.com/navikt/helved-utbetaling/blob/main/dokumentasjon/simulering.md
 * OpenAPI-fila beskriver eldre `/api/dryrun`-ruter, ikke denne direkte ruta: https://github.com/navikt/helved-utbetaling/blob/main/dokumentasjon/openapi-dryrun.yml
 * Slack: #team-hel-ved
 * Teamkatalog: https://teamkatalogen.nav.no/team/93e78ae9-babc-452c-965d-e7dc67af8612
 *
 * En HTTP-timeout gir tjenesten 130 sekunder, med ti sekunders margin over Hel Veds synkrone svarventing på opptil 120 sekunder.
 */
class SimuleringV3HttpKlient(
    private val baseUrl: String,
    private val clock: Clock,
    authTokenProvider: AuthTokenProvider,
    connectTimeout: Duration = 5.seconds,
    timeout: Duration = 130.seconds,
    transport: HttpTransport = JavaHttpTransport(connectTimeout = connectTimeout),
) : V3Simuleringsklient {
    private val klient = HttpKlient(
        clock = clock,
        config = HttpKlientConfig(
            timeout = timeout,
            auth = KlientAuth.System(authTokenProvider),
        ),
        transport = transport,
    )
    private val simuleringUri = URI.create("$baseUrl/api/simulering")

    override suspend fun simuler(
        sakId: SakId,
        saksnummer: Saksnummer,
        behandlingId: Ulid,
        fnr: Fnr,
        beregning: Beregning,
        forrigeUtbetalingJson: String?,
        meldeperiodeKjeder: MeldeperiodeKjeder,
        saksbehandler: String,
        brukersNavkontor: Navkontor,
    ): Either<KunneIkkeSimulere, SimuleringMedMetadata> {
        val jsonPayload = beregning.tilHelvedUtbetalingDTO(
            saksnummer = saksnummer,
            behandlingId = behandlingId,
            fnr = fnr,
            vedtakstidspunkt = nå(clock),
            brukersNavkontor = brukersNavkontor,
            saksbehandler = saksbehandler,
            beslutter = null,
            forrigeUtbetalingJson = forrigeUtbetalingJson,
            dryrun = true,
        ).let { serialize(it) }

        return klient.postJson<JsonNode>(
            uri = simuleringUri,
            body = SerialisertJson(jsonPayload),
            headere = listOf(Header("Transaction-ID", UUID.randomUUID().toString())),
            godta = Statusregel.Eksakt(200),
        ).mapLeft { it.tilKunneIkkeSimulere() }
            .flatMap { response ->
                response.tryMap {
                    lesHelvedSimuleringsresponsV3(
                        responseBody = response.body,
                        originalResponseBody = response.rawResponseString,
                        sakId = sakId,
                        saksnummer = saksnummer,
                        meldeperiodeKjeder = meldeperiodeKjeder,
                        clock = clock,
                        simuleringstidspunkt = UtbetalingHttpKlient.simuleringstidspunkt(
                            response.metadata.tidsstempler.responsMottatt,
                            clock,
                        ),
                    )
                }
                    .mapLeft { it.tilKunneIkkeSimulere() }
                    .flatMap { tolket ->
                        tolket
                            .mapLeft { KunneIkkeSimulere.UgyldigSimulering(it) }
                            .map { simulering ->
                                SimuleringMedMetadata(
                                    simulering = simulering,
                                    originalResponseBody = response.rawResponseString,
                                )
                            }
                    }
            }
    }

    private fun HttpKlientError.tilKunneIkkeSimulere(): KunneIkkeSimulere = when (this) {
        is HttpKlientError.Timeout -> KunneIkkeSimulere.Timeout(this)
        is HttpKlientError.UventetStatus if statusCode == 408 -> KunneIkkeSimulere.Timeout(this)
        else -> KunneIkkeSimulere.UkjentFeil(this)
    }
}
