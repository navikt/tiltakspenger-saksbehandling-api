package no.nav.tiltakspenger.saksbehandling.person.infra.http

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.getOrElse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import no.nav.tiltakspenger.libs.common.AccessToken
import no.nav.tiltakspenger.libs.common.personopplysning.Fnr
import no.nav.tiltakspenger.libs.json.objectMapper
import no.nav.tiltakspenger.libs.personklient.pdl.FellesPersonklient
import no.nav.tiltakspenger.libs.personklient.pdl.FellesPersonklientError
import no.nav.tiltakspenger.libs.personklient.pdl.GraphqlBolkQuery
import no.nav.tiltakspenger.libs.personklient.pdl.GraphqlQuery
import no.nav.tiltakspenger.libs.personklient.pdl.dto.ForelderBarnRelasjon
import no.nav.tiltakspenger.saksbehandling.felles.Loggkontekst
import no.nav.tiltakspenger.saksbehandling.felles.loggkontekst
import no.nav.tiltakspenger.saksbehandling.felles.sikkerloggkontekst
import no.nav.tiltakspenger.saksbehandling.person.Adressebeskyttelse
import no.nav.tiltakspenger.saksbehandling.person.EnkelPerson
import no.nav.tiltakspenger.saksbehandling.person.KunneIkkeHenteAdressebeskyttelseEllerSkjerming
import no.nav.tiltakspenger.saksbehandling.person.PersonKlient
import no.nav.tiltakspenger.saksbehandling.person.Personident
import org.intellij.lang.annotations.Language
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

    override suspend fun hentAdressebeskyttelse(
        fnrs: List<Fnr>,
    ): Either<KunneIkkeHenteAdressebeskyttelseEllerSkjerming.FeilVedKallMotPdl, Map<Fnr, Adressebeskyttelse>> {
        return withContext(Dispatchers.IO) {
            personklient.graphqlRequest(
                token = getToken(),
                jsonRequestBody = objectMapper.writeValueAsString(
                    GraphqlBolkQuery(
                        query = HENT_ADRESSEBESKYTTELSE_BOLK,
                        variables = mapOf("identer" to fnrs.map { it.verdi }),
                    ),
                ),
            )
                .mapLeft { it.tilFeilVedKallMotPdl(fnrs.size) }
                .flatMap { respons ->
                    Either.catch { respons.toAdressebeskyttelseBolk() }.mapLeft {
                        KunneIkkeHenteAdressebeskyttelseEllerSkjerming.FeilVedKallMotPdl(
                            loggkontekst = Loggkontekst("Feil ved $OPERASJON_ADRESSEBESKYTTELSE. Kunne ikke lese svaret for ${fnrs.size} personer"),
                            sikkerloggkontekst = Loggkontekst("Feil ved $OPERASJON_ADRESSEBESKYTTELSE. Respons: $respons", it),
                        )
                    }
                }
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

/**
 * Henter bare adressebeskyttelsen, med metadataene `avklarGradering` trenger for å velge gradering.
 */
@Language("GraphQL")
private const val HENT_ADRESSEBESKYTTELSE_BOLK = """
query(${'$'}identer: [ID!]!) {
    hentPersonBolk(identer: ${'$'}identer) {
        ident
        person {
            adressebeskyttelse(historikk: false) {
                gradering
                folkeregistermetadata { ajourholdstidspunkt }
                metadata {
                    endringer { kilde registrert registrertAv systemkilde type }
                    master
                }
            }
        }
        code
    }
}
"""

private const val OPERASJON_ADRESSEBESKYTTELSE = "PDL-oppslag av adressebeskyttelse"

/**
 * Kallfeil bærer HTTP-konteksten fra klienten.
 * De øvrige feilene er utledet av et svar vi forsto, og navngis med typen; innholdet kan bære identer og går bare til sikkerlogg.
 */
private fun FellesPersonklientError.tilFeilVedKallMotPdl(antall: Int) = when (this) {
    is FellesPersonklientError.Kallfeil -> KunneIkkeHenteAdressebeskyttelseEllerSkjerming.FeilVedKallMotPdl(
        loggkontekst = httpKlientError.loggkontekst("$OPERASJON_ADRESSEBESKYTTELSE for $antall personer"),
        sikkerloggkontekst = httpKlientError.sikkerloggkontekst(OPERASJON_ADRESSEBESKYTTELSE),
    )

    else -> KunneIkkeHenteAdressebeskyttelseEllerSkjerming.FeilVedKallMotPdl(
        loggkontekst = Loggkontekst("Feil ved $OPERASJON_ADRESSEBESKYTTELSE for $antall personer: ${this::class.simpleName}"),
        sikkerloggkontekst = Loggkontekst("Feil ved $OPERASJON_ADRESSEBESKYTTELSE: $this"),
    )
}
