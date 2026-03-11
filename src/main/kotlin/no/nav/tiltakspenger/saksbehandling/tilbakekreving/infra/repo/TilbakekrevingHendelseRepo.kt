package no.nav.tiltakspenger.saksbehandling.tilbakekreving.infra.repo

import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.hendelser.TilbakekrevingInfoBehovHendelse
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.hendelser.Tilbakekrevingshendelse
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.hendelser.TilbakekrevingshendelseId

interface TilbakekrevingHendelseRepo {
    fun lagreNy(
        hendelse: Tilbakekrevingshendelse,
        key: String,
        value: String,
        sessionContext: SessionContext? = null,
    ): Boolean

    fun oppdaterBehandletInfoBehovMedSvar(hendelseId: TilbakekrevingshendelseId, svarJson: String)
    fun oppdaterBehandletInfoBehovFeil(hendelseId: TilbakekrevingshendelseId, feil: String)

    fun hentUbehandledeInfoBehov(): List<TilbakekrevingInfoBehovHendelse>
}
