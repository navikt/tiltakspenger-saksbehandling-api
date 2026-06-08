package no.nav.tiltakspenger.saksbehandling.klage.domene

import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.Ulid
import no.nav.tiltakspenger.libs.common.UlidBase
import no.nav.tiltakspenger.libs.common.uuidToUlid
import ulid.ULID
import java.util.UUID

data class KlagebehandlingId private constructor(
    private val ulid: UlidBase,
) : BehandlingId,
    Ulid by ulid {
    companion object {
        const val PREFIX = "klage"

        fun random() = KlagebehandlingId(ulid = UlidBase("${PREFIX}_${ULID.randomULID()}"))

        fun fromString(stringValue: String) = KlagebehandlingId(ulid = UlidBase(stringValue))

        fun fromUUID(uuid: UUID) = KlagebehandlingId(ulid = UlidBase("${PREFIX}_${uuidToUlid(uuid)}"))
    }
}
