package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse

import no.nav.tiltakspenger.libs.common.Ulid
import no.nav.tiltakspenger.libs.common.UlidBase
import ulid.ULID

data class TiltaksdeltakerId private constructor(
    private val ulid: UlidBase,
) : Ulid by ulid {
    companion object {
        private const val PREFIX = "tiltaksdeltaker"

        fun random() = TiltaksdeltakerId(ulid = UlidBase("${PREFIX}_${ULID.randomULID()}"))

        fun fromString(stringValue: String): TiltaksdeltakerId {
            require(stringValue.startsWith(PREFIX)) { "Prefix må starte med $PREFIX. Dette er nok ikke en TiltaksdeltakerId ($stringValue)" }
            return TiltaksdeltakerId(ulid = UlidBase(stringValue))
        }
    }
}
