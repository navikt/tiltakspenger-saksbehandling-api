package no.nav.tiltakspenger.saksbehandling.person

import no.nav.tiltakspenger.libs.common.Fnr

interface PersonKlient {
    suspend fun hentPerson(fnr: Fnr): PersonopplysningerSøker
    suspend fun hentEnkelPerson(fnr: Fnr): EnkelPerson
}
