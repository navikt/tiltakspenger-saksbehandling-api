package no.nav.tiltakspenger.saksbehandling.behandling.domene.forlengelse

import no.nav.tiltakspenger.libs.common.VedtakId
import java.time.LocalDate

/**
 * Rammevedtaket en forlengelse forlenger.
 *
 * @param forlengesFraOgMed Dagen etter siste innvilgede dag i [rammevedtakId].
 * Forlengelsen innvilger alltid fra og med denne dagen.
 */
data class ForlengerRammevedtak(
    val rammevedtakId: VedtakId,
    val forlengesFraOgMed: LocalDate,
) {
    val sisteInnvilgedeDag: LocalDate = forlengesFraOgMed.minusDays(1)
}
