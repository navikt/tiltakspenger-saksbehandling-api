package no.nav.tiltakspenger.saksbehandling.klage.domene.opprettBehandlingFraKlage

import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeKjedeId
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldekortUnderBehandling
import no.nav.tiltakspenger.saksbehandling.sak.Sak

sealed interface OpprettBehandlingForKlageResultat {
    val sak: Sak

    data class RammebehandlingOpprettet(
        override val sak: Sak,
        val rammebehandling: Rammebehandling,
    ) : OpprettBehandlingForKlageResultat

    data class MeldekortbehandlingOpprettet(
        override val sak: Sak,
        val meldekortbehandling: MeldekortUnderBehandling,
        val kjedeId: MeldeperiodeKjedeId,
    ) : OpprettBehandlingForKlageResultat
}
