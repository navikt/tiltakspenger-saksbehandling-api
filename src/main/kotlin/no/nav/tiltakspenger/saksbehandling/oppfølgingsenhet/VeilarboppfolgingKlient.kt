package no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet

import arrow.core.Either
import no.nav.tiltakspenger.libs.common.Fnr

interface VeilarboppfolgingKlient {
    suspend fun hentOppfolgingsenhet(
        fnr: Fnr,
        sakId: String? = null,
        saksnummer: String? = null,
        rammebehandlingId: String? = null,
        meldekortbehandlingId: String? = null,
    ): Either<KanIkkeHenteOppfølgingsenhet, NavkontorMedMetadata>
}

/**
 * Et utvendig kall mot en av navkontor-klientene. Lagres som varchar JSON slik at vi i ettertid
 * kan spore eksakt hva vi sendte og mottok - tilsvarende det vi gjør for andre eksterne klienter
 * (se f.eks. journalføring og utbetaling).
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
 * Resultatet av et navkontor-oppslag, inkludert metadata om hvilke(n) klient(er) som ble brukt.
 *
 * For et rent kall mot [VeilarboppfolgingKlient]-implementasjonen vil kun [veilarboppfolgingKall]
 * være satt. Når kallet går gjennom [SammenligningVeilarboppfolgingKlient] vil typisk begge kall
 * være satt, slik at konsumenten kan lagre rådata fra både gammel og ny tjeneste.
 *
 * [brukteKlient] forteller hvilken klient sitt svar [navkontor] faktisk kommer fra.
 */
data class NavkontorMedMetadata(
    val navkontor: Navkontor,
    val brukteKlient: BruktNavkontorKlient,
    val veilarboppfolgingKall: Klientkall? = null,
    val kontorhistorikkKall: Klientkall? = null,
)

enum class BruktNavkontorKlient {
    VEILARBOPPFOLGING,
    KONTORHISTORIKK,
}

/**
 * Mulige feil ved henting av navkontor (oppfølgingsenhet).
 * Vi tar med [veilarboppfolgingKall] og [kontorhistorikkKall] også på feilstier slik at konsumenten
 * kan lagre rådata uavhengig av om kallet gikk bra eller ikke.
 */
sealed interface KanIkkeHenteOppfølgingsenhet {
    val veilarboppfolgingKall: Klientkall?
    val kontorhistorikkKall: Klientkall?

    /** Selve HTTP-kallet kastet en exception (timeout/IO/feil ved token-henting/parsing osv.). */
    data class KallFeilet(
        override val veilarboppfolgingKall: Klientkall? = null,
        override val kontorhistorikkKall: Klientkall? = null,
    ) : KanIkkeHenteOppfølgingsenhet

    /** Tjenesten returnerte en HTTP-statuskode forskjellig fra 200. */
    data class UventetHttpStatus(
        val status: Int,
        override val veilarboppfolgingKall: Klientkall? = null,
        override val kontorhistorikkKall: Klientkall? = null,
    ) : KanIkkeHenteOppfølgingsenhet

    /** Vi fikk OK-respons, men oppfølgingsenheten manglet i svaret. */
    data class ManglerOppfolgingsenhet(
        override val veilarboppfolgingKall: Klientkall? = null,
        override val kontorhistorikkKall: Klientkall? = null,
    ) : KanIkkeHenteOppfølgingsenhet
}

/**
 * Hjelpefunksjon som returnerer en kopi av feilen med [kontorhistorikkKall] satt. Brukes av
 * [SammenligningVeilarboppfolgingKlient] for å berike feilstier med rådata fra ny klient.
 */
internal fun KanIkkeHenteOppfølgingsenhet.medKontorhistorikkKall(
    kall: Klientkall?,
): KanIkkeHenteOppfølgingsenhet = when (this) {
    is KanIkkeHenteOppfølgingsenhet.KallFeilet -> copy(kontorhistorikkKall = kall)
    is KanIkkeHenteOppfølgingsenhet.UventetHttpStatus -> copy(kontorhistorikkKall = kall)
    is KanIkkeHenteOppfølgingsenhet.ManglerOppfolgingsenhet -> copy(kontorhistorikkKall = kall)
}
