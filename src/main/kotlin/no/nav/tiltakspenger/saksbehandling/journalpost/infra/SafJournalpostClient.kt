package no.nav.tiltakspenger.saksbehandling.journalpost.infra

import arrow.core.Either
import no.nav.tiltakspenger.libs.httpklient.HttpKlientError
import no.nav.tiltakspenger.libs.httpklient.HttpKlientMetadata
import no.nav.tiltakspenger.saksbehandling.dokument.PdfA
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId
import no.nav.tiltakspenger.saksbehandling.journalpost.HentDokumentCommand

interface SafJournalpostClient {
    /**
     * `Right(null)` betyr at journalposten ikke finnes (SAF svarer `not_found`/`bad_request` i GraphQL-errors-lista, eller uten journalpost-data).
     */
    suspend fun hentJournalpost(journalpostId: JournalpostId): Either<KanIkkeHenteJournalpost, Journalpost?>

    suspend fun hentDokument(
        command: HentDokumentCommand,
    ): Either<HttpKlientError, PdfA>
}

/**
 * Mulige feil ved henting av journalpost fra SAF.
 */
sealed interface KanIkkeHenteJournalpost {
    /**
     * Selve HTTP-kallet feilet (timeout/IO/token-feil/uventet status/deserialisering).
     * Se [httpKlientError] for detaljer.
     */
    data class KallFeilet(
        val httpKlientError: HttpKlientError,
    ) : KanIkkeHenteJournalpost

    /**
     * SAF svarte 200 OK, men med feil i GraphQL-errors-lista som ikke betyr «finnes ikke» (typisk `forbidden` eller `server_error`).
     * Se [feilkoder-dokumentasjonen](https://confluence.adeo.no/spaces/BOA/pages/309563246/saf+-+Utviklerveiledning#safUtviklerveiledning-Feilh%C3%A5ndtering).
     */
    data class GraphQLFeil(
        val feilkoder: List<String>,
        /** Metadata fra det (på HTTP-nivå) vellykkede kallet, slik at rå respons kan logges til sikkerlogg. */
        val httpKlientMetadata: HttpKlientMetadata,
    ) : KanIkkeHenteJournalpost
}

/**
 * Nøytral beskrivelse av feilen for bruk i vanlig logg og exception-meldinger.
 * Feiltypene bærer rå request/response via [HttpKlientError]/[HttpKlientMetadata], og en default `toString()` ville derfor dratt rådata inn i vanlig logg.
 */
fun KanIkkeHenteJournalpost.beskrivelse(): String = when (this) {
    is KanIkkeHenteJournalpost.KallFeilet -> "KallFeilet(${httpKlientError::class.simpleName})"
    is KanIkkeHenteJournalpost.GraphQLFeil -> "GraphQLFeil(feilkoder=$feilkoder)"
}
