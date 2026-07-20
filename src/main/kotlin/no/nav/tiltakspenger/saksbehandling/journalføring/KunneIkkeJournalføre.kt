package no.nav.tiltakspenger.saksbehandling.journalføring

import io.github.oshai.kotlinlogging.KLogger
import no.nav.tiltakspenger.libs.httpklient.HttpKlientError
import no.nav.tiltakspenger.libs.httpklient.HttpKlientMetadata
import no.nav.tiltakspenger.libs.httpklient.loggFeil
import no.nav.tiltakspenger.libs.logging.Sikkerlogg

/**
 * Mulige feil ved journalføring av brev mot dokarkiv.
 */
sealed interface KunneIkkeJournalføre {
    /**
     * Byggingen av dokarkiv-requesten (domene → JSON) kastet før noe HTTP-kall ble gjort.
     * Typisk en manglende påkrevd verdi i domenet (f.eks. brevtekst) eller en serialiseringsfeil.
     */
    data class KunneIkkeByggeRequest(
        val throwable: Throwable,
    ) : KunneIkkeJournalføre

    /**
     * Selve HTTP-kallet feilet (timeout/IO/token-feil/uventet status/deserialisering).
     * Se [feil] for detaljer.
     */
    data class KallFeilet(
        val feil: HttpKlientError,
    ) : KunneIkkeJournalføre

    /**
     * Dokarkiv svarte 201/409, men uten journalpostId i responsen.
     * Da tør vi ikke anta at noe ble journalført, og behandler det som feil.
     */
    data class UgyldigRespons(
        val begrunnelse: String,
        /** Metadata fra det (på HTTP-nivå) vellykkede kallet, slik at rå respons kan logges til sikkerlogg. */
        val metadata: HttpKlientMetadata,
    ) : KunneIkkeJournalføre
}

/**
 * Nøytral beskrivelse av feilen for bruk i vanlig logg og exception-meldinger.
 * Feiltypene bærer rå request/response via [HttpKlientError]/[HttpKlientMetadata], og en default `toString()` ville derfor dratt rådata inn i vanlig logg.
 */
fun KunneIkkeJournalføre.beskrivelse(): String = when (this) {
    is KunneIkkeJournalføre.KunneIkkeByggeRequest -> "KunneIkkeByggeRequest(${throwable::class.simpleName})"
    is KunneIkkeJournalføre.KallFeilet -> "KallFeilet(${feil::class.simpleName})"
    is KunneIkkeJournalføre.UgyldigRespons -> "UgyldigRespons($begrunnelse)"
}

/**
 * Én logghendelse per feilsituasjon: vanlig logg uten rådata + sikkerlogg med rå request/respons.
 * Delt fordi alle journalførings-jobbene logger den samme feiltypen.
 */
fun KunneIkkeJournalføre.loggFeil(logger: KLogger, operasjon: String, kontekst: String) {
    when (this) {
        is KunneIkkeJournalføre.KunneIkkeByggeRequest -> {
            logger.error(throwable) { "Feil ved $operasjon: kunne ikke bygge dokarkiv-requesten. $kontekst" }
            Sikkerlogg.error(throwable) { "Feil ved $operasjon: kunne ikke bygge dokarkiv-requesten. $kontekst" }
        }

        is KunneIkkeJournalføre.KallFeilet -> feil.loggFeil(logger, operasjon, kontekst)

        is KunneIkkeJournalføre.UgyldigRespons -> {
            logger.error { "Feil ved $operasjon: $begrunnelse. $kontekst. Se sikkerlogg for detaljer." }
            Sikkerlogg.error { "Feil ved $operasjon: $begrunnelse. $kontekst. response: ${metadata.rawResponseString}. request: ${metadata.rawRequestString}" }
        }
    }
}
