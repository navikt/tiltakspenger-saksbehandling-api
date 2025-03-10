package no.nav.tiltakspenger.vedtak.meldekort.ports

import arrow.core.Either
import no.nav.tiltakspenger.vedtak.meldekort.domene.Meldeperiode

interface MeldekortApiHttpClientGateway {
    suspend fun sendMeldeperiode(meldeperiode: Meldeperiode): Either<FeilVedSendingTilMeldekortApi, Unit>
}

data object FeilVedSendingTilMeldekortApi
