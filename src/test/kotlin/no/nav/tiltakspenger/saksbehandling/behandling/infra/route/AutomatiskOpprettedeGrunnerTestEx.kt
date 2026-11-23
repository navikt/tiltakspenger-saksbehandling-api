package no.nav.tiltakspenger.saksbehandling.behandling.infra.route

import no.nav.tiltakspenger.libs.common.RammebehandlingId
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Revurdering
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContext

/**
 * Antallet automatisk opprettede grunner behandlingen har nå, slik frontend sender det når grunnene er vist.
 * Er 0 for søknadsbehandlinger og revurderinger uten slike grunner.
 */
fun TestApplicationContext.antallAutomatiskOpprettedeGrunner(behandlingId: RammebehandlingId): Int {
    val behandling = behandlingContext.rammebehandlingRepo.hent(behandlingId) as? Revurdering
    return behandling?.automatiskOpprettedeGrunner?.grunner?.size ?: 0
}
