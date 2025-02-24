package no.nav.tiltakspenger.saksbehandling.domene.vedtak

import no.nav.tiltakspenger.libs.periodisering.Periodisering
import no.nav.tiltakspenger.saksbehandling.domene.sak.Sak
import no.nav.tiltakspenger.saksbehandling.domene.vilkår.AvklartUtfallForPeriode

fun Sak.utfallsperioder(): Periodisering<AvklartUtfallForPeriode> {
    return vedtaksliste.utfallsperioder
}
