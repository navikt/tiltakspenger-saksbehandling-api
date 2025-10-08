package no.nav.tiltakspenger.saksbehandling.meldekort.infra.repo

import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeId
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeKjedeId
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.BrukersMeldekort
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.BrukersMeldekortBehandletAutomatiskStatus
import no.nav.tiltakspenger.saksbehandling.meldekort.ports.BrukersMeldekortRepo

class BrukersMeldekortFakeRepo(private val meldeperiodeFakeRepo: MeldeperiodeFakeRepo) : BrukersMeldekortRepo {
    private val data = arrow.atomic.Atomic(mutableMapOf<MeldekortId, BrukersMeldekort>())

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
    ): List<BrukersMeldekort> {
        return data.get().values.filter { it.meldeperiodeId == meldeperiodeId }
    }

    override fun hentForKjedeId(
        kjedeId: MeldeperiodeKjedeId,
        sakId: SakId,
        sessionContext: SessionContext?,
    ): List<BrukersMeldekort> {
        return data.get().values.filter { it.sakId == sakId && it.kjedeId == kjedeId }
    }

    override fun hentMeldekortSomSkalBehandlesAutomatisk(sessionContext: SessionContext?): List<BrukersMeldekort> {
        return data.get().values.filter { it.behandlesAutomatisk && it.behandletAutomatiskStatus == null }
            .sortedBy { it.periode.fraOgMed }.distinctBy { it.sakId }
    }

    override fun oppdaterAutomatiskBehandletStatus(
        meldekortId: MeldekortId,
        status: BrukersMeldekortBehandletAutomatiskStatus,
        behandlesAutomatisk: Boolean,
        sessionContext: SessionContext?,
    ) {
        data.get()[meldekortId] = data.get()[meldekortId]!!.copy(
            behandlesAutomatisk = behandlesAutomatisk,
            behandletAutomatiskStatus = status,
        )
    }
}
