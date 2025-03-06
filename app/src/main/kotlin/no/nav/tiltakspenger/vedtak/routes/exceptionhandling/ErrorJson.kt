package no.nav.tiltakspenger.vedtak.routes.exceptionhandling

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respond

data class ErrorJson(
    val melding: String,
    val kode: String,
)

suspend fun ApplicationCall.respond403Forbidden(melding: String, kode: String) {
    this.respondError(
        status = HttpStatusCode.Forbidden,
        melding = melding,
        kode = kode,
    )
}

suspend fun ApplicationCall.respond403Forbidden(errorJson: ErrorJson) {
    this.respondError(HttpStatusCode.Forbidden, errorJson)
}

suspend fun ApplicationCall.respond401Unauthorized(melding: String, kode: String) {
    this.respondError(
        status = HttpStatusCode.Unauthorized,
        melding = melding,
        kode = kode,
    )
}

suspend fun ApplicationCall.respond401Unauthorized(errorJson: ErrorJson) {
    this.respondError(HttpStatusCode.Unauthorized, errorJson)
}

suspend fun ApplicationCall.respond500InternalServerError(melding: String, kode: String) {
    this.respondError(
        status = HttpStatusCode.InternalServerError,
        melding = melding,
        kode = kode,
    )
}
suspend fun ApplicationCall.respond500InternalServerError(errorJson: ErrorJson) {
    this.respondError(HttpStatusCode.InternalServerError, errorJson)
}

suspend fun ApplicationCall.respond400BadRequest(melding: String, kode: String) {
    this.respondError(
        status = HttpStatusCode.BadRequest,
        melding = melding,
        kode = kode,
    )
}
suspend fun ApplicationCall.respond400BadRequest(errorJson: ErrorJson) {
    this.respondError(HttpStatusCode.BadRequest, errorJson)
}

suspend fun ApplicationCall.respond404NotFound(errorJson: ErrorJson) {
    this.respondError(HttpStatusCode.NotFound, errorJson)
}

suspend fun ApplicationCall.respondError(status: HttpStatusCode, melding: String, kode: String) {
    this.respondError(
        status = status,
        errorJson = ErrorJson(
            melding = melding,
            kode = kode,
        ),
    )
}

suspend fun ApplicationCall.respondError(status: HttpStatusCode, errorJson: ErrorJson) {
    this.respond(
        message = errorJson,
        status = status,
    )
}

suspend fun ApplicationCall.respond501NotImplemented(melding: String, kode: String) {
    this.respondError(HttpStatusCode.NotImplemented, ErrorJson(melding, kode))
}

suspend fun ApplicationCall.respond501NotImplemented(errorJson: ErrorJson) {
    this.respondError(HttpStatusCode.NotImplemented, errorJson)
}
