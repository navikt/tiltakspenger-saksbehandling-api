package no.nav.tiltakspenger.saksbehandling.tilbakekreving.infra.repo

import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.TilbakekrevingBehandling
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.TilbakekrevingId
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.UtbetalingId

interface TilbakekrevingBehandlingRepo {
    fun lagre(tilbakekrevingBehandling: TilbakekrevingBehandling, sessionContext: SessionContext? = null)
    fun hent(id: TilbakekrevingId, sessionContext: SessionContext? = null): TilbakekrevingBehandling?
    fun hentForSakId(sakId: SakId, sessionContext: SessionContext? = null): List<TilbakekrevingBehandling>
    fun hentForUtbetalingId(utbetalingId: UtbetalingId, sessionContext: SessionContext? = null): TilbakekrevingBehandling?
}
