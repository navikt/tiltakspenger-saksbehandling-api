package no.nav.tiltakspenger.saksbehandling.infra.repo

import arrow.core.Either
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.withCharset
import io.ktor.server.application.ApplicationCall
import io.ktor.server.plugins.callid.callId
import io.ktor.server.request.receiveText
import io.ktor.server.response.respondText
import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.SøknadId
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.libs.ktor.common.respond400BadRequest
import no.nav.tiltakspenger.libs.logging.Sikkerlogg
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeId
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeKjedeId
import no.nav.tiltakspenger.saksbehandling.klage.domene.KlagebehandlingId
import no.nav.tiltakspenger.saksbehandling.sak.Saksnummer

private val logger = KotlinLogging.logger {}

internal fun ApplicationCall.correlationId(): CorrelationId {
    return this.callId?.let { CorrelationId(it) } ?: CorrelationId.generate()
}

internal suspend inline fun ApplicationCall.withSaksnummer(
    crossinline onRight: suspend (Saksnummer) -> Unit,
) {
    withValidParam(
        paramName = "saksnummer",
        parse = ::Saksnummer,
        errorMessage = "Ugyldig saksnummer",
        errorCode = "ugyldig_saksnummer",
        onSuccess = onRight,
    )
}

internal suspend inline fun ApplicationCall.withSakId(
    crossinline onRight: suspend (SakId) -> Unit,
) {
    withValidParam(
        paramName = "sakId",
        parse = SakId::fromString,
        errorMessage = "Ugyldig sak id",
        errorCode = "ugyldig_sak_id",
        onSuccess = onRight,
    )
}

internal suspend inline fun ApplicationCall.withSøknadId(
    crossinline onRight: suspend (SøknadId) -> Unit,
) {
    withValidParam(
        paramName = "søknadId",
        parse = SøknadId::fromString,
        errorMessage = "Ugyldig søknad id",
        errorCode = "ugyldig_søknad_id",
        onSuccess = onRight,
    )
}

internal suspend inline fun ApplicationCall.withMeldekortId(
    crossinline onRight: suspend (MeldekortId) -> Unit,
) {
    withValidParam(
        paramName = "meldekortId",
        parse = MeldekortId::fromString,
        errorMessage = "Ugyldig meldekort id",
        errorCode = "ugyldig_meldekort_id",
        onSuccess = onRight,
    )
}

internal suspend inline fun ApplicationCall.withMeldeperiodeKjedeId(
    crossinline onRight: suspend (MeldeperiodeKjedeId) -> Unit,
) {
    withValidParam(
        paramName = "kjedeId",
        parse = { MeldeperiodeKjedeId(it) },
        errorMessage = "Ugyldig meldeperiode-kjede id",
        errorCode = "ugyldig_meldeperiodekjede_id",
        onSuccess = onRight,
    )
}

@Suppress("unused")
internal suspend inline fun ApplicationCall.withMeldeperiodeId(
    crossinline onRight: suspend (MeldeperiodeId) -> Unit,
) {
    withValidParam(
        paramName = "meldeperiodeId",
        parse = MeldeperiodeId::fromString,
        errorMessage = "Ugyldig meldeperiode id",
        errorCode = "ugyldig_meldeperiode_id",
        onSuccess = onRight,
    )
}

internal suspend inline fun ApplicationCall.withKlagebehandlingId(
    crossinline onRight: suspend (KlagebehandlingId) -> Unit,
) {
    withValidParam(
        paramName = "klagebehandlingId",
        parse = KlagebehandlingId::fromString,
        errorMessage = "Ugyldig klagebehandlingbehandling id",
        errorCode = "ugyldig_klagebehandling_id",
        onSuccess = onRight,
    )
}

internal suspend inline fun ApplicationCall.withBehandlingId(
    crossinline onRight: suspend (BehandlingId) -> Unit,
) {
    withValidParam(
        paramName = "behandlingId",
        parse = BehandlingId::fromString,
        errorMessage = "Ugyldig behandling id",
        errorCode = "ugyldig_behandling_id",
        onSuccess = onRight,
    )
}

internal suspend inline fun <reified T> ApplicationCall.withBody(
    crossinline ifRight: suspend (T) -> Unit,
) {
    Either.catch {
        val body = this.receiveText()
        no.nav.tiltakspenger.libs.json.deserialize<T>(body)
    }.onLeft {
        logger.debug(RuntimeException("Trigger stacktrace for enklere debug")) { "Feil ved deserialisering av request. Se sikkerlogg for mer kontekst." }
        Sikkerlogg.error(it) { "Feil ved deserialisering av request" }
        this.respond400BadRequest(
            melding = "Kunne ikke deserialisere request",
            kode = "ugyldig_request",
        )
    }.onRight { ifRight(it) }
}

internal suspend inline fun ApplicationCall.respondStatus(status: HttpStatusCode) {
    this.respondText("", status = status)
}

internal suspend inline fun ApplicationCall.respondOk() {
    this.respondText("", status = HttpStatusCode.OK)
}

internal suspend inline fun ApplicationCall.respondNoContent() {
    this.respondText("", status = HttpStatusCode.NoContent)
}

/**
 * @param json ferdigserialisert JSON-string. Obs: Det gjøres ingen validering på om dette er gyldig JSON.
 *
 * Defaulter til 200 OK og Content-Type: application/json; charset=UTF-8
 */
internal suspend inline fun ApplicationCall.respondJsonString(
    json: String,
    status: HttpStatusCode = HttpStatusCode.OK,
) {
    this.respondText(
        text = json,
        contentType = ContentType.Application.Json.withCharset(Charsets.UTF_8),
        status = status,
    )
}

@Suppress("unused", "RedundantSuspendModifier", "UnusedReceiverParameter")
@Deprecated(
    message = "Bruk respondJson(json = ...) for ferdigserialiserte strenger",
    level = DeprecationLevel.ERROR,
)
internal suspend inline fun ApplicationCall.respondJson(
    value: String,
    status: HttpStatusCode = HttpStatusCode.OK,
): Nothing = error("Bruk respondJson(json = ...) for ferdigserialiserte strenger")

/**
 * Defaulter til 200 OK og Content-Type: application/json; charset=UTF-8
 * @throws IllegalArgumentException hvis T er String. Bruk respondText(json = ...) for ferdigserialiserte strenger
 */
internal suspend inline fun <reified T : Any> ApplicationCall.respondJson(
    value: T,
    status: HttpStatusCode = HttpStatusCode.OK,
) {
    require(value !is String) {
        "Bruk respondText(json = ...) for ferdigserialiserte strenger"
    }
    respondJsonString(json = serialize(value), status = status)
}

/**
 * Defaulter til 200 OK og Content-Type: application/json; charset=UTF-8
 */
internal suspend inline fun <reified T : Any> ApplicationCall.respondJson(
    valueAndStatus: Pair<HttpStatusCode, T>,
) {
    respondJson(
        value = valueAndStatus.second,
        status = valueAndStatus.first,
    )
}

internal suspend inline fun <T> ApplicationCall.withValidParam(
    paramName: String,
    parse: (String) -> T,
    errorMessage: String,
    errorCode: String,
    crossinline onSuccess: suspend (T) -> Unit,
) {
    Either.catch {
        parse(this.parameters[paramName]!!)
    }.fold(
        ifLeft = {
            logger.debug(it) { "Feil ved parsing av parameter $paramName. errorMessage: $errorMessage, errorCode: $errorCode" }
            this.respond400BadRequest(
                melding = errorMessage,
                kode = errorCode,
            )
        },
        ifRight = { onSuccess(it) },
    )
}
