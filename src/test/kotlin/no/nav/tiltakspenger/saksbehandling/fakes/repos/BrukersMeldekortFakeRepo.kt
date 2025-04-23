package no.nav.tiltakspenger.saksbehandling.fakes.repos

import arrow.atomic.Atomic
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.MeldeperiodeId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.BrukersMeldekort
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.BrukersMeldekortBehandletAutomatiskStatus
import no.nav.tiltakspenger.saksbehandling.meldekort.ports.BrukersMeldekortRepo
import no.nav.tiltakspenger.saksbehandling.oppgave.OppgaveId

class BrukersMeldekortFakeRepo(private val meldeperiodeFakeRepo: MeldeperiodeFakeRepo) : BrukersMeldekortRepo {
    private val data = Atomic(mutableMapOf<MeldekortId, BrukersMeldekort>())

    override fun lagre(brukersMeldekort: BrukersMeldekort, sessionContext: SessionContext?) {
        val meldeperiode = meldeperiodeFakeRepo.hentForMeldeperiodeId(brukersMeldekort.meldeperiodeId)

        requireNotNull(meldeperiode) { "Ingen meldeperiode for ${brukersMeldekort.meldeperiodeId}" }
        require(data.get()[brukersMeldekort.id] == null) { "Meldekortet ${brukersMeldekort.id} er allerede lagret" }

        data.get()[brukersMeldekort.id] = BrukersMeldekort(
            id = brukersMeldekort.id,
            sakId = brukersMeldekort.sakId,
            meldeperiode = meldeperiode,
            mottatt = brukersMeldekort.mottatt,
            dager = brukersMeldekort.dager,
            journalpostId = brukersMeldekort.journalpostId,
            oppgaveId = brukersMeldekort.oppgaveId,
            behandlesAutomatisk = brukersMeldekort.behandlesAutomatisk,
            behandletAutomatiskStatus = brukersMeldekort.behandletAutomatiskStatus,
        )
    }

    override fun oppdaterOppgaveId(
        meldekortId: MeldekortId,
        oppgaveId: OppgaveId,
        sessionContext: SessionContext?,
    ) {
        data.get()[meldekortId] = data.get()[meldekortId]!!.copy(oppgaveId = oppgaveId)
    }

    override fun hentForSakId(sakId: SakId, sessionContext: SessionContext?): List<BrukersMeldekort> {
        return data.get().values.filter {
            it.sakId == sakId
        }
    }

    override fun hentForMeldekortId(meldekortId: MeldekortId, sessionContext: SessionContext?): BrukersMeldekort? {
        return data.get()[meldekortId]
    }

    override fun hentForMeldeperiodeId(
        meldeperiodeId: MeldeperiodeId,
        sessionContext: SessionContext?,
    ): BrukersMeldekort? {
        return data.get().values.find { it.meldeperiodeId == meldeperiodeId }
    }

    override fun hentMeldekortSomDetSkalOpprettesOppgaveFor(sessionContext: SessionContext?): List<BrukersMeldekort> {
        return data.get().values.filter { it.oppgaveId == null }
    }

    override fun hentMeldekortSomSkalBehandlesAutomatisk(sessionContext: SessionContext?): List<BrukersMeldekort> {
        TODO("Not yet implemented")
    }

    override fun oppdaterAutomatiskBehandletStatus(
        meldekortId: MeldekortId,
        status: BrukersMeldekortBehandletAutomatiskStatus,
        behandlesAutomatisk: Boolean,
        sessionContext: SessionContext?,
    ) {
        TODO("Not yet implemented")
    }
}
