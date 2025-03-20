package no.nav.tiltakspenger.saksbehandling.distribusjon.infra

import arrow.core.Either
import io.github.oshai.kotlinlogging.KLogger
import no.nav.tiltakspenger.libs.json.deserialize
import no.nav.tiltakspenger.saksbehandling.distribusjon.DistribusjonId
import no.nav.tiltakspenger.saksbehandling.distribusjon.KunneIkkeDistribuereDokument

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
