package no.nav.tiltakspenger.fakes.repos

import arrow.atomic.Atomic
import arrow.core.toNonEmptyListOrNull
import no.nav.tiltakspenger.libs.common.HendelseId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.meldekort.domene.Meldeperiode
import no.nav.tiltakspenger.meldekort.domene.MeldeperiodeKjede
import no.nav.tiltakspenger.meldekort.domene.MeldeperiodeKjeder
import no.nav.tiltakspenger.meldekort.ports.MeldeperiodeRepo
import java.time.LocalDateTime

class MeldeperiodeFakeRepo : MeldeperiodeRepo {
    private val data = Atomic(mutableMapOf<HendelseId, Meldeperiode>())

    override fun lagre(
        meldeperiode: Meldeperiode,
        sessionContext: SessionContext?,
    ) {
        data.get()[meldeperiode.hendelseId] = meldeperiode
    }

    override fun hentUsendteTilBruker(): List<Meldeperiode> {
        TODO("Not yet implemented")
    }

    override fun markerSomSendtTilBruker(hendelseId: HendelseId, tidspunkt: LocalDateTime) {
        TODO("Not yet implemented")
    }

    override fun hentForSakId(sakId: SakId, sessionContext: SessionContext?): MeldeperiodeKjeder {
        return data.get().values.filter {
            it.sakId == sakId
        }.groupBy {
            it.id
        }.values.mapNotNull {
            it.toNonEmptyListOrNull()?.let { it1 -> MeldeperiodeKjede(it1) }
        }.let {
            MeldeperiodeKjeder(it)
        }
    }
}
