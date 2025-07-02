package no.nav.tiltakspenger.saksbehandling.meldekort.ports

import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.BeregningId
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldeperiodeBeregning

interface MeldeperiodeBeregningRepo {

    fun lagre(
        meldeperiodeBeregning: MeldeperiodeBeregning,
        sessionContext: SessionContext? = null,
    )

    fun hent(
        id: BeregningId,
        sessionContext: SessionContext? = null,
    ): MeldeperiodeBeregning?
}
