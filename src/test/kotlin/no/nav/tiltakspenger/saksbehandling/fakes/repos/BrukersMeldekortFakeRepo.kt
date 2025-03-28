package no.nav.tiltakspenger.saksbehandling.fakes.repos

import arrow.atomic.Atomic
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.MeldeperiodeId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.BrukersMeldekort
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.BrukersMeldekortRepo
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.LagreBrukersMeldekortKommando

class BrukersMeldekortFakeRepo(private val meldeperiodeFakeRepo: MeldeperiodeFakeRepo) : BrukersMeldekortRepo {
    private val data = Atomic(mutableMapOf<MeldekortId, BrukersMeldekort>())

    override fun lagre(brukersMeldekort: LagreBrukersMeldekortKommando, sessionContext: SessionContext?) {
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
        )
    }

    override fun oppdater(brukersMeldekort: BrukersMeldekort, sessionContext: SessionContext?) {
        data.get()[brukersMeldekort.id] = brukersMeldekort
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
        return data.get().values.find { it.meldeperiode.id == meldeperiodeId }
    }

    override fun hentMeldekortSomIkkeSkalGodkjennesAutomatisk(sessionContext: SessionContext?): List<BrukersMeldekort> {
        return data.get().values.filter { it.oppgaveId == null }
    }
}
