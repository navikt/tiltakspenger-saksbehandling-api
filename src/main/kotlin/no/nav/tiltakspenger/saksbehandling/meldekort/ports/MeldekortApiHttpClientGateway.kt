package no.nav.tiltakspenger.saksbehandling.meldekort.ports

import arrow.core.Either
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.Meldeperiode

interface MeldekortApiHttpClientGateway {
    suspend fun sendMeldeperiode(meldeperiode: Meldeperiode): Either<FeilVedSendingTilMeldekortApi, Unit>
}

data object FeilVedSendingTilMeldekortApi
