package no.nav.tiltakspenger.fakes.repos

import arrow.atomic.Atomic
import no.nav.tiltakspenger.felles.HendelseId
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.meldekort.domene.Meldeperiode
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
}
