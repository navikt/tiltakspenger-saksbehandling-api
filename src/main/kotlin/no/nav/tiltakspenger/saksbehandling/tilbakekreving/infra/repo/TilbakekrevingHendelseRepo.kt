package no.nav.tiltakspenger.saksbehandling.tilbakekreving.infra.repo

import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.hendelser.TilbakekrevinghendelseId
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.hendelser.Tilbakekrevingshendelse

interface TilbakekrevingHendelseRepo {
    fun lagreNy(
        hendelse: Tilbakekrevingshendelse,
        sakId: SakId?,
        key: String,
        value: String,
        sessionContext: SessionContext? = null,
    ): Boolean

    fun hentUbehandledeHendelser(): List<Tilbakekrevingshendelse>

    fun markerInfoBehovSomBehandlet(hendelseId: TilbakekrevinghendelseId, svarJson: String, sessionContext: SessionContext? = null)
    fun markerEndringSomBehandlet(hendelseId: TilbakekrevinghendelseId, sessionContext: SessionContext? = null)
    fun markerSomBehandletMedFeil(hendelseId: TilbakekrevinghendelseId, feil: String, sessionContext: SessionContext? = null)
}
