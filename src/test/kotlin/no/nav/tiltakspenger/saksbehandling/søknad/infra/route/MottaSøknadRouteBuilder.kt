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
import no.nav.tiltakspenger.libs.ktor.test.common.defaultRequest
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContext
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.tiltaksdeltakelse
import no.nav.tiltakspenger.saksbehandling.objectmothers.toSøknadstiltak
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.hentEllerOpprettSakForSystembruker
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.sak.Saksnummer
import no.nav.tiltakspenger.saksbehandling.søknad.domene.Søknad
import no.nav.tiltakspenger.saksbehandling.søknad.domene.Søknadstiltak
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.Tiltaksdeltakelse
import java.time.Clock
import java.time.LocalDateTime

/**
 * Gir mulighet til å motta en søknad via endepunktene våre.
 */
interface MottaSøknadRouteBuilder {

    suspend fun ApplicationTestBuilder.opprettSøknadPåSakId(
        tac: TestApplicationContext,
        sakId: SakId,
        søknadId: SøknadId = SøknadId.random(),
        tiltaksdeltakelse: Tiltaksdeltakelse = tiltaksdeltakelse(),
    ): Pair<Sak, Søknad> {
        val sak = tac.sakContext.sakRepo.hentForSakId(sakId)!!
        val saksnummer = hentEllerOpprettSakForSystembruker(tac, sak.fnr)
        tac.tiltakContext.tiltaksdeltakerRepo.lagre(
            id = tiltaksdeltakelse.internDeltakelseId,
            eksternId = tiltaksdeltakelse.eksternDeltakelseId,
            tiltakstype = tiltaksdeltakelse.typeKode.tilTiltakstype(),
        )
        mottaSøknad(tac, sak.fnr, saksnummer, søknadId, tiltaksdeltakelse)
        val oppdatertSak: Sak = tac.sakContext.sakRepo.hentForSaksnummer(saksnummer)!!
        return oppdatertSak to oppdatertSak.søknader.single { it.id == søknadId }
    }

    suspend fun ApplicationTestBuilder.opprettSakOgSøknad(
        tac: TestApplicationContext,
        fnr: Fnr = Fnr.random(),
        søknadId: SøknadId = SøknadId.random(),
        tiltaksdeltakelse: Tiltaksdeltakelse = tiltaksdeltakelse(),
    ): Pair<Sak, Søknad> {
        val saksnummer = hentEllerOpprettSakForSystembruker(tac, fnr)
        tac.tiltakContext.tiltaksdeltakerRepo.lagre(
            id = tiltaksdeltakelse.internDeltakelseId,
            eksternId = tiltaksdeltakelse.eksternDeltakelseId,
            tiltakstype = tiltaksdeltakelse.typeKode.tilTiltakstype(),
        )
        mottaSøknad(tac, fnr, saksnummer, søknadId, tiltaksdeltakelse)
        val sak: Sak = tac.sakContext.sakRepo.hentForSaksnummer(saksnummer)!!
        return sak to sak.søknader.single { it.id == søknadId }
    }

    suspend fun ApplicationTestBuilder.mottaSøknad(
        tac: TestApplicationContext,
        fnr: Fnr,
        saksnummer: Saksnummer,
        søknadId: SøknadId = SøknadId.random(),
        tiltaksdeltakelse: Tiltaksdeltakelse = tiltaksdeltakelse(),
    ) {
        val jwt = tac.jwtGenerator.createJwtForSystembruker(
            roles = listOf("hent_eller_opprett_sak", "lagre_soknad"),
        )
        tac.leggTilBruker(jwt, ObjectMother.systembrukerHentEllerOpprettSakOgLagreSoknad())
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
                    clock = tac.clock,
                    tiltaksdeltakelse = tiltaksdeltakelse.toSøknadstiltak(),
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
                person = personopplysningerForBrukerFraPdl,
                tiltaksdeltakelse = tiltaksdeltakelse,
            )
        }
    }

    private fun createRequest(
        søknadId: String = SøknadId.random().toString(),
        clock: Clock,
        saksnummer: String = Saksnummer.genererSaknummer(løpenr = "0001", clock = clock).verdi,
        journalpostId: String = "123456789",
        fnr: String = Fnr.random().toString(),
        tiltaksdeltakelse: Søknadstiltak,
        opprettet: LocalDateTime = tiltaksdeltakelse.deltakelseFom.atTime(0, 0, 0, 0),
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
              "id": "${tiltaksdeltakelse.id}",
              "arrangør": "Testarrangør",
              "typeKode": "${tiltaksdeltakelse.typeKode.name}",
              "typeNavn": "${tiltaksdeltakelse.typeNavn}",
              "deltakelseFom": "${tiltaksdeltakelse.deltakelseFom}",
              "deltakelseTom": "${tiltaksdeltakelse.deltakelseTom}"
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
            "opprettet": "$opprettet",
            "saksnummer": "$saksnummer"
        }
        """.trimIndent()
    }
}
