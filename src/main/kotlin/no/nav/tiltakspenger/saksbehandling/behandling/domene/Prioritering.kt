package no.nav.tiltakspenger.saksbehandling.behandling.domene

import no.nav.tiltakspenger.saksbehandling.felles.Begrunnelse

/**
 * Markerer at en rammebehandling, meldekortbehandling eller et meldekort fra bruker skal behandles foran de andre i benken.
 * Settes av saksbehandler (eller en automatisk behandlingsjobb) som en del av behandlingsskjemaet.
 * Korrigerte meldekort fra bruker prioriteres automatisk ved mottak, se [no.nav.tiltakspenger.saksbehandling.meldekort.domene.brukersmeldekort.utledPrioriteringForBrukersMeldekort].
 * Begrunnelsen forklarer hvorfor behandlingen er høyt prioritert, og er saksbehandlers fritekst.
 */
data class Prioritering(
    val begrunnelse: Begrunnelse,
) {
    companion object {
        /** @return null dersom begrunnelsen er blank */
        fun create(begrunnelse: String): Prioritering? = Begrunnelse.create(begrunnelse)?.let { Prioritering(it) }
    }
}
