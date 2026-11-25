package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.barnetillegg

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.tiltakspenger.libs.common.personopplysning.Fnr
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.httpklient.infra.kall.HttpMethod
import no.nav.tiltakspenger.libs.ktor.test.common.ForventetRespons
import no.nav.tiltakspenger.libs.ktor.test.common.defaultRequestWithAssertions
import no.nav.tiltakspenger.saksbehandling.barnetillegg.BarnMedBarnetillegg
import no.nav.tiltakspenger.saksbehandling.barnetillegg.BarnetilleggV2
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContext
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContext
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.innvilgelsesperioder
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.oppdaterSøknadsbehandlingInnvilgelse
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSøknadsbehandlingUnderBehandling
import org.json.JSONObject
import org.junit.jupiter.api.Test

class BarnFraFolkeregisterRouteTest {
    @Test
    fun `slår opp søkers barn, men ikke andre personer`() {
        withTestApplicationContext { tac ->
            val (sak, _, behandling) = opprettSøknadsbehandlingUnderBehandling(tac)
            val barnFnr = tac.leggTilBarn(sak.fnr)
            val path = "/sak/${sak.id}/behandling/${behandling.id}/barn/folkeregister"

            JSONObject(slåOpp(tac, path, barnFnr.verdi)).getJSONObject("fornavn").getString("verdi") shouldBe "Ola"

            JSONObject(slåOpp(tac, path, sak.fnr.verdi, 404)).getString("kode") shouldBe "barn_ikke_funnet"
            JSONObject(slåOpp(tac, path, "123", 400)).getString("kode") shouldBe "ugyldig_fnr"
        }
    }

    @Test
    fun `barn fra folkeregisteret lagres bare når det stemmer med søkers barn`() {
        withTestApplicationContext { tac ->
            val (sak, _, behandling) = opprettSøknadsbehandlingUnderBehandling(tac)
            val barnFnr = tac.leggTilBarn(sak.fnr)
            val tiltaksdeltakelse = behandling.saksopplysninger.tiltaksdeltakelser.first()
            val innvilgelsesperioder = innvilgelsesperioder(tiltaksdeltakelse.lukketPeriode!!, tiltaksdeltakelse)

            fun barnetilleggJson(fnr: String, fornavn: String) = """
                {
                  "begrunnelse": null,
                  "barn": [{"fnr": "$fnr", "fornavn": "$fornavn", "mellomnavn": null, "etternavn": "Nordmann", "fødselsdato": "2020-01-01", "kilde": "FOLKEREGISTER", "søknadId": null, "perioder": null}]
                }
            """.trimIndent()

            listOf(barnFnr.verdi to "Kari", sak.fnr.verdi to "Ola").forEach { (fnr, fornavn) ->
                val (_, _, responseJson) = oppdaterSøknadsbehandlingInnvilgelse(
                    tac = tac,
                    sakId = sak.id,
                    behandlingId = behandling.id,
                    innvilgelsesperioder = innvilgelsesperioder,
                    barnetilleggJson = barnetilleggJson(fnr, fornavn),
                    forventet = ForventetRespons(400, contentType = "application/json; charset=UTF-8"),
                )
                JSONObject(responseJson).getString("melding") shouldBe
                    "Barnet født 1. januar 2020 stemmer ikke med søkers barn i folkeregisteret. Fjern barnet og legg det til på nytt."
            }
            tac.behandlingContext.rammebehandlingRepo.hent(behandling.id).barnetillegg.shouldBeNull()

            oppdaterSøknadsbehandlingInnvilgelse(
                tac = tac,
                sakId = sak.id,
                behandlingId = behandling.id,
                innvilgelsesperioder = innvilgelsesperioder,
                barnetilleggJson = barnetilleggJson(barnFnr.verdi, "Ola"),
            )
            tac.behandlingContext.rammebehandlingRepo.hent(behandling.id).barnetillegg
                .shouldBeInstanceOf<BarnetilleggV2>()
                .barn.single().kilde shouldBe BarnMedBarnetillegg.Kilde.Folkeregister
        }
    }

    /** Gir søkers første (ugraderte) barn kjent navn og fødselsdato. */
    private fun TestApplicationContext.leggTilBarn(søker: Fnr): Fnr = barnAv(søker).first().also {
        leggTilPersonopplysning(
            ObjectMother.personopplysningKjedeligFyr(fnr = it, fødselsdato = 1.januar(2020), fornavn = "Ola", etternavn = "Nordmann"),
        )
    }

    private suspend fun ApplicationTestBuilder.slåOpp(
        tac: TestApplicationContext,
        path: String,
        fnr: String,
        status: Int = 200,
    ): String {
        val saksbehandler = ObjectMother.saksbehandler()
        val jwt = tac.jwtGenerator.createJwtForSaksbehandler(saksbehandler = saksbehandler)
        tac.leggTilBruker(jwt, saksbehandler)
        return defaultRequestWithAssertions(
            HttpMethod.POST,
            path,
            jwt = jwt,
            forventet = ForventetRespons(status, contentType = "application/json; charset=UTF-8"),
            body = """{"fnr":"$fnr"}""",
        ).body
    }
}
