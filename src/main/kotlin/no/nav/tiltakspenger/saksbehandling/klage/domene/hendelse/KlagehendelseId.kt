package no.nav.tiltakspenger.saksbehandling.klage.domene.hendelse

import no.nav.tiltakspenger.libs.common.Ulid
import no.nav.tiltakspenger.libs.common.UlidBase
import no.nav.tiltakspenger.libs.common.uuidToUlid
import ulid.ULID
import java.util.UUID

data class KlagehendelseId private constructor(
    private val ulid: UlidBase,
) : Ulid by ulid {
    companion object {
        private const val PREFIX = "klagehendelse"

        fun random() = KlagehendelseId(ulid = UlidBase("${PREFIX}_${ULID.randomULID()}"))

        fun fromString(stringValue: String) = KlagehendelseId(ulid = UlidBase(stringValue))

        fun fromUUID(uuid: UUID) = KlagehendelseId(ulid = UlidBase("${PREFIX}_${uuidToUlid(uuid)}"))
    }
}
