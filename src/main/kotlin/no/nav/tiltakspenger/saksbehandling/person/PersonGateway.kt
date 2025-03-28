package no.nav.tiltakspenger.saksbehandling.person

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.saksbehandling.person.EnkelPerson
import no.nav.tiltakspenger.saksbehandling.person.PersonopplysningerSøker

interface PersonGateway {
    suspend fun hentPerson(fnr: Fnr): PersonopplysningerSøker
    suspend fun hentEnkelPerson(fnr: Fnr): EnkelPerson
}
