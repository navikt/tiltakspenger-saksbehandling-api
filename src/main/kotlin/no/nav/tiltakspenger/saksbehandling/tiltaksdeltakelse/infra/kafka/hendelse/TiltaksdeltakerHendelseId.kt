package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.kafka.hendelse

import no.nav.tiltakspenger.libs.common.Ulid
import no.nav.tiltakspenger.libs.common.UlidBase
import ulid.ULID

data class TiltaksdeltakerHendelseId private constructor(
    private val ulid: UlidBase,
) : Ulid by ulid {

    companion object {
        private const val PREFIX = "tiltaksdeltakerhendelse"

        fun random() = TiltaksdeltakerHendelseId(ulid = UlidBase("${PREFIX}_${ULID.randomULID()}"))

        fun fromString(stringValue: String) = TiltaksdeltakerHendelseId(ulid = UlidBase(stringValue))
    }
}
