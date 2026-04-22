package no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene

import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.Ulid
import no.nav.tiltakspenger.libs.common.UlidBase
import ulid.ULID

data class TilbakekrevingId private constructor(
    private val ulid: UlidBase,
) : BehandlingId,
    Ulid by ulid {
    companion object {
        private const val PREFIX = "tilbakekreving"

        fun random() = TilbakekrevingId(ulid = UlidBase("${PREFIX}_${ULID.randomULID()}"))

        fun fromString(stringValue: String): TilbakekrevingId {
            require(stringValue.startsWith(PREFIX)) {
                "Prefix må starte med $PREFIX. Dette er ikke en TilbakekrevingId ($stringValue)"
            }
            return TilbakekrevingId(ulid = UlidBase(stringValue))
        }
    }
}
