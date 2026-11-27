package no.nav.tiltakspenger.saksbehandling.dev.sak.infra.route.maskinoppslag

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.server.application.ApplicationCall
import no.nav.tiltakspenger.libs.common.GenerellSystembrukerrolle
import no.nav.tiltakspenger.libs.common.GenerellSystembrukerroller
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.Saksbehandlerrolle
import no.nav.tiltakspenger.libs.common.Saksbehandlerroller
import no.nav.tiltakspenger.libs.ktor.common.respond400BadRequest
import no.nav.tiltakspenger.libs.ktor.common.respond403Forbidden
import no.nav.tiltakspenger.libs.texas.isSystembruker
import no.nav.tiltakspenger.saksbehandling.infra.route.verifisertAzurePrincipal

private val logger = KotlinLogging.logger {}

private object TommeScopes :
    GenerellSystembrukerroller<GenerellSystembrukerrolle>,
    Set<GenerellSystembrukerrolle> by emptySet() {
    override val value: Set<GenerellSystembrukerrolle> = emptySet()
}

fun ApplicationCall.erVerifisertMaskin(): Boolean = isSystembruker(verifisertAzurePrincipal().claims)

suspend fun ApplicationCall.testaktør(): Saksbehandler? {
    val klientId = verifisertAzurePrincipal().claims["azp"] as? String
    if (klientId.isNullOrBlank()) {
        respond403Forbidden(melding = "Oppslag krever maskintoken med klient-ID.", kode = "ingen_tilgang")
        return null
    }

    val parametere = request.queryParameters
    val testidenter = parametere.getAll("testident") ?: listOf("TEST-SAKSBEHANDLER")
    val testident = testidenter.singleOrNull()
    if (testident == null || testident !in setOf("TEST-SAKSBEHANDLER", "TEST-BESLUTTER")) {
        respond400BadRequest(melding = "Oppgi høyst én gyldig testident.", kode = "ugyldig_testident")
        return null
    }

    val rollenavn = parametere.getAll("rolle") ?: listOf(Saksbehandlerrolle.SAKSBEHANDLER.name)
    if (rollenavn.isEmpty()) {
        respond400BadRequest(melding = "Oppgi en gyldig rolle per rolleparameter.", kode = "ugyldig_rolle")
        return null
    }
    val roller = rollenavn.map { navn ->
        Saksbehandlerrolle.entries.firstOrNull { it.name == navn } ?: run {
            respond400BadRequest(melding = "Oppgi en gyldig rolle per rolleparameter.", kode = "ugyldig_rolle")
            return null
        }
    }

    return Saksbehandler(
        navIdent = testident,
        brukernavn = testident,
        epost = "",
        roller = Saksbehandlerroller(roller),
        klientId = klientId,
        klientnavn = "",
        scopes = TommeScopes,
    )
}

fun Saksbehandler.loggMaskinoppslag(operasjon: String) {
    logger.info { "Vellykket maskinoppslag: operasjon=$operasjon, klientId=$klientId, testident=$navIdent, roller=${roller.map { it.name }}" }
}
