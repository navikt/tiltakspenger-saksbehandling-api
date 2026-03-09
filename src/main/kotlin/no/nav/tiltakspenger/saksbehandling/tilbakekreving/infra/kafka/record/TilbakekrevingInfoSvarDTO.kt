package no.nav.tiltakspenger.saksbehandling.tilbakekreving.infra.kafka.record

import no.nav.tiltakspenger.saksbehandling.tilbakekreving.infra.domene.hendelser.Tilbakekrevingshendelse
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.infra.domene.hendelser.TilbakekrevingshendelseId
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime

data class TilbakekrevingInfoSvarDTO(
    val id: TilbakekrevingshendelseId,
    override val eksternFagsakId: String,
    override val hendelseOpprettet: LocalDateTime,
    val mottaker: TilbakekrevingMottaker,
    val revurdering: TilbakekrevingRevurdering,
    val utvidPerioder: List<TilbakekrevingUtvidPeriode>,
) : TilbakekrevingshendelseDTO {
    override val versjon: Int = 1

    override fun tilNyHendelse(
        key: String,
        clock: Clock,
    ): Tilbakekrevingshendelse {
        TODO("Not yet implemented")
    }

    override val hendelsestype = TilbakekrevingHendelsestypeDTO.fagsysteminfo_svar

    data class TilbakekrevingMottaker(
        // Vet ikke hvilke verdier denne kan ha ennå, så setter den til String inntil videre
        val type: TilbakekrevingMottakerType = TilbakekrevingMottakerType.PERSON,
        val ident: String,
    )

    enum class TilbakekrevingMottakerType {
        PERSON,
    }

    data class TilbakekrevingRevurdering(
        val behandlingId: String,
        val årsak: TilbakekrevingRevurderingÅrsak,
        val årsakTilFeilutbetaling: String?,
        val vedtaksdato: LocalDate,
    )

    enum class TilbakekrevingRevurderingÅrsak {
        NYE_OPPLYSNINGER,
        KORRIGERING,
        KLAGE,
        UKJENT,
    }

    data class TilbakekrevingUtvidPeriode(
        val kravgrunnlagPeriode: TilbakekrevingPeriodeDTO,
        val vedtaksperiode: TilbakekrevingPeriodeDTO,
    )
}
