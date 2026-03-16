package no.nav.tiltakspenger.saksbehandling.tilbakekreving.infra.kafka.dto

import no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.hendelser.Tilbakekrevingshendelse
import java.time.LocalDate
import java.time.LocalDateTime

data class TilbakekrevingInfoSvarDTO(
    override val eksternFagsakId: String,
    override val hendelseOpprettet: LocalDateTime,
    val mottaker: TilbakekrevingMottaker,
    val revurdering: TilbakekrevingRevurdering,
    val utvidPerioder: List<TilbakekrevingUtvidPeriode>,
    val behandlendeEnhet: String,
) : TilbakekrevingshendelseDTO {
    override val versjon: Int = 1
    override val hendelsestype = TilbakekrevingHendelsestypeDTO.fagsysteminfo_svar

    // Vi ønsker aldri å lagre vårt eget svar som egen hendelse i databasen
    override fun tilHendelseForLagring(key: String): Tilbakekrevingshendelse? {
        return null
    }

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
