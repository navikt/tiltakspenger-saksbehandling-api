package no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.hendelser

import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Ulid
import no.nav.tiltakspenger.libs.common.UlidBase
import no.nav.tiltakspenger.libs.common.uuidToUlid
import ulid.ULID
import java.time.LocalDateTime
import java.util.UUID

sealed interface Tilbakekrevingshendelse {
    val id: TilbakekrevingshendelseId
    val opprettet: LocalDateTime
    val behandlet: LocalDateTime?
    val hendelsestype: TilbakekrevingHendelsestype
    val eksternFagsakId: String
    val sakId: SakId?
}

data class TilbakekrevingshendelseId private constructor(
    private val ulid: UlidBase,
) : Ulid by ulid {

    companion object {
        private const val PREFIX = "tilbakekrevingshendelse"

        fun random() = TilbakekrevingshendelseId(ulid = UlidBase("${PREFIX}_${ULID.randomULID()}"))

        fun fromString(stringValue: String) = TilbakekrevingshendelseId(ulid = UlidBase(stringValue))

        fun fromUUID(uuid: UUID) = TilbakekrevingshendelseId(ulid = UlidBase("${PREFIX}_${uuidToUlid(uuid)}"))
    }
}

enum class TilbakekrevingHendelsestype {
    InfoBehov,
    InfoSvar,
    BehandlingEndret,
}
