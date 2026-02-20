package no.nav.tiltakspenger.saksbehandling.person

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.personklient.pdl.dto.ForelderBarnRelasjon

interface PersonKlient {
    suspend fun hentEnkelPerson(fnr: Fnr): EnkelPerson
    suspend fun hentPersonSineForelderBarnRelasjoner(fnr: Fnr): List<ForelderBarnRelasjon>
    suspend fun hentPersonBolk(fnrs: List<Fnr>): List<EnkelPerson>
    suspend fun hentIdenter(aktorId: String): List<Personident>
}
