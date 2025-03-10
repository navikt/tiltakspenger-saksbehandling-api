package no.nav.tiltakspenger.fakes.repos

import arrow.atomic.Atomic
import no.nav.tiltakspenger.libs.common.MeldeperiodeId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.vedtak.meldekort.domene.Meldeperiode
import no.nav.tiltakspenger.vedtak.meldekort.domene.MeldeperiodeKjeder
import no.nav.tiltakspenger.vedtak.meldekort.ports.MeldeperiodeRepo
import java.time.LocalDateTime

class MeldeperiodeFakeRepo : MeldeperiodeRepo {
    private val data = Atomic(mutableMapOf<MeldeperiodeId, Meldeperiode>())

    override fun lagre(
        meldeperiode: Meldeperiode,
        sessionContext: SessionContext?,
    ) {
        data.get()[meldeperiode.id] = meldeperiode
    }

    override fun hentUsendteTilBruker(): List<Meldeperiode> {
        TODO("Not yet implemented")
    }

    override fun markerSomSendtTilBruker(meldeperiodeId: MeldeperiodeId, tidspunkt: LocalDateTime) {
        TODO("Not yet implemented")
    }

    override fun hentForSakId(sakId: SakId, sessionContext: SessionContext?): MeldeperiodeKjeder {
        return data.get().values.filter {
            it.sakId == sakId
        }.let {
            MeldeperiodeKjeder.fraMeldeperioder(it)
        }
    }

    override fun hentForMeldeperiodeId(meldeperiodeId: MeldeperiodeId, sessionContext: SessionContext?): Meldeperiode? {
        TODO("Not yet implemented")
    }
}
