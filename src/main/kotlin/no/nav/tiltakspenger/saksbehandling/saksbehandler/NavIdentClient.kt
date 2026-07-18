package no.nav.tiltakspenger.saksbehandling.saksbehandler

import arrow.core.Either
import arrow.core.getOrElse
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.httpklient.HttpKlientError
import no.nav.tiltakspenger.libs.httpklient.HttpKlientMetadata
import no.nav.tiltakspenger.libs.logging.Sikkerlogg
import no.nav.tiltakspenger.saksbehandling.infra.http.loggFeil

/**
 * Port for oppslag av navn på en ansatt (saksbehandler/beslutter) fra navIdent.
 * Implementeres av [no.nav.tiltakspenger.saksbehandling.saksbehandler.infra.MicrosoftGraphApiClient].
 */
interface NavIdentClient {
    suspend fun hentNavnForNavIdent(navIdent: String): Either<KanIkkeHenteNavnForNavIdent, String>
}

/** Mulige feil ved henting av navn for en navIdent fra Microsoft Graph. */
sealed interface KanIkkeHenteNavnForNavIdent {
    /**
     * Selve HTTP-kallet feilet (timeout/IO/feil ved token-henting/uventet status/deserialisering osv.).
     * Se [httpKlientError] for detaljer.
     */
    data class KallFeilet(
        val httpKlientError: HttpKlientError,
    ) : KanIkkeHenteNavnForNavIdent

    /** Kallet lyktes, men søket ga ikke nøyaktig én bruker for navIdenten. */
    data class FantIkkeEntydigBruker(
        val antallTreff: Int,
        /** Metadata fra det (på HTTP-nivå) vellykkede kallet, slik at rå respons kan logges til sikkerlogg. */
        val httpKlientMetadata: HttpKlientMetadata,
    ) : KanIkkeHenteNavnForNavIdent

    /** Kallet lyktes og brukeren ble funnet, men navnet (givenName + surname) var blankt. */
    data class NavnetErBlankt(
        /** Metadata fra det (på HTTP-nivå) vellykkede kallet, slik at rå respons kan logges til sikkerlogg. */
        val httpKlientMetadata: HttpKlientMetadata,
    ) : KanIkkeHenteNavnForNavIdent
}

/**
 * Nøytral beskrivelse av feilen for bruk i vanlig logg og exception-meldinger.
 * Feiltypene bærer rå request/response via [HttpKlientError]/[HttpKlientMetadata], og en default `toString()` ville derfor dratt rådata inn i vanlig logg.
 */
internal fun KanIkkeHenteNavnForNavIdent.beskrivelse(): String = when (this) {
    is KanIkkeHenteNavnForNavIdent.KallFeilet -> "KallFeilet(${httpKlientError::class.simpleName})"
    is KanIkkeHenteNavnForNavIdent.FantIkkeEntydigBruker -> "FantIkkeEntydigBruker(antallTreff=$antallTreff)"
    is KanIkkeHenteNavnForNavIdent.NavnetErBlankt -> "NavnetErBlankt"
}

private val logger = KotlinLogging.logger {}

/**
 * Midlertidig bro for brevgenereringskoden, som fortsatt har en throw-basert kontrakt (`suspend (String) -> String` inn i brev-DTO-byggerne).
 * Logger feilen (én logghendelse: vanlig logg + sikkerlogg) og kaster med en nøytral melding.
 * Fjernes når brevgenereringen migreres til `Either` som del av PdfgenHttpClient-migreringen (#1661).
 */
suspend fun NavIdentClient.hentNavnForNavIdentEllerKast(navIdent: String): String {
    return hentNavnForNavIdent(navIdent).getOrElse { feil ->
        feil.logg(navIdent)
        throw IllegalStateException("Kunne ikke hente navn for navIdent $navIdent: ${feil.beskrivelse()}")
    }
}

private fun KanIkkeHenteNavnForNavIdent.logg(navIdent: String) {
    when (this) {
        is KanIkkeHenteNavnForNavIdent.KallFeilet -> httpKlientError.loggFeil(
            logger = logger,
            operasjon = "henting av navn fra Microsoft Graph",
            kontekst = "navIdent: $navIdent",
        )

        is KanIkkeHenteNavnForNavIdent.FantIkkeEntydigBruker -> {
            logger.error { "Fant ikke entydig bruker i Microsoft Graph for navIdent $navIdent: $antallTreff treff. Se sikkerlogg for detaljer." }
            Sikkerlogg.error { "Fant ikke entydig bruker i Microsoft Graph for navIdent $navIdent: $antallTreff treff. response: ${httpKlientMetadata.rawResponseString}" }
        }

        is KanIkkeHenteNavnForNavIdent.NavnetErBlankt -> {
            logger.error { "Navnet fra Microsoft Graph for navIdent $navIdent er blankt. Se sikkerlogg for detaljer." }
            Sikkerlogg.error { "Navnet fra Microsoft Graph for navIdent $navIdent er blankt. response: ${httpKlientMetadata.rawResponseString}" }
        }
    }
}
