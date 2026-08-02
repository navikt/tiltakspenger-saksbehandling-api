package no.nav.tiltakspenger.saksbehandling.tilbakekreving.ports

import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.TilbakekrevingBehandling

interface TilbakekrevingBehandlingRepo {
    fun lagre(tilbakekrevingBehandling: TilbakekrevingBehandling, sessionContext: SessionContext? = null)

    fun hentForTilbakeBehandlingId(id: String, sessionContext: SessionContext? = null): TilbakekrevingBehandling?

    /**
     * Kalles ikke fra prod — [no.nav.tiltakspenger.saksbehandling.sak.infra.repo.SakPostgresRepo] bruker companion-varianten som tar en åpen session.
     * Metoden står her fordi fake-repoet bygger saken sin gjennom den.
     */
    fun hentForSakId(sakId: SakId, sessionContext: SessionContext? = null): List<TilbakekrevingBehandling>

    /**
     * Ta behandling som saksbehandler.
     * Oppdaterer kun dersom saksbehandler_ident er null og status er TIL_BEHANDLING.
     */
    fun taBehandlingSaksbehandler(tilbakekrevingBehandling: TilbakekrevingBehandling, sessionContext: SessionContext? = null): Boolean

    /**
     * Ta behandling som beslutter.
     * Oppdaterer kun dersom beslutter_ident er null og status er TIL_GODKJENNING.
     */
    fun taBehandlingBeslutter(tilbakekrevingBehandling: TilbakekrevingBehandling, sessionContext: SessionContext? = null): Boolean

    /**
     * Overta fra en annen saksbehandler.
     * Oppdaterer kun dersom nåværende saksbehandler matcher [nåværendeSaksbehandler].
     */
    fun overtaSaksbehandler(tilbakekrevingBehandling: TilbakekrevingBehandling, nåværendeSaksbehandler: String, sessionContext: SessionContext? = null): Boolean

    /**
     * Overta fra en annen beslutter.
     * Oppdaterer kun dersom nåværende beslutter matcher [nåværendeBeslutter].
     */
    fun overtaBeslutter(tilbakekrevingBehandling: TilbakekrevingBehandling, nåværendeBeslutter: String, sessionContext: SessionContext? = null): Boolean

    /**
     * Legg tilbake som saksbehandler.
     * Oppdaterer kun dersom saksbehandler_ident matcher og status er UNDER_BEHANDLING.
     */
    fun leggTilbakeSaksbehandler(tilbakekrevingBehandling: TilbakekrevingBehandling, nåværendeSaksbehandler: String, sessionContext: SessionContext? = null): Boolean

    /**
     * Legg tilbake som beslutter.
     * Oppdaterer kun dersom beslutter_ident matcher og status er UNDER_GODKJENNING.
     */
    fun leggTilbakeBeslutter(tilbakekrevingBehandling: TilbakekrevingBehandling, nåværendeBeslutter: String, sessionContext: SessionContext? = null): Boolean
}
