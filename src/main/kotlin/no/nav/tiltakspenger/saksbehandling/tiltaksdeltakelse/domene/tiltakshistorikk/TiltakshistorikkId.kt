package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.domene.tiltakshistorikk

import no.nav.tiltakspenger.libs.common.Ulid
import no.nav.tiltakspenger.libs.common.UlidBase
import ulid.ULID

data class TiltakshistorikkId private constructor(
    private val ulid: UlidBase,
) : Ulid by ulid {

    companion object {
        private const val PREFIX = "tiltakshistorikk"

        fun random() = TiltakshistorikkId(ulid = UlidBase("${PREFIX}_${ULID.randomULID()}"))

        fun fromString(stringValue: String) = TiltakshistorikkId(ulid = UlidBase(stringValue))
    }
}
