package no.nav.tiltakspenger.saksbehandling.vedtak

import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.Søknadsbehandlingsresultat
import kotlin.collections.plus

fun List<Rammevedtak>.leggTil(vedtak: Rammevedtak): List<Rammevedtak> {
    if (vedtak.rammebehandlingsresultat is Søknadsbehandlingsresultat.Avslag) {
        // Avslag omgjør aldri noe
        return this.plus(vedtak)
    }
    val oppdatertVedtaksliste = this.map {
        it.oppdaterOmgjortAvRammevedtak(vedtak)
    }
    return oppdatertVedtaksliste.plus(vedtak)
}
