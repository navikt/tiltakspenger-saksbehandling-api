package no.nav.tiltakspenger.felles

import no.nav.tiltakspenger.libs.common.Ulid
import no.nav.tiltakspenger.libs.common.UlidBase
import no.nav.tiltakspenger.libs.common.uuidToUlid
import ulid.ULID
import java.util.UUID

data class HendelseId private constructor(
    private val ulid: UlidBase,
) : Ulid by ulid {
    companion object {
        private const val PREFIX = "hendelse"

        fun random() = HendelseId(ulid = UlidBase("${PREFIX}_${ULID.randomULID()}"))

        fun fromString(stringValue: String): HendelseId {
            require(stringValue.startsWith(PREFIX)) { "Prefix må starte med $PREFIX. Dette er nok ikke en HendelseId ($stringValue)" }
            return HendelseId(ulid = UlidBase(stringValue))
        }

        fun fromUUID(uuid: UUID) = HendelseId(ulid = UlidBase("${PREFIX}_${uuidToUlid(uuid)}"))
    }
}
