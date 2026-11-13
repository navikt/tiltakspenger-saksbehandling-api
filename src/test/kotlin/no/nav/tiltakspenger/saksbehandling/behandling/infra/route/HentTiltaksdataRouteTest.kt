package no.nav.tiltakspenger.saksbehandling.behandling.infra.route

import io.kotest.assertions.json.shouldEqualJson
import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.tiltakspenger.libs.common.RammebehandlingId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.personopplysning.Fnr
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.httpklient.infra.kall.HttpMethod
import no.nav.tiltakspenger.libs.ktor.test.common.ForventetRespons
import no.nav.tiltakspenger.libs.ktor.test.common.defaultRequestWithAssertions
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContext
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContext
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.person.Adressebeskyttelse
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSøknadsbehandlingKlarTilBehandling
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.tilLibsDeltakelse
import org.junit.jupiter.api.Test

class HentTiltaksdataRouteTest {

    @Test
    fun `henter tittelen på tiltakene i saksopplysningene fra kilden`() {
        withTestApplicationContext { tac ->
            val fnr = Fnr.random()
            val tiltaksdeltakelse = tac.tiltaksdeltakelse()
            val fratarRett = ObjectMother.tiltaksdeltakelseSomFratarRett(eksternDeltakelseId = "TA9")
            tac.leggTilTiltaksdeltakelseSomFratarRett(fnr, fratarRett.tilLibsDeltakelse(tittel = "Lønnstilskudd hos Bedrift AS"))
            val (sak, _, behandling) = opprettSøknadsbehandlingKlarTilBehandling(tac, fnr = fnr, tiltaksdeltakelse = tiltaksdeltakelse)
            tac.oppdaterTiltaksdeltakelse(fnr, tiltaksdeltakelse, tittel = "Sveisekurs hos Arrangør AS")

            hentTiltaksdata(tac, sak.id, behandling.id) shouldEqualJson """
                [
                  {
                    "eksternDeltakelseId": "${tiltaksdeltakelse.eksternDeltakelseId}",
                    "internDeltakelseId": "${tiltaksdeltakelse.internDeltakelseId}",
                    "tittel": {"verdi": "Sveisekurs hos Arrangør AS", "erSladdet": false}
                  },
                  {
                    "eksternDeltakelseId": "TA9",
                    "internDeltakelseId": null,
                    "tittel": {"verdi": "Lønnstilskudd hos Bedrift AS", "erSladdet": false}
                  }
                ]
            """.trimIndent()
        }
    }

    @Test
    fun `tittelen er null når deltakelsen ikke lenger finnes i kilden`() {
        withTestApplicationContext { tac ->
            val (sak, _, behandling) = opprettSøknadsbehandlingKlarTilBehandling(tac)
            tac.oppdaterTiltaksdeltakelse(sak.fnr, null)

            hentTiltaksdata(tac, sak.id, behandling.id) shouldEqualJson """
                [
                  {
                    "eksternDeltakelseId": "${behandling.saksopplysninger.tiltaksdeltakelser.single().eksternDeltakelseId}",
                    "internDeltakelseId": "${behandling.saksopplysninger.tiltaksdeltakelser.single().internDeltakelseId}",
                    "tittel": {"verdi": null, "erSladdet": false}
                  }
                ]
            """.trimIndent()
        }
    }

    @Test
    fun `tittelen tas ikke med når personen har adressebeskyttelse`() {
        withTestApplicationContext { tac ->
            val fnr = Fnr.random()
            val tiltaksdeltakelse = tac.tiltaksdeltakelse()
            val (sak, _, behandling) = opprettSøknadsbehandlingKlarTilBehandling(tac, fnr = fnr, tiltaksdeltakelse = tiltaksdeltakelse)
            tac.leggTilPerson(
                fnr = fnr,
                person = ObjectMother.personopplysningKjedeligFyr(fnr = fnr, adressebeskyttelse = Adressebeskyttelse.FORTROLIG),
                tiltaksdeltakelse = tiltaksdeltakelse,
            )
            tac.oppdaterTiltaksdeltakelse(fnr, tiltaksdeltakelse, tittel = "Sveisekurs hos Arrangør AS")

            hentTiltaksdata(tac, sak.id, behandling.id) shouldEqualJson """
                [
                  {
                    "eksternDeltakelseId": "${tiltaksdeltakelse.eksternDeltakelseId}",
                    "internDeltakelseId": "${tiltaksdeltakelse.internDeltakelseId}",
                    "tittel": {"verdi": null, "erSladdet": false}
                  }
                ]
            """.trimIndent()
        }
    }

    private suspend fun ApplicationTestBuilder.hentTiltaksdata(
        tac: TestApplicationContext,
        sakId: SakId,
        behandlingId: RammebehandlingId,
    ): String {
        val saksbehandler = ObjectMother.saksbehandler()
        val jwt = tac.jwtGenerator.createJwtForSaksbehandler(saksbehandler = saksbehandler)
        tac.leggTilBruker(jwt, saksbehandler)
        return defaultRequestWithAssertions(
            HttpMethod.GET,
            "/sak/$sakId/behandling/$behandlingId/tiltaksdata",
            jwt = jwt,
            forventet = ForventetRespons(200, contentType = "application/json; charset=UTF-8"),
        ).body
    }
}
