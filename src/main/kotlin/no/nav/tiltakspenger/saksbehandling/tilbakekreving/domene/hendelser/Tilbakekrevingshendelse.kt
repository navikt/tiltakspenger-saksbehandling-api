package no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.hendelser

import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Ulid
import no.nav.tiltakspenger.libs.common.UlidBase
import ulid.ULID
import java.time.LocalDateTime

sealed interface Tilbakekrevingshendelse {
    val id: TilbakekrevinghendelseId
    val opprettet: LocalDateTime
    val behandlet: LocalDateTime?
    val hendelsestype: TilbakekrevinghendelseType
    val eksternFagsakId: String
    val sakId: SakId?
}

data class TilbakekrevinghendelseId private constructor(
    private val ulid: UlidBase,
) : Ulid by ulid {

    companion object {
        private const val PREFIX = "tilbakekrevingshendelse"

        fun random() = TilbakekrevinghendelseId(ulid = UlidBase("${PREFIX}_${ULID.randomULID()}"))

        fun fromString(stringValue: String) = TilbakekrevinghendelseId(ulid = UlidBase(stringValue))
    }
}

enum class TilbakekrevinghendelseType {
    InfoBehov,
    BehandlingEndret,
}
