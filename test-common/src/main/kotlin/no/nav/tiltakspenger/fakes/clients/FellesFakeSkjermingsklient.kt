package no.nav.tiltakspenger.fakes.clients

import arrow.atomic.Atomic
import arrow.core.Either
import arrow.core.NonEmptyList
import arrow.core.right
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.personklient.pdl.FellesSkjermingError
import no.nav.tiltakspenger.libs.personklient.skjerming.FellesSkjermingsklient
import java.lang.IllegalStateException

class FellesFakeSkjermingsklient : FellesSkjermingsklient {
    private val data = Atomic(mutableMapOf<Fnr, Boolean>())

    override suspend fun erSkjermetPerson(
        fnr: Fnr,
        correlationId: CorrelationId,
    ): Either<FellesSkjermingError, Boolean> {
        return data.get()[fnr]!!.right()
    }

    override suspend fun erSkjermetPersoner(
        fnrListe: NonEmptyList<Fnr>,
        correlationId: CorrelationId,
    ): Either<FellesSkjermingError, Map<Fnr, Boolean>> {
        return fnrListe.map { fnr ->
            fnr to (
                data.get()[fnr]
                    ?: throw IllegalStateException("FellesFakeSkjermingsklient: Prøvde slå opp skjerming for ukjent fnr: ${fnr.verdi}. datagrunnlag i fake: ${data.get().keys.map { it.verdi }}")
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
