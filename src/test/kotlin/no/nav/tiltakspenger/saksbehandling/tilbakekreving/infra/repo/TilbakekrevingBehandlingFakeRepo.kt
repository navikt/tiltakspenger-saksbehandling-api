package no.nav.tiltakspenger.saksbehandling.tilbakekreving.infra.repo

import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.TilbakekrevingBehandling
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.TilbakekrevingId
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.UtbetalingId

class TilbakekrevingBehandlingFakeRepo : TilbakekrevingBehandlingRepo {
    private val data = arrow.atomic.Atomic(mutableMapOf<TilbakekrevingId, TilbakekrevingBehandling>())

    override fun lagre(tilbakekrevingBehandling: TilbakekrevingBehandling, sessionContext: SessionContext?) {
        data.get()[tilbakekrevingBehandling.id] = tilbakekrevingBehandling
    }

    override fun hent(id: TilbakekrevingId, sessionContext: SessionContext?): TilbakekrevingBehandling? {
        return data.get()[id]
    }

    override fun hentForSakId(sakId: SakId, sessionContext: SessionContext?): List<TilbakekrevingBehandling> {
        return data.get().values.filter { it.sakId == sakId }
    }

    override fun hentForUtbetalingId(utbetalingId: UtbetalingId, sessionContext: SessionContext?): TilbakekrevingBehandling? {
        return data.get().values.find { it.utbetalingId == utbetalingId }
    }
}
