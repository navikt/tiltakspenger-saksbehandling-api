package no.nav.tiltakspenger.saksbehandling.benk.v2.infra.repo

import no.nav.tiltakspenger.saksbehandling.benk.v2.domene.BenkKlageKolonne
import no.nav.tiltakspenger.saksbehandling.benk.v2.domene.BenkMeldekortKolonne
import no.nav.tiltakspenger.saksbehandling.benk.v2.domene.BenkRevurderingerKolonne
import no.nav.tiltakspenger.saksbehandling.benk.v2.domene.BenkSøknaderKolonne
import no.nav.tiltakspenger.saksbehandling.benk.v2.domene.BenkTilbakekrevingKolonne
import no.nav.tiltakspenger.saksbehandling.benk.v2.domene.BenkV2SorteringRetning

/**
 * Ren mapping fra benkens sorteringsvalg til `order by`-leddet i fanespørringen.
 * Rører ikke postgres.
 *
 * Verdiene er aliasnavn i spørringene i `BenkV2PostgresRepo`, så en omdøping her må følges av en omdøping der.
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
    BenkMeldekortKolonne.PERIODE -> "periode_fra_og_med"
    BenkMeldekortKolonne.BELØP -> "beløp"
    BenkMeldekortKolonne.STATUS -> "status"
    BenkMeldekortKolonne.MOTTATT -> "mottatt_tidspunkt"
    BenkMeldekortKolonne.SAKSBEHANDLER -> "saksbehandler"
    BenkMeldekortKolonne.VENTESTATUS_FRIST -> "vente_frist"
}

fun BenkKlageKolonne.toDbString(): String = when (this) {
    BenkKlageKolonne.FNR -> "fnr"
    BenkKlageKolonne.RESULTAT -> "resultat"
    BenkKlageKolonne.STATUS -> "status"
    BenkKlageKolonne.KRAVTIDSPUNKT -> "kravtidspunkt"
    BenkKlageKolonne.SIST_ENDRET -> "sist_endret"
    BenkKlageKolonne.SAKSBEHANDLER -> "saksbehandler"
    BenkKlageKolonne.BESLUTTER -> "beslutter"
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
    BenkTilbakekrevingKolonne.VENTESTATUS_FRIST -> "vente_frist"
}

fun BenkV2SorteringRetning.toDbString(): String = when (this) {
    BenkV2SorteringRetning.ASC -> "ASC"
    BenkV2SorteringRetning.DESC -> "DESC"
}
