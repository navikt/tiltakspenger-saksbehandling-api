package no.nav.tiltakspenger.saksbehandling.benk.infra.repo

import no.nav.tiltakspenger.saksbehandling.benk.domene.BenkKlageKolonne
import no.nav.tiltakspenger.saksbehandling.benk.domene.BenkMeldekortKolonne
import no.nav.tiltakspenger.saksbehandling.benk.domene.BenkRevurderingerKolonne
import no.nav.tiltakspenger.saksbehandling.benk.domene.BenkSorteringRetning
import no.nav.tiltakspenger.saksbehandling.benk.domene.BenkSøknaderKolonne
import no.nav.tiltakspenger.saksbehandling.benk.domene.BenkTilbakekrevingKolonne

/**
 * Ren mapping fra benkens sorteringsvalg til `order by`-leddet i fanespørringen.
 * Rører ikke postgres.
 *
 * Verdiene er aliasnavn i spørringene i `BenkPostgresRepo`, så en omdøping her må følges av en omdøping der.
 * Mappingen finnes nettopp for at kolonnenavnet aldri skal komme rått fra en request og inn i sql-en.
 */

fun BenkSøknaderKolonne.toDbString(): String = when (this) {
    BenkSøknaderKolonne.FNR -> "fnr"
    BenkSøknaderKolonne.SØKNADSTYPE -> "søknadstype"
    BenkSøknaderKolonne.STATUS -> "status"
    BenkSøknaderKolonne.KRAVTIDSPUNKT -> "kravtidspunkt"
    BenkSøknaderKolonne.RESULTAT -> "resultat"
    BenkSøknaderKolonne.SIST_ENDRET -> "sist_endret"
    BenkSøknaderKolonne.SAKSBEHANDLER -> "saksbehandler"
    BenkSøknaderKolonne.BESLUTTER -> "beslutter"
    BenkSøknaderKolonne.VENTESTATUS_FRIST -> "vente_frist"
}

fun BenkRevurderingerKolonne.toDbString(): String = when (this) {
    BenkRevurderingerKolonne.FNR -> "fnr"
    BenkRevurderingerKolonne.RESULTAT -> "resultat"
    BenkRevurderingerKolonne.STATUS -> "status"
    BenkRevurderingerKolonne.STARTET -> "startet"
    BenkRevurderingerKolonne.SIST_ENDRET -> "sist_endret"
    BenkRevurderingerKolonne.SAKSBEHANDLER -> "saksbehandler"
    BenkRevurderingerKolonne.BESLUTTER -> "beslutter"
    BenkRevurderingerKolonne.VENTESTATUS_FRIST -> "vente_frist"
}

fun BenkMeldekortKolonne.toDbString(): String = when (this) {
    BenkMeldekortKolonne.FNR -> "fnr"

    BenkMeldekortKolonne.TYPE -> "type"

    // Jsonb-arrayen er aggregert kronologisk, og sammenlignes elementvis — sorteringen treffer altså tidligste meldeperiode først.
    BenkMeldekortKolonne.PERIODE -> "meldeperioder"

    BenkMeldekortKolonne.BELØP -> "beløp"

    BenkMeldekortKolonne.STATUS -> "status"

    BenkMeldekortKolonne.SIST_ENDRET -> "sist_endret"

    BenkMeldekortKolonne.SAKSBEHANDLER -> "saksbehandler"

    BenkMeldekortKolonne.BESLUTTER -> "beslutter"

    BenkMeldekortKolonne.VENTESTATUS_FRIST -> "vente_frist"
}

fun BenkKlageKolonne.toDbString(): String = when (this) {
    BenkKlageKolonne.FNR -> "fnr"
    BenkKlageKolonne.RESULTAT -> "resultat"
    BenkKlageKolonne.STATUS -> "status"
    BenkKlageKolonne.KRAVTIDSPUNKT -> "kravtidspunkt"
    BenkKlageKolonne.SIST_ENDRET -> "sist_endret"
    BenkKlageKolonne.SAKSBEHANDLER -> "saksbehandler"
    BenkKlageKolonne.VENTESTATUS_FRIST -> "vente_frist"
}

fun BenkTilbakekrevingKolonne.toDbString(): String = when (this) {
    BenkTilbakekrevingKolonne.FNR -> "fnr"

    BenkTilbakekrevingKolonne.BELØP -> "beløp"

    BenkTilbakekrevingKolonne.KILDE -> "kilde"

    BenkTilbakekrevingKolonne.STATUS -> "status"

    BenkTilbakekrevingKolonne.STARTET -> "startet"

    BenkTilbakekrevingKolonne.SIST_ENDRET -> "sist_endret"

    BenkTilbakekrevingKolonne.SAKSBEHANDLER -> "saksbehandler"

    BenkTilbakekrevingKolonne.BESLUTTER -> "beslutter"

    BenkTilbakekrevingKolonne.VENTESTATUS_FRIST -> "vente_frist"

    // Composit-typen periode sammenlignes feltvis, altså fra_og_med først og deretter til_og_med.
    BenkTilbakekrevingKolonne.KRAVGRUNNLAG_PERIODE -> "kravgrunnlag_periode"
}

fun BenkSorteringRetning.toDbString(): String = when (this) {
    BenkSorteringRetning.ASC -> "ASC"
    BenkSorteringRetning.DESC -> "DESC"
}
