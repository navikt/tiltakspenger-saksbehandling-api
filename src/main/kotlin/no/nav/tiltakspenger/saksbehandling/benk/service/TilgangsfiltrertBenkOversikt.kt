package no.nav.tiltakspenger.saksbehandling.benk.service

import no.nav.tiltakspenger.saksbehandling.benk.domene.BenkAntallPerFane
import no.nav.tiltakspenger.saksbehandling.benk.domene.BenkBehandling
import no.nav.tiltakspenger.saksbehandling.benk.domene.BenkPaginering

/**
 * Én fane slik benken skal vise den, etter at radene saksbehandler ikke har tilgang til er tatt bort.
 *
 * Tilgangsfiltreringen skjer etter at siden er hentet fra databasen.
 * En side kan derfor vise færre enn [sideantall] rader, og [totalAntall] teller rader saksbehandler ikke ser.
 *
 * [totalAntall] og [totalAntallUfiltrert] er tellinger fra databasen, og er derfor ikke tilgangsfiltrert.
 * Benken bruker dem til å si hvor mye filtervalgene tok bort, mens [antallFiltrertPgaTilgang] sier hvor mye tilgangen tok bort.
 * De to tallene svarer på hvert sitt spørsmål, og skal derfor ikke slås sammen.
 *
 * [saksbehandlere] og [besluttere] er identene tildelt en rad i fanen, ufiltrert — de er kollegaer av saksbehandler, så tilgangsfiltreringen gjelder dem ikke.
 */
data class TilgangsfiltrertBenkOversikt<T : BenkBehandling>(
    val behandlinger: List<T>,
    val totalAntall: Int,
    val totalAntallUfiltrert: Int,
    val antallFiltrertPgaTilgang: Int,
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
    val oversikt: TilgangsfiltrertBenkOversikt<T>,
)
