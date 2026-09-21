package no.nav.tiltakspenger.saksbehandling.utbetaling.infra.http.utbetalingsoversikt

import arrow.core.Either
import arrow.core.flatMap
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.personopplysning.Fnr
import no.nav.tiltakspenger.libs.httpklient.HttpKlientError
import no.nav.tiltakspenger.libs.httpklient.HttpKlientResponse
import no.nav.tiltakspenger.libs.httpklient.UriSynlighet
import no.nav.tiltakspenger.libs.httpklient.infra.HttpKlient
import no.nav.tiltakspenger.libs.httpklient.infra.HttpKlientConfig
import no.nav.tiltakspenger.libs.httpklient.infra.kall.AuthTokenProvider
import no.nav.tiltakspenger.libs.httpklient.infra.kall.KlientAuth
import no.nav.tiltakspenger.libs.httpklient.infra.kall.NavHeadere
import no.nav.tiltakspenger.libs.httpklient.infra.kall.SerialisertJson
import no.nav.tiltakspenger.libs.httpklient.infra.kall.Statusregel
import no.nav.tiltakspenger.libs.httpklient.infra.retry.Retry
import no.nav.tiltakspenger.libs.httpklient.infra.transport.HttpTransport
import no.nav.tiltakspenger.libs.httpklient.infra.transport.JavaHttpTransport
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.utbetalingsoversikt.KunneIkkeHenteUtbetalingsoversikt
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.utbetalingsoversikt.Oppslagsperiodetype
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.utbetalingsoversikt.RegistrertUtbetaling
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.utbetalingsoversikt.Utbetalingsoversiktklient
import java.net.URI
import java.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Klient mot sokos-utbetaldata for oppfølging av egne utbetalinger.
 *
 * Kildekode: https://github.com/navikt/sokos-utbetaldata
 * API-spec: https://sokos-utbetaldata.dev.intern.nav.no/utbetaldata/api/v2/docs og https://github.com/navikt/sokos-utbetaldata/blob/main/src/main/resources/spec/utbetaldata-v2-openapi-spec.yaml
 * Slack: #po-utbetaling
 * Teamkatalog: https://teamkatalogen.nav.no/team/6260623b-d58e-4c17-8861-0a7b92fdc1e2
 *
 * Tidsbudsjett: ett forsøk med 5 s oppkobling og 20 s svar gir verste tilfelle 25 s per kall.
 * Kalleren er en bakgrunnsjobb uten ventende bruker, og jobben prøver saken igjen senere, så klienten har ingen retry.
 * Svarer tjenesten 401, gjør httpklient ett forsøk til med ferskt token.
 * URI-en har ingen personopplysninger, siden identen ligger i bodyen.
 *
 * Slik svarer tjenesten (lest i kildekoden 2026-09-14), og slik klassifiserer vi svarene:
 * - 200 med `[]` betyr ingen treff; applikasjonen svarer ikke 404.
 * - 400 betyr at requesten for denne personen ble avvist, og gir [KunneIkkeHenteUtbetalingsoversikt.SakAvvist].
 * - 401 og 403 betyr at tjenesten avviser oss, også når vi er tatt ut av allowlisten for intern-endepunktet, og gir [KunneIkkeHenteUtbetalingsoversikt.TilgangAvvist].
 * - 500 har samme tekst uansett årsak, og gir [KunneIkkeHenteUtbetalingsoversikt.Tjenestefeil] sammen med andre statuser, timeout og nettverksfeil.
 * - Tjenesten sier ikke fra om at økonomisystemet er stengt, i motsetning til simuleringen hos helved, som svarer 503.
 * - Tjenesten har ingen rate limiting og ingen timeout mot databasene sine, og eierteamet varsles ved mer enn to 4xx eller 5xx på tre minutter.
 *
 * Kalleren må derfor holde seg innenfor åpningstidene og stoppe ved første feil som rammer alle saker.
 */
class UtbetalingsoversiktHttpKlient(
    baseUrl: String,
    clock: Clock,
    authTokenProvider: AuthTokenProvider,
    connectTimeout: Duration = 5.seconds,
    timeout: Duration = 20.seconds,
    transport: HttpTransport = JavaHttpTransport(connectTimeout = connectTimeout),
) : Utbetalingsoversiktklient {
    private val httpKlient: HttpKlient = HttpKlient(
        clock = clock,
        config = HttpKlientConfig(
            timeout = timeout,
            auth = KlientAuth.System(authTokenProvider),
            retry = Retry.Ingen,
            uriSynlighet = UriSynlighet.VanligLogg,
        ),
        transport = transport,
    )

    private val uri = URI.create("$baseUrl/utbetaldata/api/v2/hent-utbetalingsinformasjon/intern")

    override suspend fun hent(
        fnr: Fnr,
        periode: Periode,
        periodetype: Oppslagsperiodetype,
        correlationId: CorrelationId,
    ): Either<KunneIkkeHenteUtbetalingsoversikt, HttpKlientResponse<List<RegistrertUtbetaling>>> {
        val payload = serialize(
            UtbetalingsoversiktRequestDto(
                ident = fnr.verdi,
                // Vi spør om utbetalinger personen har rett på, ikke om utbetalinger som gikk til personen.
                rolle = "RETTIGHETSHAVER",
                periode = UtbetalingsoversiktDto.UtbetalingsperiodeDto(fom = periode.fraOgMed, tom = periode.tilOgMed),
                periodetype = periodetype.tilRequestverdi(),
            ),
        )
        return httpKlient.postJson<List<UtbetalingsoversiktDto>>(
            uri = uri,
            body = SerialisertJson(payload),
            headere = listOf(NavHeadere.navCallId(correlationId.value)),
            godta = Statusregel.Eksakt(200),
        ).mapLeft { it.tilFeil() }.flatMap { response ->
            response.body.tilRegistrerteUtbetalinger()
                .mapLeft { KunneIkkeHenteUtbetalingsoversikt.UgyldigInnhold(feil = it, metadata = response.metadata) }
                .map { HttpKlientResponse(statusCode = response.statusCode, body = it, metadata = response.metadata) }
        }
    }

    private fun Oppslagsperiodetype.tilRequestverdi(): String = when (this) {
        Oppslagsperiodetype.UTBETALINGSPERIODE -> "UTBETALINGSPERIODE"
        Oppslagsperiodetype.YTELSESPERIODE -> "YTELSESPERIODE"
    }

    private fun HttpKlientError.tilFeil(): KunneIkkeHenteUtbetalingsoversikt = when (this) {
        is HttpKlientError.UventetStatus -> when {
            statusCode == 401 || statusCode == 403 -> KunneIkkeHenteUtbetalingsoversikt.TilgangAvvist(this)
            statusCode == 400 -> KunneIkkeHenteUtbetalingsoversikt.SakAvvist(this)
            else -> KunneIkkeHenteUtbetalingsoversikt.Tjenestefeil(this)
        }

        is HttpKlientError.DeserializationError -> KunneIkkeHenteUtbetalingsoversikt.UleseligSvar(this)

        is HttpKlientError.IngenRespons, is HttpKlientError.RequestIkkeSendt -> KunneIkkeHenteUtbetalingsoversikt.Tjenestefeil(this)
    }
}
