package no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet

import arrow.core.Either
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.httpklient.HttpKlientError
import no.nav.tiltakspenger.libs.httpklient.HttpKlientMetadata

/**
 * Port for navkontor-oppslag (oppfølgingsenhet) - det eneste navkontor-interfacet domenet forholder seg til.
 * Implementeres av [no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.infra.http.SammenligningVeilarboppfolgingKlient], som kaller både gammel og ny navkontor-tjeneste og gjør all logging for oppslaget.
 *
 * [loggkontekst] er en fritekst-string med domenekontekst (sakId, saksnummer, behandlingId, ...) som implementasjonen tar med i loggmeldingene for sporbarhet.
 * Når sammenligningsklienten slettes (og gammel klient med den) flyttes loggingen til [NavkontorService], og parameteren kan fjernes herfra.
 */
interface NavkontorKlient {
    suspend fun hentNavkontor(
        fnr: Fnr,
        loggkontekst: String,
    ): Either<KanIkkeHenteNavkontor, NavkontorMedMetadata>
}

/**
 * Et utvendig kall mot en av navkontor-klientene.
 * Lagres som varchar JSON slik at vi i ettertid kan spore eksakt hva vi sendte og mottok - tilsvarende det vi gjør for andre eksterne klienter (se f.eks. journalføring og utbetaling).
 *
 * - [request] er kroppen vi sendte (alltid satt - vi vet hva vi prøvde å sende selv om kallet feilet).
 * - [response] er rå responsbody fra tjenesten, eller `null` dersom vi aldri fikk svar
 *   (IO-feil, timeout, parsing av token feilet, e.l.).
 * - [httpStatus] er HTTP-statuskoden vi fikk, eller `null` dersom vi aldri fikk svar.
 */
data class Klientkall(
    val request: String,
    val response: String?,
    val httpStatus: Int?,
)

/**
 * Bygger et [Klientkall] (for persistering) fra httpklient sin [HttpKlientMetadata].
 * Gjelder både suksess ([no.nav.tiltakspenger.libs.httpklient.HttpKlientResponse.metadata]) og feil ([HttpKlientError.metadata]).
 */
fun HttpKlientMetadata.tilKlientkall(): Klientkall = Klientkall(
    request = rawRequestString,
    response = rawResponseString,
    httpStatus = statusCode,
)

/**
 * Resultatet av et navkontor-oppslag, inkludert metadata om hvilke(n) klient(er) som ble brukt.
 *
 * For et rent kall mot gammel klient (`VeilarboppfolgingHttpClient`) vil kun [veilarboppfolgingKall] være satt.
 * Når kallet går gjennom sammenligningsklienten vil typisk begge kall være satt, slik at konsumenten kan lagre rådata fra både gammel og ny tjeneste.
 *
 * [brukteKlient] forteller hvilken klient sitt svar [navkontor] faktisk kommer fra.
 */
data class NavkontorMedMetadata(
    val navkontor: Navkontor,
    val brukteKlient: BruktNavkontorKlient,
    val veilarboppfolgingKall: Klientkall? = null,
    val kontorhistorikkKall: Klientkall? = null,
    /**
     * Rå metadata fra httpklient for veilarboppfolging-kallet (headere, antall forsøk, timing, tidsstempler).
     * `null` når svaret ikke stammer fra et httpklient-kall (f.eks. fallback til kontorhistorikk i sammenligningsklienten).
     */
    val httpKlientMetadata: HttpKlientMetadata? = null,
)

enum class BruktNavkontorKlient {
    VEILARBOPPFOLGING,
    KONTORHISTORIKK,
}

/**
 * Mulige feil ved henting av navkontor (oppfølgingsenhet).
 * Konsumenten kan lagre rådata uavhengig av om kallet gikk bra eller ikke:
 * [veilarboppfolgingKall] avledes fra httpklient sin metadata, mens [kontorhistorikkKall] settes av sammenligningsklienten i etterkant (se [medKontorhistorikkKall]) og er derfor konstruktørparameter.
 */
sealed interface KanIkkeHenteNavkontor {
    val veilarboppfolgingKall: Klientkall?
    val kontorhistorikkKall: Klientkall?

    /**
     * Den rå feilen fra httpklient for veilarboppfolging-kallet - bærer feilvariant (timeout/nettverk/auth/...), underliggende throwable og full [HttpKlientMetadata].
     * Brukes av sammenligningsklienten til feillogging med stacktrace.
     * `null` når feilen ikke stammer fra en httpklient-feil ([ManglerOppfolgingsenhet]).
     */
    val httpKlientError: HttpKlientError?

    /**
     * Selve HTTP-kallet feilet (timeout/IO/feil ved token-henting/deserialisering osv.).
     * Se [httpKlientError] for detaljer.
     */
    data class KallFeilet(
        override val httpKlientError: HttpKlientError,
        override val kontorhistorikkKall: Klientkall? = null,
    ) : KanIkkeHenteNavkontor {
        override val veilarboppfolgingKall: Klientkall get() = httpKlientError.metadata.tilKlientkall()
    }

    /** Tjenesten returnerte en HTTP-statuskode forskjellig fra 200. */
    data class UventetHttpStatus(
        override val httpKlientError: HttpKlientError.UventetStatus,
        override val kontorhistorikkKall: Klientkall? = null,
    ) : KanIkkeHenteNavkontor {
        val status: Int get() = httpKlientError.statusCode
        override val veilarboppfolgingKall: Klientkall get() = httpKlientError.metadata.tilKlientkall()
    }

    /** Vi fikk OK-respons, men oppfølgingsenheten manglet i svaret. */
    data class ManglerOppfolgingsenhet(
        /** Metadata fra det (på HTTP-nivå) vellykkede kallet. */
        val httpKlientMetadata: HttpKlientMetadata,
        override val kontorhistorikkKall: Klientkall? = null,
    ) : KanIkkeHenteNavkontor {
        override val veilarboppfolgingKall: Klientkall get() = httpKlientMetadata.tilKlientkall()

        /** HTTP-kallet lyktes - det finnes ingen httpklient-feil, kun [httpKlientMetadata]. */
        override val httpKlientError: HttpKlientError? get() = null
    }
}

/**
 * Nøytral, ikke-sensitiv beskrivelse av feilen for bruk i vanlig logg og exception-meldinger.
 *
 * Feiltypene bærer [Klientkall]/[HttpKlientError] med rå request/response, og en default `toString()` ville derfor lekke persondata (fnr i requesten, stedslokaliserende navkontor i responsen) til vanlig logg.
 * Vi tar kun med feiltypen, HTTP-status og httpklient-varianten (ikke sensitivt) - rådata hører hjemme i sikkerlogg.
 */
internal fun KanIkkeHenteNavkontor.beskrivelse(): String = when (this) {
    is KanIkkeHenteNavkontor.KallFeilet -> "KallFeilet(${httpKlientError::class.simpleName})"
    is KanIkkeHenteNavkontor.UventetHttpStatus -> "UventetHttpStatus(status=$status)"
    is KanIkkeHenteNavkontor.ManglerOppfolgingsenhet -> "ManglerOppfolgingsenhet"
}

/**
 * Hjelpefunksjon som returnerer en kopi av feilen med [KanIkkeHenteNavkontor.kontorhistorikkKall] satt.
 * Brukes av sammenligningsklienten for å berike feilstier med rådata fra ny klient.
 */
internal fun KanIkkeHenteNavkontor.medKontorhistorikkKall(
    kall: Klientkall?,
): KanIkkeHenteNavkontor = when (this) {
    is KanIkkeHenteNavkontor.KallFeilet -> copy(kontorhistorikkKall = kall)
    is KanIkkeHenteNavkontor.UventetHttpStatus -> copy(kontorhistorikkKall = kall)
    is KanIkkeHenteNavkontor.ManglerOppfolgingsenhet -> copy(kontorhistorikkKall = kall)
}
