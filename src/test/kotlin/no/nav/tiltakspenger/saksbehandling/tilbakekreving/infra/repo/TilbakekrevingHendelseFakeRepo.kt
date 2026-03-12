package no.nav.tiltakspenger.saksbehandling.tilbakekreving.infra.repo

import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.json.deserialize
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.hendelser.TilbakekrevingBehandlingEndretHendelse
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.hendelser.TilbakekrevingInfoBehovHendelse
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.hendelser.TilbakekrevingInfoSvarHendelse
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.hendelser.Tilbakekrevingshendelse
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.hendelser.TilbakekrevingshendelseId
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.infra.kafka.dto.TilbakekrevingInfoSvarDTO
import java.time.Clock

class TilbakekrevingHendelseFakeRepo(
    private val clock: Clock,
) : TilbakekrevingHendelseRepo {
    private val data = arrow.atomic.Atomic(mutableMapOf<TilbakekrevingshendelseId, Tilbakekrevingshendelse>())

    override fun lagreNy(
        hendelse: Tilbakekrevingshendelse,
        sakId: SakId?,
        key: String,
        value: String,
        sessionContext: SessionContext?,
    ): Boolean {
        if (hendelse is TilbakekrevingInfoBehovHendelse &&
            data.get().values.any {
                it is TilbakekrevingInfoBehovHendelse && it.kravgrunnlagReferanse == hendelse.kravgrunnlagReferanse
            }
        ) {
            return false
        }

        data.get()[hendelse.id] = hendelse
        return true
    }

    override fun hentUbehandledeHendelser(): List<Tilbakekrevingshendelse> {
        return data.get().values.filter { it.behandlet == null }
    }

    override fun markerInfoBehovSomBehandlet(
        hendelseId: TilbakekrevingshendelseId,
        svarJson: String,
        sessionContext: SessionContext?,
    ) {
        val hendelse = data.get()[hendelseId] as? TilbakekrevingInfoBehovHendelse
            ?: throw IllegalArgumentException("Fant ikke hendelse med id $hendelseId")

        data.get()[hendelseId] = hendelse.copy(
            behandlet = nå(clock),
            svar = deserialize<TilbakekrevingInfoSvarDTO>(svarJson),
        )
    }

    override fun markerEndringSomBehandlet(hendelseId: TilbakekrevingshendelseId, sessionContext: SessionContext?) {
        val hendelse = data.get()[hendelseId] as? TilbakekrevingBehandlingEndretHendelse
            ?: throw IllegalArgumentException("Fant ikke hendelse med id $hendelseId")

        data.get()[hendelseId] = hendelse.copy(behandlet = nå(clock))
    }

    override fun markerSomBehandletMedFeil(
        hendelseId: TilbakekrevingshendelseId,
        feil: String,
        sessionContext: SessionContext?,
    ) {
        val hendelse = data.get()[hendelseId] ?: throw IllegalArgumentException("Fant ikke hendelse med id $hendelseId")

        data.get()[hendelseId] = when (hendelse) {
            is TilbakekrevingBehandlingEndretHendelse -> hendelse.copy(behandlet = nå(clock))
            is TilbakekrevingInfoBehovHendelse -> hendelse.copy(behandlet = nå(clock))
            is TilbakekrevingInfoSvarHendelse -> hendelse.copy(behandlet = nå(clock))
        }
    }
}
