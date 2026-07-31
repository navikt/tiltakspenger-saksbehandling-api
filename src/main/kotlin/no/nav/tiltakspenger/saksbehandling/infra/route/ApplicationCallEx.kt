package no.nav.tiltakspenger.saksbehandling.infra.route

import io.ktor.server.application.ApplicationCall
import io.ktor.server.plugins.callid.callId
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.Saksnummer
import no.nav.tiltakspenger.libs.ktor.common.withValidParam
import no.nav.tiltakspenger.saksbehandling.journalpost.DokumentInfoId
import no.nav.tiltakspenger.saksbehandling.klage.domene.KlagebehandlingId
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.TilbakekrevingId

fun ApplicationCall.correlationId(): CorrelationId {
    return this.callId?.let { CorrelationId(it) } ?: CorrelationId.generate()
}

suspend inline fun ApplicationCall.withSaksnummer(
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

suspend inline fun ApplicationCall.withKlagebehandlingId(
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

suspend inline fun ApplicationCall.withTilbakekrevingId(
    crossinline onRight: suspend (TilbakekrevingId) -> Unit,
) {
    withValidParam(
        paramName = "tilbakekrevingId",
        parse = TilbakekrevingId::fromString,
        errorMessage = "Ugyldig tilbakekreving id",
        errorCode = "ugyldig_tilbakekreving_id",
        onSuccess = onRight,
    )
}

suspend inline fun ApplicationCall.withDokumentInfoId(
    crossinline onRight: suspend (DokumentInfoId) -> Unit,
) {
    withValidParam(
        paramName = "dokumentInfoId",
        parse = { id -> DokumentInfoId(id) },
        errorMessage = "Ugyldig dokumentInfoId id",
        errorCode = "ugyldig_dokumentInfoId_id",
        onSuccess = onRight,
    )
}
