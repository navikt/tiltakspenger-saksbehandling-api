package no.nav.tiltakspenger.saksbehandling.infra.route

import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.dato.juni
import no.nav.tiltakspenger.libs.httpklient.infra.kall.HttpMethod
import no.nav.tiltakspenger.libs.json.objectMapper
import no.nav.tiltakspenger.libs.ktor.test.common.ForventetRespons
import no.nav.tiltakspenger.libs.ktor.test.common.defaultRequestWithAssertions
import no.nav.tiltakspenger.saksbehandling.common.IsolatedDatabaseTest
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContext
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsresultat
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.ferdigstiltOpprettholdtKlagebehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSøknadsbehandlingKlarTilBehandling
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import org.junit.jupiter.api.Test
import tools.jackson.databind.JsonNode

/**
 * Kontrollerer at personopplysninger ikke lekker ut i json-en til brukere uten fagrolle.
 * Kontrollen som saksbehandler beviser at verdiene faktisk finnes i svarene, slik at den negative skanningen er verdt noe.
 */
class SladdingRouteTest {

    private val barnFnr = Fnr.random()
    private val barnetilleggMedIdent = ObjectMother.barnetilleggMedIdent(
        fnr = barnFnr,
        fornavn = "Barnefornavn",
        mellomnavn = "Barnemellomnavn",
        etternavn = "Barneetternavn",
        fødselsdato = 14.juni(2012),
    )
    private val barnetilleggUtenIdent = ObjectMother.barnetilleggUtenIdent(
        fornavn = "Manueltbarnfornavn",
        mellomnavn = "Manueltbarnmellomnavn",
        etternavn = "Manueltbarnetternavn",
        fødselsdato = 14.juni(2012),
    )

    @Test
    @IsolatedDatabaseTest
    fun `bruker uten fagrolle får sladdet sak uten fødselsnummer navn eller barneopplysninger i json`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            val (sak) = opprettSakMedBarnetillegg(tac)

            val somSaksbehandler = alleLesekall(tac, sak, ObjectMother.saksbehandler())
            val somUtvikler = alleLesekall(tac, sak, ObjectMother.utvikler())

            val sensitiveVerdier = sensitiveVerdier(sak, somSaksbehandler)

            somUtvikler.forEach { (navn, body) ->
                sensitiveVerdier.forEach { verdi ->
                    withClue(navn) { body shouldNotContain verdi }
                }
            }

            objectMapper.readTree(somUtvikler.getValue(SAK)).let {
                it["fnr"].stringValue() shouldBe SLADDET_TEKST
                it["søknader"].single()["barnetillegg"].forEach { barn ->
                    barn["fornavn"].stringValue() shouldBe SLADDET_TEKST
                    barn["fødselsdato"].stringValue() shouldBe SLADDET_TEKST
                }
                it["rammebehandlinger"].single()["saksopplysninger"]["fødselsdato"].stringValue() shouldBe
                    SLADDET_TEKST
            }
            objectMapper.readTree(somUtvikler.getValue(SØK_SAK))["fnr"].stringValue() shouldBe SLADDET_TEKST
            objectMapper.readTree(somUtvikler.getValue(PERSONOPPLYSNINGER))["fnr"].stringValue() shouldBe SLADDET_TEKST
            objectMapper.readTree(somUtvikler.getValue(BARN)).forEach {
                it["fnr"].stringValue() shouldBe SLADDET_TEKST
            }
            objectMapper.readTree(somUtvikler.getValue(TILTAKSDELTAKELSER)).forEach {
                it["visningsnavn"].stringValue() shouldBe SLADDET_TEKST
            }
            objectMapper.readTree(somUtvikler.getValue(BENK))["oversikt"]["behandlinger"].forEach {
                it["fnr"].stringValue() shouldBe SLADDET_TEKST
            }
        }
    }

    @Test
    @IsolatedDatabaseTest
    fun `saksbehandler får full sak med urørte personopplysninger`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            val (sak) = opprettSakMedBarnetillegg(tac)

            val somSaksbehandler = alleLesekall(tac, sak, ObjectMother.saksbehandler())

            somSaksbehandler.getValue(SAK).let {
                it shouldContain sak.fnr.verdi
                it shouldContain barnFnr.verdi
                it shouldContain "Barnefornavn"
                it shouldContain "Manueltbarnfornavn"
                it shouldContain 14.juni(2012).toString()
            }
            somSaksbehandler.getValue(SØK_SAK) shouldContain sak.fnr.verdi
            somSaksbehandler.getValue(PERSONOPPLYSNINGER) shouldContain sak.fnr.verdi
            somSaksbehandler.getValue(TILTAKSDELTAKELSER) shouldContain
                ObjectMother.tiltaksdeltakelseMedArrangørnavn().visningsnavn
            somSaksbehandler.getValue(BENK) shouldContain sak.fnr.verdi
        }
    }

    @Test
    fun `bruker uten fagrolle får 403 ved henting av innstillingsbrev som pdf`() {
        withTestApplicationContextAndPostgres { tac ->
            val (sak, klagebehandling) = ferdigstiltOpprettholdtKlagebehandling(tac = tac)!!
            val dokumentInfoId = (klagebehandling.resultat as Klagebehandlingsresultat.Opprettholdt)
                .dokumentInfoIder
                .first()
            val path = "/sak/${sak.id}/klage/${klagebehandling.id}/innstillingsbrev/$dokumentInfoId"

            hentSomBruker(
                tac = tac,
                path = path,
                saksbehandler = ObjectMother.utvikler(),
                forventet = ForventetRespons(403, contentType = "application/json; charset=UTF-8"),
            ).let {
                objectMapper.readTree(it)["kode"].stringValue() shouldBe "pdf_krever_fagrolle"
            }

            hentSomBruker(
                tac = tac,
                path = path,
                saksbehandler = ObjectMother.veileder(),
                forventet = ForventetRespons(200, contentType = "application/pdf"),
            )
        }
    }

    private suspend fun ApplicationTestBuilder.opprettSakMedBarnetillegg(
        tac: TestApplicationContext,
    ): Triple<Sak, *, *> = opprettSøknadsbehandlingKlarTilBehandling(
        tac = tac,
        barnetillegg = listOf(barnetilleggMedIdent, barnetilleggUtenIdent),
    )

    /**
     * Verdiene som ikke skal finnes igjen i svaret til en bruker uten fagrolle.
     * Navnene på søkeren og barna hentes fra saksbehandlerens svar, slik at fakenes genererte verdier også dekkes.
     * Barn med adressebeskyttelse får navnet sitt fjernet allerede i domenet, så feltene leses nullsikkert.
     */
    private fun sensitiveVerdier(sak: Sak, somSaksbehandler: Map<String, String>): List<String> {
        val personopplysninger = objectMapper.readTree(somSaksbehandler.getValue(PERSONOPPLYSNINGER))
        val barn = objectMapper.readTree(somSaksbehandler.getValue(BARN))

        return listOfNotNull(
            sak.fnr.verdi,
            barnFnr.verdi,
            "Barnefornavn",
            "Barnemellomnavn",
            "Barneetternavn",
            "Manueltbarnfornavn",
            "Manueltbarnmellomnavn",
            "Manueltbarnetternavn",
            14.juni(2012).toString(),
            ObjectMother.tiltaksdeltakelseMedArrangørnavn().visningsnavn,
            personopplysninger.tekst("fornavn"),
            personopplysninger.tekst("etternavn"),
            personopplysninger.tekst("fødselsdato"),
        ) + barn.flatMap {
            listOfNotNull(it.tekst("fnr"), it.tekst("fornavn"), it.tekst("etternavn"))
        }
    }

    private fun JsonNode.tekst(felt: String): String? = this[felt]?.takeUnless { it.isNull }?.stringValue()

    private suspend fun ApplicationTestBuilder.alleLesekall(
        tac: TestApplicationContext,
        sak: Sak,
        saksbehandler: Saksbehandler,
    ): Map<String, String> = mapOf(
        SAK to hentSomBruker(tac, "/sak/${sak.saksnummer.verdi}", saksbehandler),
        SØK_SAK to hentSomBruker(
            tac = tac,
            path = "/sak",
            saksbehandler = saksbehandler,
            metode = HttpMethod.POST,
            body = """{"fnr":"${sak.saksnummer.verdi}"}""",
        ),
        PERSONOPPLYSNINGER to hentSomBruker(tac, "/sak/${sak.id}/personopplysninger", saksbehandler),
        BARN to hentSomBruker(tac, "/sak/${sak.id}/personopplysninger/barn", saksbehandler),
        TILTAKSDELTAKELSER to hentSomBruker(
            tac = tac,
            path = "/sak/${sak.id}/tiltaksdeltakelser?fraOgMed=2023-01-01&tilOgMed=2023-03-31",
            saksbehandler = saksbehandler,
        ),
        BENK to hentSomBruker(
            tac = tac,
            path = "/benk/soknader",
            saksbehandler = saksbehandler,
            metode = HttpMethod.POST,
            body = """{}""",
        ),
    )

    private suspend fun ApplicationTestBuilder.hentSomBruker(
        tac: TestApplicationContext,
        path: String,
        saksbehandler: Saksbehandler,
        metode: HttpMethod = HttpMethod.GET,
        body: String? = null,
        forventet: ForventetRespons? = ForventetRespons(200, contentType = "application/json; charset=UTF-8"),
    ): String {
        val jwt = tac.jwtGenerator.createJwtForSaksbehandler(saksbehandler = saksbehandler)
        tac.leggTilBruker(jwt, saksbehandler)
        return defaultRequestWithAssertions(
            metode,
            path,
            jwt = jwt,
            forventet = forventet,
            body = body,
        ).body
    }

    private companion object {
        const val SAK = "GET /sak/{saksnummer}"
        const val SØK_SAK = "POST /sak"
        const val PERSONOPPLYSNINGER = "GET /sak/{sakId}/personopplysninger"
        const val BARN = "GET /sak/{sakId}/personopplysninger/barn"
        const val TILTAKSDELTAKELSER = "GET /sak/{sakId}/tiltaksdeltakelser"
        const val BENK = "POST /benk/soknader"
    }
}
