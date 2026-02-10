package no.nav.tiltakspenger.saksbehandling.klage.ports

import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.saksbehandling.klage.domene.hendelse.NyKlagehendelse

interface KlagehendelseRepo {
    fun lagreNyHendelse(nyKlagehendelse: NyKlagehendelse, sessionContext: SessionContext? = null)
}
