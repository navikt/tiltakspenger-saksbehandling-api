package no.nav.tiltakspenger.vedtak.clients.dokdist

import arrow.core.Either
import mu.KLogger
import no.nav.tiltakspenger.libs.json.deserialize
import no.nav.tiltakspenger.vedtak.distribusjon.domene.DistribusjonId
import no.nav.tiltakspenger.vedtak.distribusjon.ports.KunneIkkeDistribuereDokument

private data class DokdistResponse(
    val bestillingsId: String,
)

internal fun String.dokdistResponseToDomain(log: KLogger): Either<KunneIkkeDistribuereDokument, DistribusjonId> {
    return Either.catch { DistribusjonId(deserialize<DokdistResponse>(this).bestillingsId) }
        .mapLeft {
            log.error(it) { "Kunne ikke deserialisere respons fra dokdist. forventet 'bestillingsId'. jsonResponse: $this" }
            KunneIkkeDistribuereDokument
        }
}
