package no.nav.tiltakspenger.saksbehandling.behandling.domene

import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.Revurderingsresultat

data class Revurderinger(
    val revurderinger: List<Revurdering>,
) : List<Revurdering> by revurderinger {

    fun harÅpenRevurderingSomKanFjerneRett(): Boolean {
        return revurderinger.any {
            !it.erAvsluttet && it.resultat !is Revurderingsresultat.Innvilgelse && it.resultat !is Revurderingsresultat.Forlengelse
        }
    }
}
