package no.nav.tiltakspenger.saksbehandling.meldekort.domene

import no.nav.tiltakspenger.saksbehandling.sak.Sak

fun Sak.sisteGodkjenteMeldekortBehandling(): MeldekortBehandling? {
    return meldekortBehandlinger.godkjenteMeldekort.lastOrNull()
}
