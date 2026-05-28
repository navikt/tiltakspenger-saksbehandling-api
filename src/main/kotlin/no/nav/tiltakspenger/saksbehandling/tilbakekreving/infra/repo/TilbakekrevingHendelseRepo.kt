package no.nav.tiltakspenger.saksbehandling.tilbakekreving.infra.repo

import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.hendelser.TilbakekrevinghendelseFeil
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.hendelser.TilbakekrevinghendelseId
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.hendelser.Tilbakekrevingshendelse

interface TilbakekrevingHendelseRepo {
    fun lagreNy(
        hendelse: Tilbakekrevingshendelse,
        key: String,
        value: String,
        sessionContext: SessionContext? = null,
    ): Boolean

    fun hentUbehandledeHendelser(): List<Tilbakekrevingshendelse>

    fun hentHendelse(hendelseId: TilbakekrevinghendelseId): Tilbakekrevingshendelse?

    fun markerInfoBehovSomBehandlet(hendelseId: TilbakekrevinghendelseId, sakId: SakId, svarJson: String, sessionContext: SessionContext? = null)

    fun markerEndringSomBehandlet(hendelseId: TilbakekrevinghendelseId, sakId: SakId, sessionContext: SessionContext? = null)

    fun markerSomBehandletMedFeil(hendelseId: TilbakekrevinghendelseId, sakId: SakId?, feil: TilbakekrevinghendelseFeil, sessionContext: SessionContext? = null)

    /**
     * Oppdaterer en eksisterende [no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.hendelser.TilbakekrevingUkjentHendelse]
     * rad med innholdet fra en ny hendelse av kjent type, slik at den kan behandles normalt ved neste jobbkjøring.
     * [oppdatertHendelse] må ha samme id som den eksisterende ukjent-raden.
     */
    fun oppdaterUkjent(oppdatertHendelse: Tilbakekrevingshendelse, sessionContext: SessionContext? = null)

    fun slett(hendelseId: TilbakekrevinghendelseId, sessionContext: SessionContext? = null)
}
