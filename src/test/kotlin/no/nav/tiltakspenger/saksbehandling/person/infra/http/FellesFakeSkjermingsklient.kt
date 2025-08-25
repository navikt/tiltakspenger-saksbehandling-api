@file:Suppress("UnusedImport")

package no.nav.tiltakspenger.saksbehandling.person.infra.http

import arrow.atomic.Atomic
import arrow.core.Either
import arrow.core.NonEmptyList
import arrow.core.right
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.personklient.pdl.FellesSkjermingError
import no.nav.tiltakspenger.libs.personklient.skjerming.FellesSkjermingsklient

class FellesFakeSkjermingsklient : FellesSkjermingsklient {
    private val data = Atomic(mutableMapOf<Fnr, Boolean>())

    override suspend fun erSkjermetPerson(
        fnr: Fnr,
        correlationId: CorrelationId,
    ): Either<FellesSkjermingError, Boolean> {
        return (data.get()[fnr] ?: false).right()
    }

    override suspend fun erSkjermetPersoner(
        fnrListe: NonEmptyList<Fnr>,
        correlationId: CorrelationId,
    ): Either<FellesSkjermingError, Map<Fnr, Boolean>> {
        return fnrListe.map { fnr ->
            fnr to (
                data.get()[fnr]
                    ?: false
                )
        }.toMap().right()
    }

    fun leggTil(
        fnr: Fnr,
        skjermet: Boolean,
    ) {
        data.get()[fnr] = skjermet
    }
}
