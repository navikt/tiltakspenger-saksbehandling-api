package no.nav.tiltakspenger.saksbehandling.meldekort.ports

import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.MeldeperiodeId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.BrukersMeldekort
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.BrukersMeldekortBehandletAutomatiskStatus
import no.nav.tiltakspenger.saksbehandling.oppgave.OppgaveId

interface BrukersMeldekortRepo {
    fun lagre(
        brukersMeldekort: BrukersMeldekort,
        sessionContext: SessionContext? = null,
    )

    fun oppdaterOppgaveId(
        meldekortId: MeldekortId,
        oppgaveId: OppgaveId,
        sessionContext: SessionContext? = null,
    )

    fun hentForSakId(
        sakId: SakId,
        sessionContext: SessionContext? = null,
    ): List<BrukersMeldekort>

    fun hentForMeldekortId(
        meldekortId: MeldekortId,
        sessionContext: SessionContext? = null,
    ): BrukersMeldekort?

    fun hentForMeldeperiodeId(
        meldeperiodeId: MeldeperiodeId,
        sessionContext: SessionContext? = null,
    ): BrukersMeldekort?

    fun hentMeldekortSomDetSkalOpprettesOppgaveFor(
        sessionContext: SessionContext? = null,
    ): List<BrukersMeldekort>

    fun hentMeldekortSomSkalBehandlesAutomatisk(
        sessionContext: SessionContext? = null,
    ): List<BrukersMeldekort>

    fun oppdaterAutomatiskBehandletStatus(
        meldekortId: MeldekortId,
        status: BrukersMeldekortBehandletAutomatiskStatus,
        behandlesAutomatisk: Boolean,
        sessionContext: SessionContext? = null,
    )
}
