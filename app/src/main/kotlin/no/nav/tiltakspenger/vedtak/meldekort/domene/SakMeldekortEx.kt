package no.nav.tiltakspenger.vedtak.meldekort.domene

import no.nav.tiltakspenger.vedtak.saksbehandling.domene.sak.Sak

fun Sak.sisteGodkjenteMeldekortBehandling(): MeldekortBehandling? {
    return meldekortBehandlinger.godkjenteMeldekort.lastOrNull()
}
