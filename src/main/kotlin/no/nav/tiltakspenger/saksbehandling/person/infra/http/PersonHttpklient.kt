package no.nav.tiltakspenger.saksbehandling.person.infra.http

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.getOrElse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import no.nav.tiltakspenger.libs.common.AccessToken
import no.nav.tiltakspenger.libs.common.personopplysning.Fnr
import no.nav.tiltakspenger.libs.json.objectMapper
import no.nav.tiltakspenger.libs.personklient.infra.pdl.FellesPersonklient
import no.nav.tiltakspenger.libs.personklient.infra.pdl.FellesPersonklientError
import no.nav.tiltakspenger.libs.personklient.infra.pdl.GraphqlBolkQuery
import no.nav.tiltakspenger.libs.personklient.infra.pdl.GraphqlQuery
import no.nav.tiltakspenger.libs.personklient.infra.pdl.dto.ForelderBarnRelasjon
import no.nav.tiltakspenger.saksbehandling.felles.Loggkontekst
import no.nav.tiltakspenger.saksbehandling.felles.loggkontekst
import no.nav.tiltakspenger.saksbehandling.felles.sikkerloggkontekst
import no.nav.tiltakspenger.saksbehandling.person.Adressebeskyttelse
import no.nav.tiltakspenger.saksbehandling.person.EnkelPerson
import no.nav.tiltakspenger.saksbehandling.person.Fødselsnumre
import no.nav.tiltakspenger.saksbehandling.person.KunneIkkeHenteAdressebeskyttelseEllerSkjerming
import no.nav.tiltakspenger.saksbehandling.person.KunneIkkeHenteFødselsnumre
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
    private val personklient = FellesPersonklient.create(
        endepunkt = endepunkt,
        clock = clock,
    )

    /**
     * Kommentar jah: Dersom vi ønsker og sende saksbehandler sitt OBO-token, kan vi lage en egen metode for dette.
     */
    override suspend fun hentEnkelPerson(fnr: Fnr): EnkelPerson = spør(HENT_PERSON, fnr).map { it.toEnkelPerson(fnr) }.getOrElse { it.mapError() }

    override suspend fun hentPersonSineForelderBarnRelasjoner(fnr: Fnr): List<ForelderBarnRelasjon> = spør(HENT_FORELDER_BARN_RELASJON, fnr).map { it.toForelderBarnRelasjon(fnr) }.getOrElse { it.mapError() }

    override suspend fun hentPersonBolk(fnrs: List<Fnr>): List<EnkelPerson> = spørBolk(HENT_PERSON_BOLK, fnrs).map { it.toEnkelPersonBolk(fnrs) }.getOrElse { it.mapError() }

    override suspend fun hentAdressebeskyttelse(
        fnrs: List<Fnr>,
    ): Either<KunneIkkeHenteAdressebeskyttelseEllerSkjerming.FeilVedKallMotPdl, Map<Fnr, Adressebeskyttelse>> {
        return spørBolk(HENT_ADRESSEBESKYTTELSE_BOLK, fnrs).mapLeft {
            it.tilPdlfeil(
                operasjon = OPERASJON_ADRESSEBESKYTTELSE,
                omfang = " for ${fnrs.size} personer",
                lagFeil = KunneIkkeHenteAdressebeskyttelseEllerSkjerming::FeilVedKallMotPdl,
            )
        }.flatMap { respons ->
            Either.catch { respons.toAdressebeskyttelseBolk() }.mapLeft {
                KunneIkkeHenteAdressebeskyttelseEllerSkjerming.FeilVedKallMotPdl(
                    loggkontekst = Loggkontekst("Feil ved $OPERASJON_ADRESSEBESKYTTELSE. Kunne ikke lese svaret for ${fnrs.size} personer"),
                    sikkerloggkontekst = Loggkontekst("Feil ved $OPERASJON_ADRESSEBESKYTTELSE. Respons: $respons", it),
                )
            }
        }
    }

    override suspend fun hentIdenter(aktorId: String): List<Personident> = spør(HENT_IDENTER, aktorId).map { it.toPersonidenter(aktorId) }.getOrElse { it.mapError() }

    override suspend fun hentNåværendeOgHistoriskeFnr(fnr: Fnr): Either<KunneIkkeHenteFødselsnumre, Fødselsnumre> {
        return spør(HENT_IDENTER_MED_HISTORIKK, fnr).mapLeft {
            it.tilPdlfeil(operasjon = OPERASJON_FØDSELSNUMRE, omfang = "", lagFeil = ::KunneIkkeHenteFødselsnumre)
        }.flatMap { respons ->
            Either.catch { respons.toFødselsnumre(fnr) }.mapLeft {
                KunneIkkeHenteFødselsnumre(
                    loggkontekst = Loggkontekst("Feil ved $OPERASJON_FØDSELSNUMRE. Kunne ikke lese svaret"),
                    sikkerloggkontekst = Loggkontekst("Feil ved $OPERASJON_FØDSELSNUMRE. Respons: $respons", it),
                )
            }
        }
    }

    private suspend fun spør(query: String, fnr: Fnr): Either<FellesPersonklientError, String> = spør(query, fnr.verdi)

    private suspend fun spør(query: String, ident: String): Either<FellesPersonklientError, String> = post(objectMapper.writeValueAsString(GraphqlQuery(query = query, variables = mapOf("ident" to ident))))

    private suspend fun spørBolk(query: String, fnrs: List<Fnr>): Either<FellesPersonklientError, String> {
        return post(
            objectMapper.writeValueAsString(
                GraphqlBolkQuery(
                    query = query,
                    variables = mapOf("identer" to fnrs.map { it.verdi }),
                ),
            ),
        )
    }

    private suspend fun post(jsonRequestBody: String): Either<FellesPersonklientError, String> {
        return withContext(Dispatchers.IO) {
            personklient.graphqlRequest(token = getToken(), jsonRequestBody = jsonRequestBody)
        }
    }
}

private const val OPERASJON_ADRESSEBESKYTTELSE = "PDL-oppslag av adressebeskyttelse"

private const val OPERASJON_FØDSELSNUMRE = "PDL-oppslag av nåværende og historiske fødselsnumre"

/**
 * Kallfeil bærer HTTP-konteksten fra klienten.
 * De øvrige feilene er utledet av et svar vi forsto, og navngis med typen; innholdet kan bære identer og går bare til sikkerlogg.
 *
 * @param omfang PII-fri tekst som følger operasjonen i vanlig logg, for eksempel antall personer.
 */
private fun <T> FellesPersonklientError.tilPdlfeil(
    operasjon: String,
    omfang: String,
    lagFeil: (loggkontekst: Loggkontekst, sikkerloggkontekst: Loggkontekst) -> T,
): T {
    val (loggkontekst, sikkerloggkontekst) = when (this) {
        is FellesPersonklientError.Kallfeil -> httpKlientError.loggkontekst("$operasjon$omfang") to httpKlientError.sikkerloggkontekst(operasjon)
        else -> Loggkontekst("Feil ved $operasjon$omfang: ${this::class.simpleName}") to Loggkontekst("Feil ved $operasjon: $this")
    }
    return lagFeil(loggkontekst, sikkerloggkontekst)
}
