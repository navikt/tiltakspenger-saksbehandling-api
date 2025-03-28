package no.nav.tiltakspenger.saksbehandling.behandling.domene.vedtak

import no.nav.tiltakspenger.libs.periodisering.Periodisering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.vilkår.Utfallsperiode
import no.nav.tiltakspenger.saksbehandling.sak.Sak

fun Sak.utfallsperioder(): Periodisering<Utfallsperiode?> {
    return vedtaksliste.utfallsperioder
}
