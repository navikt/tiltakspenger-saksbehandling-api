package no.nav.tiltakspenger.saksbehandling.beregning

import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.MeldekortId

sealed interface BeregningKilde {

    /** @param id Id for meldekort-behandlingen som utløste denne beregningen. Denne kan være ulik [MeldeperiodeBeregning.meldekortId] for beregninger som er et resultat av en korrigering som påvirket en påfølgende meldeperiode.
     * */
    data class Meldekort(val id: MeldekortId) : BeregningKilde

    /** @param id Id for behandlingen/revurderingen som utløste denne beregningen.
     * */
    data class Behandling(val id: BehandlingId) : BeregningKilde
}
