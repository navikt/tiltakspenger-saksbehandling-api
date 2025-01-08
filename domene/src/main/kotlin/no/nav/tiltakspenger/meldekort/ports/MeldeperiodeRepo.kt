package no.nav.tiltakspenger.meldekort.ports

import no.nav.tiltakspenger.felles.HendelseId
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.meldekort.domene.Meldeperiode
import java.time.LocalDateTime

interface MeldeperiodeRepo {

    fun lagre(
        meldeperiode: Meldeperiode,
        sessionContext: SessionContext? = null,
    )

    fun hentUsendteTilBruker(): List<Meldeperiode>

    fun markerSomSendtTilBruker(hendelseId: HendelseId, tidspunkt: LocalDateTime)
}
