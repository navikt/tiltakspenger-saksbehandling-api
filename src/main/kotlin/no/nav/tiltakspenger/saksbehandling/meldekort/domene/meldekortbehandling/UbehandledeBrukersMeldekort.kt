package no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling

import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeKjedeId
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.brukersmeldekort.BrukersMeldekort
import no.nav.tiltakspenger.saksbehandling.sak.Sak

/**
 * Meldekortene fra bruker på kjeden som ikke er behandlet av en annen meldekortbehandling, sortert eldst først.
 * Et meldekort er behandlet når det er knyttet til en annen behandling av kjeden som ikke er avbrutt.
 * En avbrutt behandling beholder koblingene sine, men meldekortene regnes fortsatt som ubehandlede.
 * Meldekort knyttet til [meldekortbehandlingId] regnes som ubehandlede, slik at behandlingen beholder dem når den lagres på nytt.
 * Manuelle behandlinger fra før koblingene ble innført, er knyttet til meldekortene sine av migreringen V287.
 */
fun Sak.ubehandledeBrukersMeldekort(
    kjedeId: MeldeperiodeKjedeId,
    meldekortbehandlingId: MeldekortId,
): List<BrukersMeldekort> {
    val behandledeMeldekortIder = this.meldekortbehandlinger
        .hentIkkeAvbrutteBehandlingerForKjede(kjedeId)
        .filter { it.id != meldekortbehandlingId }
        .flatMap { behandling -> behandling.meldeperioder.single { it.kjedeId == kjedeId }.brukersMeldekort }
        .map { it.id }
        .toSet()

    return this.brukersMeldekort
        .filter { it.kjedeId == kjedeId }
        .filter { it.id !in behandledeMeldekortIder }
        .sortedBy { it.mottatt }
}
