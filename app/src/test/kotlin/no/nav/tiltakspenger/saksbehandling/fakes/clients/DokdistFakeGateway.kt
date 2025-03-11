package no.nav.tiltakspenger.saksbehandling.fakes.clients

import arrow.core.Either
import arrow.core.right
import no.nav.tiltakspenger.common.DistribusjonIdGenerator
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.saksbehandling.distribusjon.domene.DistribusjonId
import no.nav.tiltakspenger.saksbehandling.distribusjon.ports.DokdistGateway
import no.nav.tiltakspenger.saksbehandling.distribusjon.ports.KunneIkkeDistribuereDokument
import no.nav.tiltakspenger.saksbehandling.felles.journalføring.JournalpostId
import java.util.concurrent.ConcurrentHashMap

class DokdistFakeGateway(
    private val distribusjonIdGenerator: DistribusjonIdGenerator,
) : DokdistGateway {

    private val data = ConcurrentHashMap<JournalpostId, DistribusjonId>()

    override suspend fun distribuerDokument(
        journalpostId: JournalpostId,
        correlationId: CorrelationId,
    ): Either<KunneIkkeDistribuereDokument, DistribusjonId> {
        return data.computeIfAbsent(journalpostId) { distribusjonIdGenerator.neste() }.right()
    }
}
