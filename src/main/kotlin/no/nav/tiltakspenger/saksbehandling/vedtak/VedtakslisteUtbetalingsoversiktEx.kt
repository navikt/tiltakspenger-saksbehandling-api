package no.nav.tiltakspenger.saksbehandling.vedtak

import no.nav.tiltakspenger.libs.periode.Periode

/** Perioden fra første til siste dag sakens vedtak gjelder, uten avslag, eller null når saken ikke har slike vedtak. */
fun Vedtaksliste.periodeForUtbetalingsoversikt(): Periode? {
    val perioder = rammevedtaksliste.vedtaksperioder + meldekortvedtaksliste.map { it.periode }
    if (perioder.isEmpty()) return null
    return Periode(
        fraOgMed = perioder.minOf { it.fraOgMed },
        tilOgMed = perioder.maxOf { it.tilOgMed },
    )
}
