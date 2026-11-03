package no.nav.tiltakspenger.saksbehandling.dagpenger.infra.http

import arrow.core.Either
import arrow.core.flatMap
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.personopplysning.Fnr
import no.nav.tiltakspenger.libs.httpklient.HttpKlientError
import no.nav.tiltakspenger.libs.httpklient.UriSynlighet
import no.nav.tiltakspenger.libs.httpklient.infra.HttpKlient
import no.nav.tiltakspenger.libs.httpklient.infra.HttpKlientConfig
import no.nav.tiltakspenger.libs.httpklient.infra.kall.AuthTokenProvider
import no.nav.tiltakspenger.libs.httpklient.infra.kall.KlientAuth
import no.nav.tiltakspenger.libs.httpklient.infra.kall.NavHeadere
import no.nav.tiltakspenger.libs.httpklient.infra.kall.SerialisertJson
import no.nav.tiltakspenger.libs.httpklient.infra.kall.Statusregel
import no.nav.tiltakspenger.libs.httpklient.infra.transport.HttpTransport
import no.nav.tiltakspenger.libs.httpklient.infra.transport.JavaHttpTransport
import no.nav.tiltakspenger.libs.httpklient.tryMap
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.saksbehandling.dagpenger.domene.Dagpengeperiode
import java.net.URI
import java.time.Clock
import java.time.LocalDate
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Klient mot dp-datadeling for å hente periodene bruker har rett til dagpenger, både fra Arena og ny dagpengeløsning.
 *
 * Kildekode: https://github.com/navikt/dp-datadeling
 * Dokumentasjon: README-en i kildekode-repoet
 * API-spec: https://navikt.github.io/dp-datadeling/openapi.html og https://github.com/navikt/dp-datadeling/blob/main/openapi/src/main/resources/datadeling-api.yaml
 * Slack: #team-dagpenger-datadeling
 * Teamkatalog: - (Team Dagpenger)
 */
class DpDatadelingHttpClient(
    baseUrl: String,
    authTokenProvider: AuthTokenProvider,
    connectTimeout: Duration = 3.seconds,
    timeout: Duration = 6.seconds,
    clock: Clock,
    transport: HttpTransport = JavaHttpTransport(connectTimeout = connectTimeout),
) : DagpengerKlient {
    private val httpKlient: HttpKlient = HttpKlient(
        clock = clock,
        config = HttpKlientConfig(
            timeout = timeout,
            auth = KlientAuth.System(authTokenProvider),
            // Fødselsnummeret sendes i bodyen, så stien inneholder ikke personopplysninger.
            uriSynlighet = UriSynlighet.VanligLogg,
        ),
        transport = transport,
    )

    private val uri = URI.create("$baseUrl/dagpenger/datadeling/v1/perioder")

    override suspend fun hentDagpengeperioder(
        fnr: Fnr,
        periode: Periode,
        correlationId: CorrelationId,
    ): Either<HttpKlientError, List<Dagpengeperiode>> {
        val jsonPayload = serialize(
            DagpengeperioderRequest(
                personIdent = fnr.verdi,
                fraOgMedDato = periode.fraOgMed,
                tilOgMedDato = periode.tilOgMed,
            ),
        )
        return httpKlient.postJson<DagpengeperioderResponse>(
            uri = uri,
            body = SerialisertJson(jsonPayload),
            headere = listOf(NavHeadere.navCallId(correlationId.value)),
            godta = Statusregel.Eksakt(200),
        )
            // En ukjent ytelsetype eller kilde blir en typet feil i stedet for en ukontrollert exception, slik at behandlingen ikke går videre uten at dagpenger er sjekket.
            .flatMap { response -> response.tryMap { body -> body.perioder.map { it.toDomain() } } }
    }
}

private data class DagpengeperioderRequest(
    val personIdent: String,
    val fraOgMedDato: LocalDate,
    val tilOgMedDato: LocalDate,
) {
    override fun toString(): String = "DagpengeperioderRequest(personIdent=***, fraOgMedDato=$fraOgMedDato, tilOgMedDato=$tilOgMedDato)"
}

/** Kun ment brukt av testene utenfor denne fila. */
data class DagpengeperioderResponse(
    val personIdent: String,
    val perioder: List<DagpengeperiodeDto>,
) {
    override fun toString(): String = "DagpengeperioderResponse(personIdent=***, perioder=$perioder)"
}

/** Kun ment brukt av testene utenfor denne fila. */
data class DagpengeperiodeDto(
    val fraOgMedDato: LocalDate,
    val tilOgMedDato: LocalDate?,
    val ytelseType: String,
    val kilde: String,
) {
    fun toDomain(): Dagpengeperiode = Dagpengeperiode(
        fraOgMed = fraOgMedDato,
        tilOgMed = tilOgMedDato,
        ytelsetype = when (ytelseType) {
            "DAGPENGER_ARBEIDSSOKER_ORDINAER" -> Dagpengeperiode.Ytelsetype.ORDINÆRE_DAGPENGER
            "DAGPENGER_PERMITTERING_ORDINAER" -> Dagpengeperiode.Ytelsetype.PERMITTERING
            "DAGPENGER_PERMITTERING_FISKEINDUSTRI" -> Dagpengeperiode.Ytelsetype.PERMITTERING_FISKEINDUSTRI
            else -> throw IllegalArgumentException("Ukjent ytelseType fra dp-datadeling: $ytelseType")
        },
        kilde = when (kilde) {
            "ARENA" -> Dagpengeperiode.Kilde.ARENA
            "DP_SAK" -> Dagpengeperiode.Kilde.DP_SAK
            else -> throw IllegalArgumentException("Ukjent kilde fra dp-datadeling: $kilde")
        },
    )
}
