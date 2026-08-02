package no.nav.tiltakspenger.saksbehandling.benk.infra.repo

import no.nav.tiltakspenger.saksbehandling.benk.domene.BenkSorteringKolonne
import no.nav.tiltakspenger.saksbehandling.benk.domene.SorteringRetning

/**
 * Ren mapping fra sorteringsvalget på benken til `order by`-leddet i benkspørringen.
 * Rører ikke postgres.
 * Verdiene er kolonne- og aliasnavn i spørringen i `BenkOversiktPostgresRepo`, så en omdøping her må følges av en omdøping der.
 */

fun BenkSorteringKolonne.toDbString(): String =
    when (this) {
        BenkSorteringKolonne.STARTET -> "startet"
        BenkSorteringKolonne.SIST_ENDRET -> "sist_endret"
        BenkSorteringKolonne.FRIST -> "sattPåVentFrist"
        BenkSorteringKolonne.FNR -> "fnr"
        BenkSorteringKolonne.BEHANDLINGSTYPE -> "behandlingstype"
        BenkSorteringKolonne.STATUS -> "status"
        BenkSorteringKolonne.SAKSBEHANDLER -> "saksbehandler"
        BenkSorteringKolonne.BESLUTTER -> "beslutter"
        BenkSorteringKolonne.BELØP -> "beløp"
    }

fun SorteringRetning.toDbString(): String =
    when (this) {
        SorteringRetning.ASC -> "ASC"
        SorteringRetning.DESC -> "DESC"
    }
