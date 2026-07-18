package no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet

import no.nav.tiltakspenger.libs.httpklient.HttpKlientError
import no.nav.tiltakspenger.libs.httpklient.HttpKlientMetadata
import no.nav.tiltakspenger.libs.httpklient.throwableOrNull

/**
 * Kontorhistorikk pakket sammen med rå request/response slik at konsumenten kan lagre rådata tilsvarende det vi gjør for andre eksterne klienter.
 */
data class KontorhistorikkMedMetadata(
    val kontorhistorikk: Kontorhistorikk,
    val kall: Klientkall,
    /** Rå metadata fra httpklient (headere, antall forsøk, timing, tidsstempler). */
    val httpKlientMetadata: HttpKlientMetadata,
)

/**
 * Mulige feil ved henting av kontorhistorikk.
 * Vi skiller på typer slik at konsumenter kan reagere ulikt (f.eks. på timeout vs. en gjennomgående tjenestefeil) hvis det blir aktuelt senere.
 *
 * [kall] er rå request/response slik vi sendte og mottok, avledet fra httpklient sin metadata.
 * Kun [UventetFeil] mangler det; på andre feilstier kan [Klientkall.response] / [Klientkall.httpStatus] være `null` (f.eks. når vi aldri fikk svar).
 */
sealed interface KanIkkeHenteKontorhistorikk {
    val kall: Klientkall?

    /**
     * Den rå feilen fra httpklient - bærer feilvariant (timeout/nettverk/auth/...), underliggende throwable og full [HttpKlientMetadata].
     * Brukes av sammenligningsklienten til feillogging med stacktrace.
     * `null` når feilen ikke stammer fra en httpklient-feil ([GraphQlFeil], eller feil konstruert utenfor klienten).
     */
    val httpKlientError: HttpKlientError?

    /**
     * Selve HTTP-kallet feilet (timeout/IO/feil ved token-henting/deserialisering osv.).
     * Se [httpKlientError] for detaljer.
     */
    data class KallFeilet(
        override val httpKlientError: HttpKlientError,
    ) : KanIkkeHenteKontorhistorikk {
        override val kall: Klientkall get() = httpKlientError.metadata.tilKlientkall()
    }

    /** Tjenesten returnerte en HTTP-statuskode forskjellig fra 200. */
    data class UventetHttpStatus(
        override val httpKlientError: HttpKlientError.UventetStatus,
    ) : KanIkkeHenteKontorhistorikk {
        val status: Int get() = httpKlientError.statusCode
        override val kall: Klientkall get() = httpKlientError.metadata.tilKlientkall()
    }

    /** Responsen inneholdt et `errors`-felt fra GraphQL-tjenesten (selve HTTP-kallet lyktes). */
    data class GraphQlFeil(
        /** Metadata fra det (på HTTP-nivå) vellykkede kallet. */
        val httpKlientMetadata: HttpKlientMetadata,
    ) : KanIkkeHenteKontorhistorikk {
        override val kall: Klientkall get() = httpKlientMetadata.tilKlientkall()

        /** HTTP-kallet lyktes - det finnes ingen httpklient-feil, kun [httpKlientMetadata]. */
        override val httpKlientError: HttpKlientError? get() = null
    }

    /**
     * Klienten brøt kontrakten og kastet en exception i stedet for å returnere Either.
     * Skal ikke skje; brukes kun av sammenligningsklientens exception-vern, som logger stacktracen til sikkerlogg.
     * Vi har verken rått kall eller httpklient-feil på denne stien.
     */
    data class UventetFeil(
        val throwable: Throwable,
    ) : KanIkkeHenteKontorhistorikk {
        override val kall: Klientkall? get() = null
        override val httpKlientError: HttpKlientError? get() = null
    }
}

/**
 * Nøytral, ikke-sensitiv beskrivelse av feilen for bruk i vanlig logg og exception-meldinger.
 * Se [KanIkkeHenteNavkontor.beskrivelse] for begrunnelsen.
 */
internal fun KanIkkeHenteKontorhistorikk.beskrivelse(): String = when (this) {
    is KanIkkeHenteKontorhistorikk.KallFeilet -> "KallFeilet(${httpKlientError::class.simpleName})"
    is KanIkkeHenteKontorhistorikk.UventetHttpStatus -> "UventetHttpStatus(status=$status)"
    is KanIkkeHenteKontorhistorikk.GraphQlFeil -> "GraphQlFeil"
    is KanIkkeHenteKontorhistorikk.UventetFeil -> "UventetFeil(${throwable::class.simpleName})"
}

/**
 * Throwable for feillogging med stacktrace (kun sikkerlogg - kan inneholde persondata):
 * httpklient-feilens underliggende throwable, eller den ukontrollerte exceptionen for [KanIkkeHenteKontorhistorikk.UventetFeil].
 */
internal fun KanIkkeHenteKontorhistorikk.throwableForLogg(): Throwable? = when (this) {
    is KanIkkeHenteKontorhistorikk.UventetFeil -> throwable
    is KanIkkeHenteKontorhistorikk.KallFeilet -> httpKlientError.throwableOrNull()
    is KanIkkeHenteKontorhistorikk.UventetHttpStatus -> httpKlientError.throwableOrNull()
    is KanIkkeHenteKontorhistorikk.GraphQlFeil -> null
}
