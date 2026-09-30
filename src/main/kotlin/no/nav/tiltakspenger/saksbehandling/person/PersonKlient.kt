package no.nav.tiltakspenger.saksbehandling.person

import arrow.core.Either
import no.nav.tiltakspenger.libs.common.personopplysning.Fnr
import no.nav.tiltakspenger.libs.personklient.pdl.dto.ForelderBarnRelasjon

interface PersonKlient {
    suspend fun hentEnkelPerson(fnr: Fnr): EnkelPerson

    suspend fun hentPersonSineForelderBarnRelasjoner(fnr: Fnr): List<ForelderBarnRelasjon>

    suspend fun hentPersonBolk(fnrs: List<Fnr>): List<EnkelPerson>

    /**
     * Henter bare adressebeskyttelsen til [fnrs], uten andre personopplysninger.
     * Personer PDL ikke finner, eller der graderingen ikke kan avklares, er utelatt fra svaret.
     * Metoden kaster ikke og logger ikke; feilen logges én gang av route-laget.
     */
    suspend fun hentAdressebeskyttelse(
        fnrs: List<Fnr>,
    ): Either<KunneIkkeHenteAdressebeskyttelseEllerSkjerming.FeilVedKallMotPdl, Map<Fnr, Adressebeskyttelse>>

    suspend fun hentIdenter(aktorId: String): List<Personident>
}
