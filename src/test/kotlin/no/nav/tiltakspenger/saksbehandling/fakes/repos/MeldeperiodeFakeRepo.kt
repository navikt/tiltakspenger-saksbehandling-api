@file:Suppress("UnusedImport")

package no.nav.tiltakspenger.saksbehandling.fakes.repos

import arrow.atomic.Atomic
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeId
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.Meldeperiode
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldeperiodeKjeder
import no.nav.tiltakspenger.saksbehandling.meldekort.ports.MeldeperiodeRepo
import java.time.LocalDateTime

class MeldeperiodeFakeRepo : MeldeperiodeRepo {
    private val data = Atomic(mutableMapOf<MeldeperiodeId, Meldeperiode>())

    override fun lagre(
        meldeperiode: Meldeperiode,
        sessionContext: SessionContext?,
    ) {
        data.get()[meldeperiode.id] = meldeperiode
    }

    override fun lagre(meldeperioder: List<Meldeperiode>, sessionContext: SessionContext?) {
        meldeperioder.forEach(::lagre)
    }

    override fun hentUsendteTilBruker(): List<Meldeperiode> {
        return data.get().filter { it.value.sendtTilMeldekortApi == null }.map { it.value }
    }

    override fun markerSomSendtTilBruker(meldeperiodeId: MeldeperiodeId, tidspunkt: LocalDateTime) {
        data.get()[meldeperiodeId] = data.get()[meldeperiodeId]!!.copy(
            sendtTilMeldekortApi = tidspunkt,
        )
    }

    override fun hentForSakId(sakId: SakId, sessionContext: SessionContext?): MeldeperiodeKjeder {
        return data.get().values.filter {
            it.sakId == sakId
        }.let {
            MeldeperiodeKjeder.fraMeldeperioder(it)
        }
    }

    override fun hentForMeldeperiodeId(meldeperiodeId: MeldeperiodeId, sessionContext: SessionContext?): Meldeperiode? {
        return data.get()[meldeperiodeId]
    }
}
