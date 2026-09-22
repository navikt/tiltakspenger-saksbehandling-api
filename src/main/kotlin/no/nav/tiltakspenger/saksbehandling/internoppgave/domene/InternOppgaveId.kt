package no.nav.tiltakspenger.saksbehandling.internoppgave.domene

import no.nav.tiltakspenger.libs.common.Ulid
import no.nav.tiltakspenger.libs.common.UlidBase
import no.nav.tiltakspenger.libs.common.uuidToUlid
import ulid.ULID
import java.util.UUID

data class InternOppgaveId private constructor(
    private val ulid: UlidBase,
) : Ulid by ulid {
    override fun toString(): String = ulid.toString()

    companion object {
        private const val PREFIX = "internoppgave"

        fun random() = InternOppgaveId(UlidBase("${PREFIX}_${ULID.randomULID()}"))

        fun fromString(stringValue: String): InternOppgaveId {
            require(stringValue.startsWith("${PREFIX}_")) { "Ugyldig prefiks for InternOppgaveId" }
            return InternOppgaveId(UlidBase(stringValue))
        }

        fun fromUUID(uuid: UUID) = InternOppgaveId(UlidBase("${PREFIX}_${uuidToUlid(uuid)}"))
    }
}
