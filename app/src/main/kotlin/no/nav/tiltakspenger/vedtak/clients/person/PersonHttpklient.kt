package no.nav.tiltakspenger.vedtak.clients.person

import arrow.core.getOrElse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import no.nav.tiltakspenger.libs.common.AccessToken
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.json.objectMapper
import no.nav.tiltakspenger.libs.personklient.pdl.FellesPersonklient
import no.nav.tiltakspenger.vedtak.felles.sikkerlogg
import no.nav.tiltakspenger.vedtak.saksbehandling.domene.personopplysninger.EnkelPerson
import no.nav.tiltakspenger.vedtak.saksbehandling.domene.personopplysninger.PersonopplysningerSøker
import no.nav.tiltakspenger.vedtak.saksbehandling.ports.PersonGateway

class PersonHttpklient(
    endepunkt: String,
    private val getToken: suspend () -> AccessToken,
) : PersonGateway {
    private val personklient =
        FellesPersonklient.create(
            endepunkt = endepunkt,
            sikkerlogg = sikkerlogg,
        )

    /**
     * Kommentar jah: Dersom vi ønsker og sende saksbehandler sitt OBO-token, kan vi lage en egen metode for dette.
     */
    override suspend fun hentPerson(fnr: Fnr): PersonopplysningerSøker {
        return withContext(Dispatchers.IO) {
            val body = objectMapper.writeValueAsString(hentPersonQuery(fnr))
            personklient
                .hentPerson(fnr, getToken(), body)
                .map { mapPersonopplysninger(it, fnr) }
                .getOrElse { it.mapError() }
        }
    }

    override suspend fun hentEnkelPerson(fnr: Fnr): EnkelPerson {
        return withContext(Dispatchers.IO) {
            val body = objectMapper.writeValueAsString(hentEnkelPersonQuery(fnr))
            personklient.hentPerson(fnr, getToken(), body).map { it.toEnkelPerson(fnr) }.getOrElse { it.mapError() }
        }
    }
}
