package no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling

import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeKjedeId
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.brukersmeldekort.BrukersMeldekort
import no.nav.tiltakspenger.saksbehandling.sak.Sak

/**
 * Meldekortene fra bruker på kjeden som ingen iverksatt eller avbrutt behandling har behandlet, sortert eldst først.
 * Se [BrukersMeldekort.behandletAv].
 * Siden en kjede bare kan ha én åpen behandling om gangen, er meldekortene den åpne behandlingen er knyttet til, alltid blant de ubehandlede.
 */
fun Sak.ubehandledeBrukersMeldekort(kjedeId: MeldeperiodeKjedeId): List<BrukersMeldekort> {
    return this.brukersMeldekort
        .filter { it.kjedeId == kjedeId && it.behandletAv == null }
        .sortedBy { it.mottatt }
}
