package no.nav.tiltakspenger.saksbehandling.distribusjon

import arrow.core.Either
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId

interface DokdistGateway {
    suspend fun distribuerDokument(
        journalpostId: JournalpostId,
        correlationId: CorrelationId,
    ): Either<KunneIkkeDistribuereDokument, DistribusjonId>
}

object KunneIkkeDistribuereDokument
