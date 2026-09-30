@file:Suppress("UnusedImport")

package no.nav.tiltakspenger.saksbehandling.person.infra.http

import arrow.atomic.Atomic
import arrow.core.Either
import arrow.core.NonEmptyList
import arrow.core.left
import arrow.core.right
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.personopplysning.Fnr
import no.nav.tiltakspenger.libs.personklient.pdl.FellesSkjermingError
import no.nav.tiltakspenger.libs.personklient.skjerming.FellesSkjermingsklient
import no.nav.tiltakspenger.saksbehandling.felles.uventetStatus

class FellesFakeSkjermingsklient : FellesSkjermingsklient {
    private val data = Atomic(mutableMapOf<Fnr, Boolean>())
    private val feiler = Atomic(false)

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
        if (feiler.get()) return FellesSkjermingError.Ikke2xx(uventetStatus(uri = "http://skjerming.test/skjermetBulk")).left()
        return fnrListe.map { fnr ->
            fnr to (
                data.get()[fnr]
                    ?: false
                )
        }.toMap().right()
    }

    /** Alle bolkoppslag etter dette svarer med en serverfeil. */
    fun feilVedBolkoppslag() {
        feiler.set(true)
    }

    fun leggTil(
        fnr: Fnr,
        skjermet: Boolean,
    ) {
        data.get()[fnr] = skjermet
    }
}
