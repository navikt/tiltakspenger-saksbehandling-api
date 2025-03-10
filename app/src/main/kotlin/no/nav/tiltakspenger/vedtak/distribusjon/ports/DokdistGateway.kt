package no.nav.tiltakspenger.vedtak.distribusjon.ports

import arrow.core.Either
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.vedtak.distribusjon.domene.DistribusjonId
import no.nav.tiltakspenger.vedtak.felles.journalføring.JournalpostId

interface DokdistGateway {
    suspend fun distribuerDokument(
        journalpostId: JournalpostId,
        correlationId: CorrelationId,
    ): Either<KunneIkkeDistribuereDokument, DistribusjonId>
}

object KunneIkkeDistribuereDokument
