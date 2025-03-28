package no.nav.tiltakspenger.saksbehandling.sak

import no.nav.tiltakspenger.libs.periodisering.Periodisering
import no.nav.tiltakspenger.saksbehandling.felles.Utfallsperiode

fun Sak.utfallsperioder(): Periodisering<Utfallsperiode?> {
    return vedtaksliste.utfallsperioder
}
