package no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet

import arrow.core.Either
import no.nav.tiltakspenger.libs.common.Fnr

/**
 * Klient for det nye navkontor-API'et basert på GraphQL-spørringen
 * `kontorHistorikk(ident: String!): [KontorHistorikkQueryDto!]!`.
 *
 * Brukes i første iterasjon kun for sammenligning/logging mot eksisterende [VeilarboppfolgingKlient]
 * i dev/local/test. Implementasjoner skal returnere alle innslag (uten filtrering) - filtrering og valg
 * av "riktig" kontor er domenelogikk og hører hjemme på [Kontorhistorikk].
 *
 * Klienten har ansvar for å logge tilstrekkelig (både vanlig logg og sikkerlogg). Konsumenter kan derfor
 * forholde seg til returverdien uten å måtte logge feil selv. De nullable metadatafeltene
 * ([sakId], [saksnummer], [rammebehandlingId], [meldekortbehandlingId]) blir inkludert i log-meldingene
 * for å gi sporbarhet tilbake til det konsumenten holder på med.
 */
interface KontorhistorikkKlient {
    suspend fun hentKontorhistorikk(
        fnr: Fnr,
        sakId: String? = null,
        saksnummer: String? = null,
        rammebehandlingId: String? = null,
        meldekortbehandlingId: String? = null,
    ): Either<KanIkkeHenteKontorhistorikk, KontorhistorikkMedMetadata>
}

/**
 * Kontorhistorikk pakket sammen med rå request/response slik at konsumenten kan lagre rådata
 * tilsvarende det vi gjør for andre eksterne klienter.
 */
data class KontorhistorikkMedMetadata(
    val kontorhistorikk: Kontorhistorikk,
    val kall: Klientkall,
)

/**
 * Mulige feil ved henting av kontorhistorikk. Vi skiller på typer slik at konsumenter kan reagere ulikt
 * (f.eks. på timeout vs. en gjennomgående tjenestefeil) hvis det blir aktuelt senere.
 *
 * [kall] er rå request/response slik vi sendte og mottok. På noen feilstier (f.eks. når vi aldri
 * klarte å sende kallet) kan [kall] være `null`, eller [Klientkall.response] / [Klientkall.httpStatus]
 * være `null`.
 */
sealed interface KanIkkeHenteKontorhistorikk {
    val kall: Klientkall?

    /** Selve HTTP-kallet kastet en exception (timeout/IO/feil ved token-henting/parsing osv.). */
    data class KallFeilet(override val kall: Klientkall? = null) : KanIkkeHenteKontorhistorikk

    /** Tjenesten returnerte en HTTP-statuskode forskjellig fra 200. */
    data class UventetHttpStatus(val status: Int, override val kall: Klientkall) : KanIkkeHenteKontorhistorikk

    /** Responsen inneholdt et `errors`-felt fra GraphQL-tjenesten. */
    data class GraphQlFeil(override val kall: Klientkall) : KanIkkeHenteKontorhistorikk

    /** Responsen inneholdt minst ett innslag som gjaldt en annen ident enn det vi spurte om. */
    data class IdentMismatch(override val kall: Klientkall) : KanIkkeHenteKontorhistorikk
}
