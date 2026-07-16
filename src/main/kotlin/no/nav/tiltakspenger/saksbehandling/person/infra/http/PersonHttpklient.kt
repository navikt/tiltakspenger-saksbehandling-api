package no.nav.tiltakspenger.saksbehandling.person.infra.http

import arrow.core.getOrElse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import no.nav.tiltakspenger.libs.common.AccessToken
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.json.objectMapper
import no.nav.tiltakspenger.libs.personklient.pdl.FellesPersonklient
import no.nav.tiltakspenger.libs.personklient.pdl.GraphqlBolkQuery
import no.nav.tiltakspenger.libs.personklient.pdl.GraphqlQuery
import no.nav.tiltakspenger.libs.personklient.pdl.dto.ForelderBarnRelasjon
import no.nav.tiltakspenger.saksbehandling.person.EnkelPerson
import no.nav.tiltakspenger.saksbehandling.person.PersonKlient
import no.nav.tiltakspenger.saksbehandling.person.Personident
import java.time.Clock

/**
 * Klient mot PDL (persondataløsningen) for å hente personopplysninger, via personklienten i tiltakspenger-libs.
 *
 * Kildekode: https://github.com/navikt/pdl
 * Dokumentasjon: https://pdl-docs.ansatt.nav.no/ og https://pdl-docs.ansatt.nav.no/intern/index.html
 * API-spec: https://github.com/navikt/pdl/blob/15bdc571f0357f97f524dc496fb16217ff4aa94d/apps/api/src/main/resources/schemas/pdl.graphqls#L17 og https://pdl-playground.dev.intern.nav.no/ og https://pdl-pip-api.intern.dev.nav.no/swagger-ui/index.html (Swagger)
 * Slack: #pdl
 * Teamkatalog: https://teamkatalogen.nav.no/team/034cbcd2-ac28-4e2e-88c8-345945933f70
 */
class PersonHttpklient(
    endepunkt: String,
    clock: Clock,
    private val getToken: suspend () -> AccessToken,
) : PersonKlient {
    private val personklient =
        FellesPersonklient.create(
            endepunkt = endepunkt,
            clock = clock,
        )

    /**
     * Kommentar jah: Dersom vi ønsker og sende saksbehandler sitt OBO-token, kan vi lage en egen metode for dette.
     */
    override suspend fun hentEnkelPerson(fnr: Fnr): EnkelPerson {
        return withContext(Dispatchers.IO) {
            personklient.graphqlRequest(
                token = getToken(),
                jsonRequestBody = objectMapper.writeValueAsString(hentPersonQuery(fnr)),
            )
                .map { it.toEnkelPerson(fnr) }
                .getOrElse { it.mapError() }
        }
    }

    override suspend fun hentPersonSineForelderBarnRelasjoner(fnr: Fnr): List<ForelderBarnRelasjon> {
        return withContext(Dispatchers.IO) {
            personklient.graphqlRequest(
                token = getToken(),
                jsonRequestBody = objectMapper.writeValueAsString(hentForelderBarnRelasjon(fnr)),
            )
                .map { it.toForelderBarnRelasjon(fnr) }
                .getOrElse { it.mapError() }
        }
    }

    override suspend fun hentPersonBolk(fnrs: List<Fnr>): List<EnkelPerson> {
        return withContext(Dispatchers.IO) {
            personklient.graphqlRequest(
                token = getToken(),
                jsonRequestBody = objectMapper.writeValueAsString(hentPersonBolkQuery(fnrs)),
            )
                .map { it.toEnkelPersonBolk(fnrs) }
                .getOrElse { it.mapError() }
        }
    }

    override suspend fun hentIdenter(aktorId: String): List<Personident> {
        return withContext(Dispatchers.IO) {
            personklient.graphqlRequest(
                token = getToken(),
                jsonRequestBody = objectMapper.writeValueAsString(hentIdenterQuery(aktorId)),
            )
                .map { it.toPersonidenter(aktorId) }
                .getOrElse { it.mapError() }
        }
    }

    /**
     * Query for å hente person fra PersonDataLøsningen (PDL)
     */
    private fun hentPersonQuery(fnr: Fnr): GraphqlQuery {
        return GraphqlQuery(
            query = getResource("/pdl/hentPerson.graphql"),
            variables = mapOf("ident" to fnr.verdi),
        )
    }

    /**
     * Query for å hente informasjon om en forelder/barn relasjoner for person fra PersonDataLøsningen (PDL)
     */
    private fun hentForelderBarnRelasjon(fnr: Fnr): GraphqlQuery {
        return GraphqlQuery(
            query = getResource("/pdl/hentPersonForelderBarnRelasjon.graphql"),
            variables = mapOf("ident" to fnr.verdi),
        )
    }

    /**
     * Query for å hente informasjon om en forelder/barn relasjoner for person fra PersonDataLøsningen (PDL)
     */
    private fun hentPersonBolkQuery(fnrs: List<Fnr>): GraphqlBolkQuery {
        return GraphqlBolkQuery(
            query = getResource("/pdl/hentPersonBolk.graphql"),
            variables = mapOf("identer" to fnrs.map { it.verdi }),
        )
    }

    private fun hentIdenterQuery(aktorId: String): GraphqlQuery {
        return GraphqlQuery(
            query = getResource("/pdl/hentIdenter.graphql"),
            variables = mapOf("ident" to aktorId),
        )
    }

    private fun getResource(path: String): String {
        return requireNotNull(PersonHttpklient::class.java.getResource(path)).readText()
    }
}
