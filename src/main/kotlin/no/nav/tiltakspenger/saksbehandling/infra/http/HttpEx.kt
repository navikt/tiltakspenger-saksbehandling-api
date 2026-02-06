package no.nav.tiltakspenger.saksbehandling.infra.http

import java.net.http.HttpResponse

fun <T> HttpResponse<T>.isSuccess(): Boolean = this.statusCode() in 200..299
