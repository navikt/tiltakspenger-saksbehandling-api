package no.nav.tiltakspenger.meldekort.ports

import arrow.core.Either
import no.nav.tiltakspenger.meldekort.domene.Meldeperiode

interface MeldekortApiHttpClientGateway {
    suspend fun sendMeldeperiode(meldeperiode: Meldeperiode): Either<FeilVedSendingTilMeldekortApi, Unit>
}

data object FeilVedSendingTilMeldekortApi
