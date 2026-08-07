package no.nav.tiltakspenger.saksbehandling.benk.v2.service

import no.nav.tiltakspenger.saksbehandling.benk.v2.domene.BenkV2AntallPerFane
import no.nav.tiltakspenger.saksbehandling.benk.v2.domene.BenkV2Behandling
import no.nav.tiltakspenger.saksbehandling.benk.v2.domene.BenkV2Repo

/**
 * Én fane slik benken skal vise den, etter at radene saksbehandler ikke har tilgang til er tatt bort.
 *
 * [totalAntall] og [totalAntallUfiltrert] er tellinger fra databasen, og er derfor ikke tilgangsfiltrert.
 * Benken bruker dem til å si hvor mye filtervalgene tok bort, mens [antallFiltrertPgaTilgang] sier hvor mye tilgangen tok bort.
 * De to tallene svarer på hvert sitt spørsmål, og skal derfor ikke slås sammen.
 */
data class TilgangsfiltrertBenkV2Oversikt<T : BenkV2Behandling>(
    val behandlinger: List<T>,
    val totalAntall: Int,
    val totalAntallUfiltrert: Int,
    val antallFiltrertPgaTilgang: Int,
) {
    val limit = BenkV2Repo.DEFAULT_LIMIT
}

/**
 * Hele svaret på ett benk-kall: fanen det ble spurt om, og antallet i alle fanene.
 * Antallet i alle fanene følger med fordi benken viser det i fanetitlene, og ellers måtte hentet det i et eget kall.
 */
data class BenkV2Respons<T : BenkV2Behandling>(
    val antallPerFane: BenkV2AntallPerFane,
    val oversikt: TilgangsfiltrertBenkV2Oversikt<T>,
)
