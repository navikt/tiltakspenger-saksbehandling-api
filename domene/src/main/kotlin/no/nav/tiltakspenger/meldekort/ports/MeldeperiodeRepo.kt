package no.nav.tiltakspenger.meldekort.ports

import no.nav.tiltakspenger.libs.common.HendelseId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.meldekort.domene.Meldeperiode
import no.nav.tiltakspenger.meldekort.domene.MeldeperiodeKjeder
import java.time.LocalDateTime

interface MeldeperiodeRepo {

    fun lagre(
        meldeperiode: Meldeperiode,
        sessionContext: SessionContext? = null,
    )

    fun hentUsendteTilBruker(): List<Meldeperiode>

    fun markerSomSendtTilBruker(hendelseId: HendelseId, tidspunkt: LocalDateTime)

    fun hentForSakId(
        sakId: SakId,
        sessionContext: SessionContext? = null,
    ): MeldeperiodeKjeder
}
