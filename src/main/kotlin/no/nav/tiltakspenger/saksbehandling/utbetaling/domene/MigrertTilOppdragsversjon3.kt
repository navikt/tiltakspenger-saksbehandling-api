package no.nav.tiltakspenger.saksbehandling.utbetaling.domene

import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksnummer

/**
 * @param alleredeMigrert `true` når saken allerede var på [Oppdragsversjon.V3], slik at ingenting ble endret.
 */
data class MigrertTilOppdragsversjon3(
    val sakId: SakId,
    val saksnummer: Saksnummer,
    val alleredeMigrert: Boolean,
)
