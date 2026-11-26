package no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling

import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeKjedeId
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.brukersmeldekort.BrukersMeldekort
import no.nav.tiltakspenger.saksbehandling.sak.Sak

/**
 * Meldekortene fra bruker på kjeden som ikke er behandlet av en annen meldekortbehandling, sortert eldst først.
 * Et meldekort er behandlet når det er knyttet til en annen behandling av kjeden som ikke er avbrutt.
 * Meldekort knyttet til [meldekortbehandlingId] regnes som ubehandlede, slik at behandlingen beholder dem når den lagres på nytt.
 *
 * Manuelle behandlinger ble ikke knyttet til meldekortene de behandlet før dette ble innført, og har derfor ingen koblinger.
 * En iverksatt behandling uten koblinger til kjeden regnes derfor å ha behandlet meldekortene som var mottatt da den ble iverksatt.
 * TODO: Fjern tidsregelen når de eldre manuelle behandlingene er knyttet til meldekortene sine med en migrering.
 */
fun Sak.ubehandledeBrukersMeldekort(
    kjedeId: MeldeperiodeKjedeId,
    meldekortbehandlingId: MeldekortId,
): List<BrukersMeldekort> {
    val andreBehandlingerAvKjeden = this.meldekortbehandlinger
        .hentIkkeAvbrutteBehandlingerForKjede(kjedeId)
        .filter { it.id != meldekortbehandlingId }
        .map { behandling -> behandling to behandling.meldeperioder.single { it.kjedeId == kjedeId } }

    val behandledeMeldekortIder = andreBehandlingerAvKjeden
        .flatMap { (_, meldeperiodebehandling) -> meldeperiodebehandling.brukersMeldekort }
        .map { it.id }
        .toSet()

    val sisteIverksettelseUtenKobling = andreBehandlingerAvKjeden
        .filter { (_, meldeperiodebehandling) -> meldeperiodebehandling.brukersMeldekort.isEmpty() }
        .mapNotNull { (behandling, _) -> behandling.iverksattTidspunkt }
        .maxOrNull()

    return this.brukersMeldekort
        .filter { it.kjedeId == kjedeId }
        .filter { it.id !in behandledeMeldekortIder }
        .filter { sisteIverksettelseUtenKobling == null || it.mottatt > sisteIverksettelseUtenKobling }
        .sortedBy { it.mottatt }
}
