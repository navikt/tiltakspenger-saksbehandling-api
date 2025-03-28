package no.nav.tiltakspenger.saksbehandling.routes

import arrow.core.Either
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.server.application.ApplicationCall
import io.ktor.server.plugins.callid.callId
import io.ktor.server.request.receiveText
import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.MeldeperiodeId
import no.nav.tiltakspenger.libs.common.MeldeperiodeKjedeId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.SøknadId
import no.nav.tiltakspenger.libs.ktor.common.respond400BadRequest
import no.nav.tiltakspenger.saksbehandling.felles.sikkerlogg
import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.sak.Saksnummer

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
        no.nav.tiltakspenger.libs.json.deserialize<T>(this.receiveText())
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
