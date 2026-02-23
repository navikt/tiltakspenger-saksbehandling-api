package no.nav.tiltakspenger.saksbehandling.klage.ports

import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.saksbehandling.klage.domene.hendelse.KlagehendelseId
import no.nav.tiltakspenger.saksbehandling.klage.domene.hendelse.NyKlagehendelse

interface KlagehendelseRepo {
    fun lagreNyHendelse(nyKlagehendelse: NyKlagehendelse, sessionContext: SessionContext? = null)
    fun hentUbehandledeHendelser(limit: Int = 10): List<NyKlagehendelse>
    fun hentNyHendelse(klagehendelseId: KlagehendelseId, sessionContext: SessionContext? = null): NyKlagehendelse?
    fun knyttHendelseTilSakOgKlagebehandling(nyKlagehendelse: NyKlagehendelse, sessionContext: SessionContext? = null)
}
