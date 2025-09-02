package no.nav.tiltakspenger.saksbehandling.søknad.infra.route

import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLProtocol
import io.ktor.http.contentType
import io.ktor.http.path
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.util.url
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.SøknadId
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.dato.april
import no.nav.tiltakspenger.libs.ktor.test.common.defaultRequest
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContext
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.hentEllerOpprettSak
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.sak.Saksnummer
import no.nav.tiltakspenger.saksbehandling.søknad.Søknad
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.Tiltaksdeltagelse

/**
 * Gir mulighet til å motta en søknad via endepunktene våre.
 */
interface MottaSøknadRouteBuilder {

    suspend fun ApplicationTestBuilder.opprettSøknadPåSakId(
        tac: TestApplicationContext,
        sakId: SakId,
        søknadId: SøknadId = SøknadId.random(),
        deltakelsesperiode: Periode = Periode(1.april(2025), 10.april(2025)),
        tiltaksdeltagelse: Tiltaksdeltagelse = ObjectMother.tiltaksdeltagelseTac(
            fom = deltakelsesperiode.fraOgMed,
            tom = deltakelsesperiode.tilOgMed,
            eksternTiltaksdeltagelseId = "ABC1234",
        ),
    ): Pair<Sak, Søknad> {
        val sak = tac.sakContext.sakRepo.hentForSakId(sakId)!!
        val saksnummer = hentEllerOpprettSak(tac, sak.fnr)
        mottaSøknad(tac, sak.fnr, saksnummer, søknadId, deltakelsesperiode, tiltaksdeltagelse)
        val oppdatertSak: Sak = tac.sakContext.sakRepo.hentForSaksnummer(saksnummer)!!
        return oppdatertSak to oppdatertSak.soknader.single { it.id == søknadId }
    }

    suspend fun ApplicationTestBuilder.opprettSakOgSøknad(
        tac: TestApplicationContext,
        fnr: Fnr = Fnr.random(),
        søknadId: SøknadId = SøknadId.random(),
        deltakelsesperiode: Periode = Periode(1.april(2025), 10.april(2025)),
        tiltaksdeltagelse: Tiltaksdeltagelse = ObjectMother.tiltaksdeltagelseTac(
            fom = deltakelsesperiode.fraOgMed,
            tom = deltakelsesperiode.tilOgMed,
        ),
    ): Pair<Sak, Søknad> {
        val saksnummer = hentEllerOpprettSak(tac, fnr)
        mottaSøknad(tac, fnr, saksnummer, søknadId, deltakelsesperiode, tiltaksdeltagelse)
        val sak: Sak = tac.sakContext.sakRepo.hentForSaksnummer(saksnummer)!!
        return sak to sak.soknader.single { it.id == søknadId }
    }

    suspend fun ApplicationTestBuilder.mottaSøknad(
        tac: TestApplicationContext,
        fnr: Fnr,
        saksnummer: Saksnummer,
        søknadId: SøknadId = SøknadId.random(),
        deltakelsesperiode: Periode = Periode(1.april(2025), 10.april(2025)),
        tiltaksdeltagelse: Tiltaksdeltagelse = ObjectMother.tiltaksdeltagelseTac(
            fom = deltakelsesperiode.fraOgMed,
            tom = deltakelsesperiode.tilOgMed,
        ),
    ) {
        val jwt = tac.jwtGenerator.createJwtForSystembruker(
            roles = listOf("hent_eller_opprett_sak", "lagre_soknad"),
        )
        tac.texasClient.leggTilBruker(jwt, ObjectMother.systembrukerHentEllerOpprettSakOgLagreSoknad())
        defaultRequest(
            HttpMethod.Post,
            url {
                protocol = URLProtocol.HTTPS
                path("/soknad")
            },
            jwt = jwt,
        ) {
            setBody(
                createRequest(
                    saksnummer = saksnummer.verdi,
                    fnr = fnr.verdi,
                    søknadId = søknadId.toString(),
                    deltakelsesperiode = deltakelsesperiode,
                    tiltaksdeltagelse = tiltaksdeltagelse,
                ),
            )
        }.apply {
            val bodyAsText = this.bodyAsText()
            withClue(
                "Response details:\n" + "Status: ${this.status}\n" + "Content-Type: ${this.contentType()}\n" + "Body: $bodyAsText\n",
            ) {
                status shouldBe HttpStatusCode.OK
            }
            bodyAsText shouldBe "OK"

            val personopplysningerForBrukerFraPdl = ObjectMother.personopplysningKjedeligFyr(
                fnr = fnr,
            )
            tac.leggTilPerson(
                fnr = fnr,
                personopplysningerForBruker = personopplysningerForBrukerFraPdl,
                tiltaksdeltagelse = tiltaksdeltagelse,
            )
        }
    }

    private fun createRequest(
        søknadId: String = SøknadId.random().toString(),
        saksnummer: String = Saksnummer.genererSaknummer(løpenr = "0001").verdi,
        journalpostId: String = "123456789",
        fnr: String = Fnr.random().toString(),
        deltakelsesperiode: Periode = Periode(1.april(2025), 10.april(2025)),
        tiltaksdeltagelse: Tiltaksdeltagelse,
    ): String {
        return """
        {
            "versjon": "4",
            "søknadId": "$søknadId",
            "journalpostId": "$journalpostId",
            "personopplysninger": {
              "ident": "$fnr",
              "fornavn": "NØDVENDIG",
              "etternavn": "HOFTE"
            },
            "tiltak": {
              "id": "${tiltaksdeltagelse.eksternDeltagelseId}",
              "arrangør": "Testarrangør",
              "typeKode": "${tiltaksdeltagelse.typeKode}",
              "typeNavn": "${tiltaksdeltagelse.typeNavn}",
              "deltakelseFom": "${deltakelsesperiode.fraOgMed}",
              "deltakelseTom": "${deltakelsesperiode.tilOgMed}"
            },
            "barnetilleggPdl": [],
            "barnetilleggManuelle": [],
            "vedlegg": 0,
            "kvp": {
              "svar": "Nei",
              "fom": null,
              "tom": null
            },
            "intro": {
              "svar": "Nei",
              "fom": null,
              "tom": null
            },
            "institusjon": {
              "svar": "Nei",
              "fom": null,
              "tom": null
            },
            "etterlønn": {
              "svar": "Nei"
            },
            "gjenlevendepensjon": {
              "svar": "Nei",
              "fom": null,
              "tom": null
            },
            "alderspensjon": {
              "svar": "Nei",
              "fom": null
            },
            "sykepenger": {
              "svar": "Nei",
              "fom": null,
              "tom": null
            },
            "supplerendeStønadAlder": {
              "svar": "Nei",
              "fom": null,
              "tom": null
            },
            "supplerendeStønadFlyktning": {
              "svar": "Nei",
              "fom": null,
              "tom": null
            },
            "jobbsjansen": {
              "svar": "Nei",
              "fom": null,
              "tom": null
            },
            "trygdOgPensjon": {
              "svar": "Nei",
              "fom": null,
              "tom": null
            },
            "opprettet": "${deltakelsesperiode.fraOgMed.atTime(0, 0, 0, 0)}",
            "saksnummer": "$saksnummer"
        }
        """.trimIndent()
    }
}
