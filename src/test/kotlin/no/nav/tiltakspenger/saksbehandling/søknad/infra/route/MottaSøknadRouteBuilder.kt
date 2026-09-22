package no.nav.tiltakspenger.saksbehandling.søknad.infra.route

import io.kotest.matchers.shouldBe
import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksnummer
import no.nav.tiltakspenger.libs.common.SøknadId
import no.nav.tiltakspenger.libs.common.personopplysning.Fnr
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.httpklient.infra.kall.HttpMethod
import no.nav.tiltakspenger.libs.ktor.test.common.ForventetRespons
import no.nav.tiltakspenger.libs.ktor.test.common.defaultRequestWithAssertions
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContext
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.objectmothers.toSøknadstiltak
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.hentEllerOpprettSakForSystembruker
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.søknad.domene.BarnetilleggFraSøknad
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
        tiltaksdeltakelse: Tiltaksdeltakelse = tac.tiltaksdeltakelse(),
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
        tiltaksdeltakelse: Tiltaksdeltakelse = tac.tiltaksdeltakelse(),
        barnetillegg: List<BarnetilleggFraSøknad> = emptyList(),
    ): Pair<Sak, Søknad> {
        val saksnummer = hentEllerOpprettSakForSystembruker(tac, fnr)
        tac.tiltakContext.tiltaksdeltakerRepo.lagre(
            id = tiltaksdeltakelse.internDeltakelseId,
            eksternId = tiltaksdeltakelse.eksternDeltakelseId,
            tiltakstype = tiltaksdeltakelse.typeKode.tilTiltakstype(),
        )
        mottaSøknad(tac, fnr, saksnummer, søknadId, tiltaksdeltakelse, barnetillegg)
        val sak: Sak = tac.sakContext.sakRepo.hentForSaksnummer(saksnummer)!!
        return sak to sak.søknader.single { it.id == søknadId }
    }

    suspend fun ApplicationTestBuilder.mottaSøknad(
        tac: TestApplicationContext,
        fnr: Fnr,
        saksnummer: Saksnummer,
        søknadId: SøknadId = SøknadId.random(),
        tiltaksdeltakelse: Tiltaksdeltakelse = tac.tiltaksdeltakelse(),
        barnetillegg: List<BarnetilleggFraSøknad> = emptyList(),
    ) {
        val jwt = tac.jwtGenerator.createJwtForSystembruker(
            roles = listOf("hent_eller_opprett_sak", "lagre_soknad"),
        )
        tac.leggTilBruker(jwt, ObjectMother.systembrukerHentEllerOpprettSakOgLagreSoknad())
        defaultRequestWithAssertions(
            HttpMethod.POST,
            "/soknad",
            jwt = jwt,
            forventet = ForventetRespons(status = 200),
            body =
            createRequest(
                saksnummer = saksnummer.verdi,
                fnr = fnr.verdi,
                søknadId = søknadId.toString(),
                clock = tac.clock,
                tiltaksdeltakelse = tiltaksdeltakelse.toSøknadstiltak(),
                barnetillegg = barnetillegg,
            ),
        ).apply {
            val bodyAsText = this.body
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
        barnetillegg: List<BarnetilleggFraSøknad> = emptyList(),
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
            "barnetilleggPdl": ${barnetillegg.filterIsInstance<BarnetilleggFraSøknad.FraPdl>().tilJson()},
            "barnetilleggManuelle": ${barnetillegg.filterIsInstance<BarnetilleggFraSøknad.Manuell>().tilJson()},
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

/**
 * Serialiserer barnetilleggene til formen søknad-api sender dem på.
 * Brukes av testene som trenger en søknad med barn, for eksempel sladdingstestene.
 */
private fun List<BarnetilleggFraSøknad>.tilJson(): String = this.joinToString(
    separator = ",",
    prefix = "[",
    postfix = "]",
) { barn ->
    val fnr = (barn as? BarnetilleggFraSøknad.FraPdl)?.fnr?.verdi
    """
    {
      "fnr": ${fnr.tilJsonstreng()},
      "fødselsdato": "${barn.fødselsdato}",
      "fornavn": ${barn.fornavn.tilJsonstreng()},
      "mellomnavn": ${barn.mellomnavn.tilJsonstreng()},
      "etternavn": ${barn.etternavn.tilJsonstreng()},
      "oppholderSegIEØS": { "svar": "${barn.oppholderSegIEØS.tilSpmSvar()}" }
    }
    """.trimIndent()
}

private fun String?.tilJsonstreng(): String = if (this == null) "null" else "\"$this\""

private fun Søknad.JaNeiSpm.tilSpmSvar(): String = when (this) {
    Søknad.JaNeiSpm.Ja -> "Ja"

    Søknad.JaNeiSpm.Nei,
    Søknad.JaNeiSpm.IkkeBesvart,
    -> "Nei"
}
