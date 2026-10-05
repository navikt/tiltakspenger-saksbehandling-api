package no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet

import no.nav.tiltakspenger.libs.httpklient.HttpKlientError
import no.nav.tiltakspenger.libs.httpklient.HttpKlientMetadata

/**
 * Kontorhistorikk pakket sammen med httpklient sin metadata for kallet.
 * [httpKlientMetadata] bærer rå request/response, headere, antall forsøk og timing, slik at vi kan logge eller lagre rådata ved behov.
 */
data class KontorhistorikkMedMetadata(
    val kontorhistorikk: Kontorhistorikk,
    val httpKlientMetadata: HttpKlientMetadata,
)

/**
 * Mulige feil ved henting av kontorhistorikk.
 * Vi skiller på typer slik at konsumenter kan reagere ulikt (f.eks. på timeout vs. en gjennomgående tjenestefeil) hvis det blir aktuelt senere.
 *
 * [httpKlientMetadata] er rå request/response slik vi sendte og mottok.
 * [HttpKlientMetadata.rawResponseString] og [HttpKlientMetadata.statusCode] kan være `null` når vi aldri fikk svar.
 */
sealed interface KanIkkeHenteKontorhistorikk {
    val httpKlientMetadata: HttpKlientMetadata

    /**
     * Selve HTTP-kallet feilet (timeout/IO/feil ved token-henting/deserialisering osv.).
     * [httpKlientError] bærer feilvariant, underliggende throwable og full metadata.
     */
    data class KallFeilet(
        val httpKlientError: HttpKlientError,
    ) : KanIkkeHenteKontorhistorikk {
        override val httpKlientMetadata: HttpKlientMetadata get() = httpKlientError.metadata
    }

    /** Tjenesten returnerte en HTTP-statuskode forskjellig fra 200. */
    data class UventetHttpStatus(
        val httpKlientError: HttpKlientError.UventetStatus,
    ) : KanIkkeHenteKontorhistorikk {
        val status: Int get() = httpKlientError.statusCode
        override val httpKlientMetadata: HttpKlientMetadata get() = httpKlientError.metadata
    }

    /** Responsen inneholdt et `errors`-felt fra GraphQL-tjenesten (selve HTTP-kallet lyktes). */
    data class GraphQlFeil(
        override val httpKlientMetadata: HttpKlientMetadata,
    ) : KanIkkeHenteKontorhistorikk
}

/**
 * Nøytral, ikke-sensitiv beskrivelse av feilen for bruk i vanlig logg og exception-meldinger.
 *
 * Feiltypene bærer [HttpKlientMetadata] med rå request/response, og en default `toString()` ville derfor lekke persondata (fnr i requesten, stedslokaliserende navkontor i responsen) til vanlig logg.
 * Vi tar kun med feiltypen, HTTP-status og httpklient-varianten (ikke sensitivt) - rådata hører hjemme i sikkerlogg.
 */
fun KanIkkeHenteKontorhistorikk.beskrivelse(): String = when (this) {
    is KanIkkeHenteKontorhistorikk.KallFeilet -> "KallFeilet(${httpKlientError::class.simpleName})"
    is KanIkkeHenteKontorhistorikk.UventetHttpStatus -> "UventetHttpStatus(status=$status)"
    is KanIkkeHenteKontorhistorikk.GraphQlFeil -> "GraphQlFeil"
}
