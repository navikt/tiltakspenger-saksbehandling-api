package no.nav.tiltakspenger.saksbehandling.meldekort.ports

import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeId
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldeperiode.Meldeperiode

interface MeldeperiodeRepo {

    fun lagre(
        meldeperiode: Meldeperiode,
        sessionContext: SessionContext? = null,
    )

    fun lagre(
        meldeperioder: List<Meldeperiode>,
        sessionContext: SessionContext? = null,
    )

    fun hentForMeldeperiodeId(
        meldeperiodeId: MeldeperiodeId,
        sessionContext: SessionContext? = null,
    ): Meldeperiode?
}
