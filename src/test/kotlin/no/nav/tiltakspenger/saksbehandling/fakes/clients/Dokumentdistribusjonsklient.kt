package no.nav.tiltakspenger.saksbehandling.fakes.clients

import arrow.core.Either
import arrow.core.right
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.saksbehandling.common.DistribusjonIdGenerator
import no.nav.tiltakspenger.saksbehandling.distribusjon.DistribusjonId
import no.nav.tiltakspenger.saksbehandling.distribusjon.Dokumentdistribusjonsklient
import no.nav.tiltakspenger.saksbehandling.distribusjon.KunneIkkeDistribuereDokument
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId
import java.util.concurrent.ConcurrentHashMap

class Dokumentdistribusjonsklient(
    private val distribusjonIdGenerator: DistribusjonIdGenerator,
) : Dokumentdistribusjonsklient {

    private val data = ConcurrentHashMap<JournalpostId, DistribusjonId>()

    override suspend fun distribuerDokument(
        journalpostId: JournalpostId,
        correlationId: CorrelationId,
    ): Either<KunneIkkeDistribuereDokument, DistribusjonId> {
        return data.computeIfAbsent(journalpostId) { distribusjonIdGenerator.neste() }.right()
    }
}
