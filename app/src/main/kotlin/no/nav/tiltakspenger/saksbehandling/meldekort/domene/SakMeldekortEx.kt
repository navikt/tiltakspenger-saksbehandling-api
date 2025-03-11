package no.nav.tiltakspenger.saksbehandling.meldekort.domene

import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.sak.Sak

fun Sak.sisteGodkjenteMeldekortBehandling(): MeldekortBehandling? {
    return meldekortBehandlinger.godkjenteMeldekort.lastOrNull()
}
