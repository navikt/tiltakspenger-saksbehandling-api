package no.nav.tiltakspenger.vedtak.routes

import arrow.core.Either
import io.ktor.server.application.ApplicationCall
import io.ktor.server.plugins.callid.callId
import io.ktor.server.request.receiveText
import mu.KotlinLogging
import no.nav.tiltakspenger.felles.sikkerlogg
import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.saksbehandling.domene.sak.Saksnummer
import no.nav.tiltakspenger.vedtak.db.deserialize
import no.nav.tiltakspenger.vedtak.exceptions.UgyldigRequestException
import no.nav.tiltakspenger.vedtak.routes.exceptionhandling.respond400BadRequest

private val logger = KotlinLogging.logger {}

internal fun ApplicationCall.parameter(parameterNavn: String): String =
    this.parameters[parameterNavn] ?: throw UgyldigRequestException("$parameterNavn ikke funnet")

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
        deserialize<T>(this.receiveText())
    }.onLeft {
        logger.debug(RuntimeException("Trigger stacktrace for enklere debug")) { "Feil ved deserialisering av request. Se sikkerlogg for mer kontekst." }
        sikkerlogg.error(it) { "Feil ved deserialisering av request" }
        this.respond400BadRequest(
            melding = "Kunne ikke deserialisere request",
            kode = "ugyldig_request",
        )
    }.onRight { ifRight(it) }
}

private suspend inline fun <T> ApplicationCall.withValidParam(
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
