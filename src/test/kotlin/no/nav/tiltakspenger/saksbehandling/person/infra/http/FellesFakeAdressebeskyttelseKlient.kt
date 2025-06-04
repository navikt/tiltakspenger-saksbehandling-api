package no.nav.tiltakspenger.saksbehandling.person.infra.http

import arrow.core.Either
import arrow.core.right
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.person.AdressebeskyttelseGradering
import no.nav.tiltakspenger.libs.personklient.pdl.FellesAdressebeskyttelseError
import no.nav.tiltakspenger.libs.personklient.pdl.adressebeskyttelse.FellesAdressebeskyttelseKlient

class FellesFakeAdressebeskyttelseKlient : FellesAdressebeskyttelseKlient {
    private val data = arrow.atomic.Atomic(mutableMapOf<Fnr, List<AdressebeskyttelseGradering>>())

    override suspend fun enkel(fnr: Fnr): Either<FellesAdressebeskyttelseError, List<AdressebeskyttelseGradering>?> =
        (data.get()[fnr] ?: listOf(AdressebeskyttelseGradering.UGRADERT)).right()

    override suspend fun bolk(fnrListe: List<Fnr>): Either<FellesAdressebeskyttelseError, Map<Fnr, List<AdressebeskyttelseGradering>?>> =
        fnrListe.associateWith { data.get()[it] ?: listOf(AdressebeskyttelseGradering.UGRADERT) }.right()

    fun leggTil(
        fnr: Fnr,
        gradering: List<AdressebeskyttelseGradering>,
    ) {
        data.get()[fnr] = gradering
    }
}
