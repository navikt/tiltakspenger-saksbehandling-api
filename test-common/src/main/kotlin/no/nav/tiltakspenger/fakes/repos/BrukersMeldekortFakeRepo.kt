package no.nav.tiltakspenger.fakes.repos

import arrow.atomic.Atomic
import no.nav.tiltakspenger.libs.common.HendelseId
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.meldekort.domene.BrukersMeldekort
import no.nav.tiltakspenger.meldekort.domene.BrukersMeldekortRepo
import no.nav.tiltakspenger.meldekort.domene.NyttBrukersMeldekort

class BrukersMeldekortFakeRepo(val meldeperiodeFakeRepo: MeldeperiodeFakeRepo) : BrukersMeldekortRepo {
    private val data = Atomic(mutableMapOf<MeldekortId, BrukersMeldekort>())

    override fun lagre(brukersMeldekort: NyttBrukersMeldekort, sessionContext: SessionContext?) {
        val meldeperiode = meldeperiodeFakeRepo.hentForHendelseId(brukersMeldekort.meldeperiodeHendelseId)

        requireNotNull(meldeperiode) { "Ingen meldeperiode for ${brukersMeldekort.meldeperiodeHendelseId}" }

        data.get()[brukersMeldekort.id] = BrukersMeldekort(
            id = brukersMeldekort.id,
            sakId = brukersMeldekort.sakId,
            meldeperiode = meldeperiode,
            mottatt = brukersMeldekort.mottatt,
            dager = brukersMeldekort.dager,
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
        hendelseId: HendelseId,
        sessionContext: SessionContext?,
    ): BrukersMeldekort? {
        return data.get().values.find { it.meldeperiode.id == hendelseId }
    }
}
