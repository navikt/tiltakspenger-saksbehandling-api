package no.nav.tiltakspenger.saksbehandling.behandling.domene.avbryt

import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.NonBlankString
import no.nav.tiltakspenger.libs.common.RammebehandlingId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.Saksnummer

/**
 * Avbryter kun tilhørende søknad dersom dette er den første søknadsbehandlingen som vurderer den søknaden.
 * @param antallVisteAutomatiskOpprettedeGrunner Antallet automatisk opprettede grunner saksbehandler fikk vist da behandlingen ble avbrutt.
 * Er null når klienten ikke oppgir det.
 */
data class AvbrytRammebehandlingKommando(
    val saksnummer: Saksnummer,
    val behandlingId: RammebehandlingId,
    val avsluttetAv: Saksbehandler,
    val correlationId: CorrelationId,
    val begrunnelse: NonBlankString,
    val antallVisteAutomatiskOpprettedeGrunner: Int?,
)
