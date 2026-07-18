package no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.hendelser

import java.time.LocalDateTime

/**
 * Brukes når vi mottar en tilbakekrevingshendelse fra Kafka som vi ikke klarer å deserialisere.
 * Lagres slik at vi kan undersøke hendelsen i ettertid, uten å blokkere konsumeringen av nye hendelser.
 *
 * Merk: Vi har ikke deserialisert kafka-meldingen, så vi vet ikke nødvendigvis verdiene for [eksternFagsakId] eller det egentlige opprettet-tidspunktet.
 * [opprettet] settes til når vi mottok hendelsen.
 */
data class TilbakekrevingUkjentHendelse(
    override val id: TilbakekrevinghendelseId,
    override val opprettet: LocalDateTime,
    override val behandlet: LocalDateTime? = null,
    override val feil: TilbakekrevinghendelseFeil? = null,
    val value: String,
) : Tilbakekrevingshendelse {
    override val hendelsestype = TilbakekrevinghendelseType.Ukjent
    override val sakId = null
    override val eksternFagsakId = null
}
