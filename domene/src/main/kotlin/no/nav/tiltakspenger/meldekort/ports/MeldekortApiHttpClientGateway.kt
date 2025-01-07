package no.nav.tiltakspenger.meldekort.ports

import arrow.core.Either
import no.nav.tiltakspenger.meldekort.domene.MeldekortBehandling

interface MeldekortApiHttpClientGateway {
    suspend fun sendMeldekort(meldekort: MeldekortBehandling): Either<FeilVedSendingTilMeldekortApi, Unit>
}

data object FeilVedSendingTilMeldekortApi
