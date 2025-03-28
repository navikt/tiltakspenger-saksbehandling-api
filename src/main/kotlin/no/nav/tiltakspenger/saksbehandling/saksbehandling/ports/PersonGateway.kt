package no.nav.tiltakspenger.saksbehandling.saksbehandling.ports

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.personopplysninger.EnkelPerson
import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.personopplysninger.PersonopplysningerSøker

interface PersonGateway {
    suspend fun hentPerson(fnr: Fnr): PersonopplysningerSøker
    suspend fun hentEnkelPerson(fnr: Fnr): EnkelPerson
}
