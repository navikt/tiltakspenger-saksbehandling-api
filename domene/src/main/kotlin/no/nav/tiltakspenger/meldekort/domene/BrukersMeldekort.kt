package no.nav.tiltakspenger.meldekort.domene

import no.nav.tiltakspenger.libs.common.MeldeperiodeId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.periodisering.Periode
import java.time.LocalDate
import java.time.LocalDateTime

data class BrukersMeldekort(
    // Tidspunktet mottatt fra bruker
    val mottatt: LocalDateTime,
    val meldeperiodeId: MeldeperiodeId,
    val versjon: Int,
    val sakId: SakId,

    val periode: Periode,
    val dager: List<BrukersMeldekortDag>,
) {
    data class BrukersMeldekortDag(
        val status: InnmeldtStatus,
        val dato: LocalDate,
    )
}

enum class InnmeldtStatus {
    DELTATT,
    FRAVÆR_SYK,
    FRAVÆR_SYKT_BARN,
    FRAVÆR_ANNET,
    IKKE_REGISTRERT,
}
