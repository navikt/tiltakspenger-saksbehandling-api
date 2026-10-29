package no.nav.tiltakspenger.saksbehandling.dokument.infra

import no.nav.tiltakspenger.libs.periode.Periode
import java.time.LocalDate

/** pdfgenrs formaterer datoene. */
data class BrevPeriodeDTO private constructor(val fraOgMed: LocalDate, val tilOgMed: LocalDate) {

    companion object {
        fun fraPeriode(periode: Periode): BrevPeriodeDTO {
            return BrevPeriodeDTO(
                fraOgMed = periode.fraOgMed,
                tilOgMed = periode.tilOgMed,
            )
        }
    }
}
