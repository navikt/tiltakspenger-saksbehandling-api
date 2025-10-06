package no.nav.tiltakspenger.saksbehandling.person.infra.http

import arrow.core.getOrElse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import no.nav.tiltakspenger.libs.common.AccessToken
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.json.objectMapper
import no.nav.tiltakspenger.libs.personklient.pdl.FellesPersonklient
import no.nav.tiltakspenger.saksbehandling.person.EnkelPerson
import no.nav.tiltakspenger.saksbehandling.person.PersonKlient

class PersonHttpklient(
    endepunkt: String,
    private val getToken: suspend () -> AccessToken,
) : PersonKlient {
    private val personklient =
        FellesPersonklient.create(
            endepunkt = endepunkt,
        )

    /**
     * Kommentar jah: Dersom vi ønsker og sende saksbehandler sitt OBO-token, kan vi lage en egen metode for dette.
     */
    override suspend fun hentEnkelPerson(fnr: Fnr): EnkelPerson {
        return withContext(Dispatchers.IO) {
            val body = objectMapper.writeValueAsString(hentEnkelPersonQuery(fnr))
            personklient.hentPerson(fnr, getToken(), body)
                .map { it.toEnkelPerson(fnr) }
                .getOrElse { it.mapError() }
        }
    }
}
