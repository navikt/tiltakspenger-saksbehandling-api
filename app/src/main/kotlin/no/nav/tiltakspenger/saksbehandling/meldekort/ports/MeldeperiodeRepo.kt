package no.nav.tiltakspenger.saksbehandling.meldekort.ports

import no.nav.tiltakspenger.libs.common.MeldeperiodeId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.Meldeperiode
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldeperiodeKjeder
import java.time.LocalDateTime

interface MeldeperiodeRepo {

    fun lagre(
        meldeperiode: Meldeperiode,
        sessionContext: SessionContext? = null,
    )

    fun lagre(
        meldeperioder: List<Meldeperiode>,
        sessionContext: SessionContext? = null,
    )

    fun hentUsendteTilBruker(): List<Meldeperiode>

    fun markerSomSendtTilBruker(meldeperiodeId: MeldeperiodeId, tidspunkt: LocalDateTime)

    fun hentForSakId(
        sakId: SakId,
        sessionContext: SessionContext? = null,
    ): MeldeperiodeKjeder

    fun hentForMeldeperiodeId(
        meldeperiodeId: MeldeperiodeId,
        sessionContext: SessionContext? = null,
    ): Meldeperiode?
}
