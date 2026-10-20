package no.nav.tiltakspenger.saksbehandling.utbetaling.domene.utbetalingsoversikt

import no.nav.tiltakspenger.saksbehandling.ytelser.domene.Ytelsetype

/**
 * Ytelsestypen på en ytelse fra økonomisystemet, tolket mot typene vi kjenner.
 * [Ytelsetype] er typene saksbehandlingen henter som andre ytelser til vurderingen etter tiltakspengeforskriften § 7, men lista er ikke en fasit for § 7.
 */
sealed interface MottattYtelsestype {
    /** En type i [Ytelsetype]. */
    data class Relevant(val ytelsetype: Ytelsetype) : MottattYtelsestype {
        init {
            require(ytelsetype != Ytelsetype.UKJENT) { "En ukjent ytelsestype er ikke relevant" }
        }
    }

    /** En kjent type saksbehandlingen ikke henter, med teksten slik økonomisystemet sendte den. */
    data class Utelatt(val ytelsestype: String) : MottattYtelsestype

    /** En type som verken står i [Ytelsetype] eller blant de utelatte, med teksten slik økonomisystemet sendte den. */
    data class Ukjent(val ytelsestype: String) : MottattYtelsestype
}
