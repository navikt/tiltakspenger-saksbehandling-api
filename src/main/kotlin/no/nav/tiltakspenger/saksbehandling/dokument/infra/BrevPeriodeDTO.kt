package no.nav.tiltakspenger.saksbehandling.dokument.infra

import no.nav.tiltakspenger.libs.dato.norskDatoFormatter
import no.nav.tiltakspenger.libs.periode.Periode

// Periode med datoer formattert for tekst i brev
data class BrevPeriodeDTO private constructor(val fraOgMed: String, val tilOgMed: String) {

    companion object {
        fun fraPeriode(periode: Periode): BrevPeriodeDTO {
            return BrevPeriodeDTO(
                fraOgMed = periode.fraOgMed.format(norskDatoFormatter),
                tilOgMed = periode.tilOgMed.format(norskDatoFormatter),
            )
        }
    }
}
