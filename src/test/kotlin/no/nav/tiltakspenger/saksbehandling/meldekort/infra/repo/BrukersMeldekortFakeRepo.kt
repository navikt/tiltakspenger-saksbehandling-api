package no.nav.tiltakspenger.saksbehandling.meldekort.infra.repo

import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.saksbehandling.felles.Forsøkshistorikk
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.brukersmeldekort.BrukersMeldekort
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldekortBehandletAutomatiskStatus
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
            behandlesAutomatisk = brukersMeldekort.behandlesAutomatisk,
            behandletAutomatiskStatus = brukersMeldekort.behandletAutomatiskStatus,
            behandletAutomatiskForsøkshistorikk = brukersMeldekort.behandletAutomatiskForsøkshistorikk,
        )
    }

    fun hentForSakId(sakId: SakId): List<BrukersMeldekort> {
        return data.get().values.filter {
            it.sakId == sakId
        }
    }

    override fun hentForMeldekortId(meldekortId: MeldekortId, sessionContext: SessionContext?): BrukersMeldekort? {
        return data.get()[meldekortId]
    }

    override fun hentMeldekortSomSkalBehandlesAutomatisk(limit: Int, sessionContext: SessionContext?): List<BrukersMeldekort> {
        return data.get().values
            .filter { it.behandlesAutomatisk }
            .sortedBy { it.periode.fraOgMed }.distinctBy { it.sakId }
            .take(limit)
    }

    override fun oppdaterAutomatiskBehandletStatus(
        meldekortId: MeldekortId,
        status: MeldekortBehandletAutomatiskStatus,
        behandlesAutomatisk: Boolean,
        metadata: Forsøkshistorikk,
        sessionContext: SessionContext?,
    ) {
        data.get()[meldekortId] = data.get()[meldekortId]!!.copy(
            behandlesAutomatisk = behandlesAutomatisk,
            behandletAutomatiskStatus = status,
            behandletAutomatiskForsøkshistorikk = metadata,
        )
    }

    override fun markerSomAutomatiskBehandlet(
        meldekortId: MeldekortId,
        metadata: Forsøkshistorikk,
        sessionContext: SessionContext?,
    ) {
        data.get()[meldekortId] = data.get()[meldekortId]!!.copy(
            behandlesAutomatisk = false,
            behandletAutomatiskStatus = MeldekortBehandletAutomatiskStatus.BEHANDLET,
            behandletAutomatiskForsøkshistorikk = metadata,
        )
    }
}
