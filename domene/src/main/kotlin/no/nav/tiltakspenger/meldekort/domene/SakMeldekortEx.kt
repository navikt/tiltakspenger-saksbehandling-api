package no.nav.tiltakspenger.meldekort.domene

import no.nav.tiltakspenger.saksbehandling.domene.sak.Sak

fun Sak.sisteGodkjenteMeldekortBehandling(): MeldekortBehandling? {
    return meldekortBehandlinger.godkjenteMeldekort.lastOrNull()
}
