package no.nav.tiltakspenger.vedtak.saksbehandling.domene.vedtak

import no.nav.tiltakspenger.libs.periodisering.Periodisering
import no.nav.tiltakspenger.vedtak.saksbehandling.domene.sak.Sak
import no.nav.tiltakspenger.vedtak.saksbehandling.domene.vilkår.Utfallsperiode

fun Sak.utfallsperioder(): Periodisering<Utfallsperiode> {
    return vedtaksliste.utfallsperioder
}
