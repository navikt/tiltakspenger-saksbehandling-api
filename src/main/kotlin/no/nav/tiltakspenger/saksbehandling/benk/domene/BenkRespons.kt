package no.nav.tiltakspenger.saksbehandling.benk.domene

import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.TilgangsvurderingAvvistÅrsak
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.TilgangsvurderingBulk
import no.nav.tiltakspenger.saksbehandling.person.AdressebeskyttelseOgSkjerming

/**
 * Én rad i benken, beriket med det saksbehandleren trenger for å se hva raden gjelder.
 * Tilgangen kommer fra Tilgangsmaskinen; se [BenkPersonmarkører] for hvor markørene kommer fra.
 */
data class BenkRad<T : BenkBehandling>(
    val behandling: T,
    val tilgang: TilgangsvurderingBulk,
    val personmarkører: BenkPersonmarkører,
)

/**
 * Markørene sier om personen raden gjelder har adressebeskyttelse eller er skjermet.
 *
 * For rader uten tilgang utledes de av regelen som avviste tilgangen.
 * Tilgangsmaskinen evaluerer reglene i rekkefølge og rapporterer den første som avviser, så en person som både er skjermet og har strengt fortrolig adresse får bare kode 6.
 * For rader med tilgang er de bare kjent når filteret på adressebeskyttelse og skjerming er valgt, og kommer da fra PDL og skjermingsregisteret.
 */
data class BenkPersonmarkører(
    val skjermet: Boolean,
    val kode6: Boolean,
    val kode7: Boolean,
) {
    companion object {
        private val INGEN = BenkPersonmarkører(skjermet = false, kode6 = false, kode7 = false)

        /**
         * Kode 6 dekker både strengt fortrolig og strengt fortrolig utland; kode 7 er fortrolig adresse.
         * En avvisning fra en regel utenfor kjernesettet gir ingen markører.
         * En godkjent rad får markørene fra [adressebeskyttelseOgSkjerming], eller ingen når den ikke er slått opp.
         */
        fun fra(
            tilgang: TilgangsvurderingBulk,
            adressebeskyttelseOgSkjerming: AdressebeskyttelseOgSkjerming? = null,
        ): BenkPersonmarkører = when (tilgang) {
            is TilgangsvurderingBulk.Avvist -> BenkPersonmarkører(
                skjermet = tilgang.årsak == TilgangsvurderingAvvistÅrsak.SKJERMET,
                kode6 = tilgang.årsak == TilgangsvurderingAvvistÅrsak.STRENGT_FORTROLIG ||
                    tilgang.årsak == TilgangsvurderingAvvistÅrsak.STRENGT_FORTROLIG_UTLAND,
                kode7 = tilgang.årsak == TilgangsvurderingAvvistÅrsak.FORTROLIG,
            )

            TilgangsvurderingBulk.Godkjent -> adressebeskyttelseOgSkjerming?.let {
                BenkPersonmarkører(
                    skjermet = it.skjermet,
                    kode6 = it.erKode6,
                    kode7 = it.erKode7,
                )
            } ?: INGEN
        }
    }
}

/**
 * Tellingene gjelder radene på den returnerte siden, ikke alle radene som matcher filteret.
 * Samme person teller flere ganger dersom personen har flere rader på siden.
 * Markørene telles bare på radene uten tilgang, fordi benken viser dem som en del av antallet uten tilgang.
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
            antallSkjermet = rader.count { it.utenTilgang && it.personmarkører.skjermet },
            antallKode6 = rader.count { it.utenTilgang && it.personmarkører.kode6 },
            antallKode7 = rader.count { it.utenTilgang && it.personmarkører.kode7 },
        )
    }
}

private val BenkRad<*>.utenTilgang: Boolean get() = tilgang is TilgangsvurderingBulk.Avvist

/**
 * Radene i én fane med tilgang og personmarkører, sammen med tellingene benken viser over tabellen.
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
 * Hele svaret på ett benk-kall.
 * [harTilgang] er usann når saksbehandleren ikke har en rolle i [no.nav.tiltakspenger.saksbehandling.infra.route.ROLLER_SOM_KAN_SE_BENK], og da er resten av svaret utelatt.
 */
sealed interface BenkRespons<out T : BenkBehandling> {
    val harTilgang: Boolean
}

/**
 * Svaret til en saksbehandler som kan se benken: fanen det ble spurt om, og antallet i alle fanene.
 * Antallet i alle fanene følger med fordi benken viser det i fanetitlene, og ellers måtte hentet det i et eget kall.
 */
data class BenkResponsMedTilgang<T : BenkBehandling>(
    val antallPerFane: BenkAntallPerFane,
    val oversikt: BenkOversiktMedTilgang<T>,
) : BenkRespons<T> {
    override val harTilgang: Boolean = true
}

/**
 * Svaret på mine-fanen: én oversikt per seksjon, med samme radformat som fanen seksjonen tilhører.
 * Seksjonene som ikke ble spurt om (typefilteret), er utelatt.
 */
data class BenkMineResponsMedTilgang(
    val antallPerFane: BenkAntallPerFane,
    val seksjoner: Map<BenkFane, BenkOversiktMedTilgang<BenkBehandling>>,
) : BenkRespons<BenkBehandling> {
    override val harTilgang: Boolean = true
}

/** Svaret til en bruker uten benkrolle — ingen telling eller rader, så det gjøres ingen databaseoppslag. */
data object BenkResponsUtenTilgang : BenkRespons<Nothing> {
    override val harTilgang: Boolean = false
}
