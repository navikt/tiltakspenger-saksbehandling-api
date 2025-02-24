package no.nav.tiltakspenger.saksbehandling.domene.vedtak

import no.nav.tiltakspenger.libs.periodisering.Periodisering
import no.nav.tiltakspenger.saksbehandling.domene.sak.Sak
import no.nav.tiltakspenger.saksbehandling.domene.vilkår.Utfallsperiode

fun Sak.utfallsperioder(): Periodisering<Utfallsperiode> {
    return vedtaksliste.utfallsperioder
}
