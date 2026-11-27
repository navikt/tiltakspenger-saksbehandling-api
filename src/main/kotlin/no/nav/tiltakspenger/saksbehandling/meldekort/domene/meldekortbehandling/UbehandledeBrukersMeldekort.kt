package no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling

import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeKjedeId
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.brukersmeldekort.BrukersMeldekort
import no.nav.tiltakspenger.saksbehandling.sak.Sak

/**
 * Meldekortene fra bruker på kjeden som ingen iverksatt eller avbrutt behandling har behandlet, sortert eldst først.
 * Se [BrukersMeldekort.behandlingId].
 * Siden en kjede bare kan ha én åpen behandling om gangen, er meldekortene den åpne behandlingen er knyttet til, alltid blant de ubehandlede.
 */
fun Sak.ubehandledeBrukersMeldekort(kjedeId: MeldeperiodeKjedeId): List<BrukersMeldekort> {
    return this.brukersMeldekort
        .filter { it.kjedeId == kjedeId && it.behandlingId == null }
        .sortedBy { it.mottatt }
}

/**
 * Om meldekortet fra bruker venter på at en behandling tar stilling til det.
 * Det gjør det så lenge ingen iverksatt eller avbrutt behandling har behandlet det, og den åpne behandlingen av kjeden ikke er knyttet til det.
 * En åpen behandling knyttes til de ubehandlede meldekortene når den opprettes og lagres, så et meldekort mottatt etter siste lagring venter fortsatt.
 * Er en nyere innsending for kjeden behandlet, er meldekortet erstattet av den og venter ikke.
 * Det skjer når den nyere innsendingen behandles automatisk, siden en automatisk behandling bare behandler sitt eget meldekort.
 * Benken bruker samme regel for meldekort fra bruker.
 */
fun Sak.venterPåBehandling(brukersMeldekort: BrukersMeldekort): Boolean {
    if (brukersMeldekort.behandlingId != null) {
        return false
    }
    val erErstattetAvNyereBehandlet = this.brukersMeldekort.any {
        it.kjedeId == brukersMeldekort.kjedeId && it.behandlingId != null && it.mottatt > brukersMeldekort.mottatt
    }
    if (erErstattetAvNyereBehandlet) {
        return false
    }
    val åpenBehandling = meldekortbehandlinger.hentÅpenBehandlingForKjede(brukersMeldekort.kjedeId) ?: return true
    return åpenBehandling.meldeperioder.brukersMeldekort.none { it.id == brukersMeldekort.id }
}
