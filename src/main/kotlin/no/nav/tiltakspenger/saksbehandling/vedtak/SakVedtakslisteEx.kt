package no.nav.tiltakspenger.saksbehandling.vedtak

import no.nav.tiltakspenger.saksbehandling.sak.Sak
import java.time.LocalDate

fun Sak.harInnvilgetTiltakspengerPåDato(dato: LocalDate): Boolean {
    return vedtaksliste.harInnvilgetTiltakspengerPåDato(dato)
}

fun Sak.harInnvilgetTiltakspengerEtterDato(dato: LocalDate): Boolean {
    return vedtaksliste.harInnvilgetTiltakspengerEtterDato(dato)
}
