package no.nav.tiltakspenger.saksbehandling.benk.domene

import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.TilgangsvurderingAvvistÅrsak
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.TilgangsvurderingBulk

/**
 * Én rad i benken, beriket med det saksbehandleren trenger for å se hva raden gjelder.
 * Både tilgangen og personmarkørene kommer fra Tilgangsmaskinen.
 */
data class BenkRad<T : BenkBehandling>(
    val behandling: T,
    val tilgang: TilgangsvurderingBulk,
    val personmarkører: BenkPersonmarkører,
)

/**
 * Markørene sier hvorfor saksbehandleren ikke får se raden.
 *
 * De utledes av regelen som avviste tilgangen, og er derfor bare kjent for rader saksbehandleren ikke har tilgang til.
 * Benken slår ikke opp PDL eller skjermingsregisteret, fordi det ville gitt unødvendig last for hver sidevisning (avklart 2026-09-16).
 * Tilgangsmaskinen evaluerer reglene i rekkefølge og rapporterer den første som avviser, så en person som både er skjermet og har strengt fortrolig adresse får bare kode 6.
 */
data class BenkPersonmarkører(
    val skjermet: Boolean,
    val kode6: Boolean,
    val kode7: Boolean,
) {
    companion object {
        /**
         * Kode 6 dekker både strengt fortrolig og strengt fortrolig utland; kode 7 er fortrolig adresse.
         * Alle markørene er false når tilgangen er godkjent, og når avvisningen kom fra en regel utenfor kjernesettet.
         */
        fun fra(tilgang: TilgangsvurderingBulk): BenkPersonmarkører {
            val årsak = when (tilgang) {
                TilgangsvurderingBulk.Godkjent -> null
                is TilgangsvurderingBulk.Avvist -> tilgang.årsak
            }
            return BenkPersonmarkører(
                skjermet = årsak == TilgangsvurderingAvvistÅrsak.SKJERMET,
                kode6 = årsak == TilgangsvurderingAvvistÅrsak.STRENGT_FORTROLIG ||
                    årsak == TilgangsvurderingAvvistÅrsak.STRENGT_FORTROLIG_UTLAND,
                kode7 = årsak == TilgangsvurderingAvvistÅrsak.FORTROLIG,
            )
        }
    }
}

/**
 * Tellingene gjelder radene på den returnerte siden, ikke alle radene som matcher filteret.
 * Samme person teller flere ganger dersom personen har flere rader på siden.
 */
data class BenkOppsummering(
    val antallMedTilgang: Int,
    val antallUtenTilgang: Int,
    val antallSkjermet: Int,
    val antallKode6: Int,
    val antallKode7: Int,
) {
    companion object {
        fun <T : BenkBehandling> fra(rader: List<BenkRad<T>>): BenkOppsummering = BenkOppsummering(
            antallMedTilgang = rader.count { it.tilgang is TilgangsvurderingBulk.Godkjent },
            antallUtenTilgang = rader.count { it.tilgang is TilgangsvurderingBulk.Avvist },
            antallSkjermet = rader.count { it.personmarkører.skjermet },
            antallKode6 = rader.count { it.personmarkører.kode6 },
            antallKode7 = rader.count { it.personmarkører.kode7 },
        )
    }
}

/**
 * Radene i én fane med tilgang og personmarkører, sammen med tellingene benken viser over tabellen.
 * Tilgangen kommer fra Tilgangsmaskinen, og personmarkørene fra regelen som eventuelt avviste den.
 */
data class BenkOversiktMedTilgang<T : BenkBehandling>(
    val rader: List<BenkRad<T>>,
    val totalAntall: Int,
    val totalAntallUfiltrert: Int,
    val oppsummering: BenkOppsummering,
    val saksbehandlere: List<String>,
    val besluttere: List<String>,
    /** Siden som ble spurt om, 0-basert. */
    val side: Int,
) {
    val sideantall = BenkPaginering.SIDEANTALL
}

/**
 * Hele svaret på ett benk-kall: fanen det ble spurt om, og antallet i alle fanene.
 * Antallet i alle fanene følger med fordi benken viser det i fanetitlene, og ellers måtte hentet det i et eget kall.
 */
data class BenkRespons<T : BenkBehandling>(
    val antallPerFane: BenkAntallPerFane,
    val oversikt: BenkOversiktMedTilgang<T>,
)
