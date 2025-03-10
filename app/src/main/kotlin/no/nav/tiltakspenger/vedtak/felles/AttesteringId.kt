package no.nav.tiltakspenger.vedtak.felles

import no.nav.tiltakspenger.libs.common.Ulid
import no.nav.tiltakspenger.libs.common.UlidBase
import no.nav.tiltakspenger.libs.common.uuidToUlid
import ulid.ULID
import java.util.UUID

data class AttesteringId private constructor(
    private val ulid: UlidBase,
) : Ulid by ulid {
    companion object {
        private const val PREFIX = "att"

        fun random() = AttesteringId(ulid = UlidBase("${PREFIX}_${ULID.randomULID()}"))

        fun fromString(stringValue: String) = AttesteringId(ulid = UlidBase(stringValue))

        fun fromUUID(uuid: UUID) = AttesteringId(ulid = UlidBase("${PREFIX}_${uuidToUlid(uuid)}"))
    }
}
