package no.nav.tiltakspenger.saksbehandling.tilbakekreving.infra.repo

import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.TilbakekrevingBehandling
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.TilbakekrevingBehandlingsstatus
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.TilbakekrevingBehandlingsstatusIntern
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.TilbakekrevingId
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.UtbetalingId

class TilbakekrevingBehandlingFakeRepo : TilbakekrevingBehandlingRepo {
    private val data = arrow.atomic.Atomic(mutableMapOf<TilbakekrevingId, TilbakekrevingBehandling>())

    override fun lagre(tilbakekrevingBehandling: TilbakekrevingBehandling, sessionContext: SessionContext?) {
        data.get()[tilbakekrevingBehandling.id] = tilbakekrevingBehandling
    }

    override fun taBehandlingSaksbehandler(
        tilbakekrevingBehandling: TilbakekrevingBehandling,
        sessionContext: SessionContext?,
    ): Boolean {
        val existing = data.get()[tilbakekrevingBehandling.id] ?: return false
        if (existing.saksbehandler != null || existing.status != TilbakekrevingBehandlingsstatus.TIL_BEHANDLING) return false
        data.get()[tilbakekrevingBehandling.id] = tilbakekrevingBehandling
        return true
    }

    override fun taBehandlingBeslutter(
        tilbakekrevingBehandling: TilbakekrevingBehandling,
        sessionContext: SessionContext?,
    ): Boolean {
        val existing = data.get()[tilbakekrevingBehandling.id] ?: return false
        if (existing.beslutter != null || existing.status != TilbakekrevingBehandlingsstatus.TIL_GODKJENNING) return false
        data.get()[tilbakekrevingBehandling.id] = tilbakekrevingBehandling
        return true
    }

    override fun overtaSaksbehandler(
        tilbakekrevingBehandling: TilbakekrevingBehandling,
        nåværendeSaksbehandler: String,
        sessionContext: SessionContext?,
    ): Boolean {
        val existing = data.get()[tilbakekrevingBehandling.id] ?: return false
        if (existing.saksbehandler != nåværendeSaksbehandler || existing.statusIntern != TilbakekrevingBehandlingsstatusIntern.UNDER_BEHANDLING) return false
        data.get()[tilbakekrevingBehandling.id] = tilbakekrevingBehandling
        return true
    }

    override fun overtaBeslutter(
        tilbakekrevingBehandling: TilbakekrevingBehandling,
        nåværendeBeslutter: String,
        sessionContext: SessionContext?,
    ): Boolean {
        val existing = data.get()[tilbakekrevingBehandling.id] ?: return false
        if (existing.beslutter != nåværendeBeslutter || existing.statusIntern != TilbakekrevingBehandlingsstatusIntern.UNDER_GODKJENNING) return false
        data.get()[tilbakekrevingBehandling.id] = tilbakekrevingBehandling
        return true
    }

    override fun leggTilbakeSaksbehandler(
        tilbakekrevingBehandling: TilbakekrevingBehandling,
        nåværendeSaksbehandler: String,
        sessionContext: SessionContext?,
    ): Boolean {
        val existing = data.get()[tilbakekrevingBehandling.id] ?: return false
        if (existing.saksbehandler != nåværendeSaksbehandler || existing.statusIntern != TilbakekrevingBehandlingsstatusIntern.UNDER_BEHANDLING) return false
        data.get()[tilbakekrevingBehandling.id] = tilbakekrevingBehandling
        return true
    }

    override fun leggTilbakeBeslutter(
        tilbakekrevingBehandling: TilbakekrevingBehandling,
        nåværendeBeslutter: String,
        sessionContext: SessionContext?,
    ): Boolean {
        val existing = data.get()[tilbakekrevingBehandling.id] ?: return false
        if (existing.beslutter != nåværendeBeslutter || existing.statusIntern != TilbakekrevingBehandlingsstatusIntern.UNDER_GODKJENNING) return false
        data.get()[tilbakekrevingBehandling.id] = tilbakekrevingBehandling
        return true
    }

    override fun hent(id: TilbakekrevingId, sessionContext: SessionContext?): TilbakekrevingBehandling? {
        return data.get()[id]
    }

    override fun hentForTilbakeBehandlingId(
        id: String,
        sessionContext: SessionContext?,
    ): TilbakekrevingBehandling? {
        return data.get().values.find { it.tilbakeBehandlingId == id }
    }

    override fun hentForSakId(sakId: SakId, sessionContext: SessionContext?): List<TilbakekrevingBehandling> {
        return data.get().values.filter { it.sakId == sakId }
    }

    override fun hentForUtbetalingId(
        utbetalingId: UtbetalingId,
        sessionContext: SessionContext?,
    ): List<TilbakekrevingBehandling> {
        return data.get().values.filter { utbetalingId in it.utbetalingIder }
    }
}
