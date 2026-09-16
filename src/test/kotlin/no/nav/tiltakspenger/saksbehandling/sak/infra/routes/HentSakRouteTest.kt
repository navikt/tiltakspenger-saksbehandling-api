package no.nav.tiltakspenger.saksbehandling.sak.infra.routes

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.tiltakspenger.libs.ktor.test.common.ForventetRespons
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.AvvistMetadata
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.Tilgangsvurdering
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.TilgangsvurderingAvvistÅrsak
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContext
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.hentSakForSaksnummer
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSøknadsbehandlingKlarTilBehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.søkFnrSaksnummerOgSakIdRoute
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class HentSakRouteTest {

    /**
     * Veileder er en leserolle med personinnsyn og skal kunne søke opp en sak på fnr, sakId eller saksnummer.
     */
    @Test
    fun `veileder kan søke opp sak på fnr`() {
        withTestApplicationContextAndPostgres { tac ->
            val (sak) = opprettSøknadsbehandlingKlarTilBehandling(tac = tac)

            val sakJson = søkFnrSaksnummerOgSakIdRoute(
                tac = tac,
                id = sak.fnr.verdi,
                saksbehandler = ObjectMother.veileder(),
            )
            sakJson shouldNotBe null
            sakJson!!.getString("saksnummer") shouldBe sak.saksnummer.verdi
        }
    }

    @Test
    fun `veileder og utvikler kan åpne sak via sakId og saksnummer`() {
        withTestApplicationContext { tac ->
            val (sak) = opprettSøknadsbehandlingKlarTilBehandling(tac = tac)

            listOf(ObjectMother.veileder(), ObjectMother.utvikler()).forEach { leserolle ->
                listOf(sak.id.toString(), sak.saksnummer.verdi).forEach { id ->
                    val sakJson = søkFnrSaksnummerOgSakIdRoute(
                        tac = tac,
                        id = id,
                        saksbehandler = leserolle,
                    ).shouldNotBeNull()

                    sakJson.getString("saksnummer") shouldBe sak.saksnummer.verdi
                    sakJson.getJSONObject("fnr").getBoolean("erSladdet") shouldBe leserolle.roller.erUtvikler
                }
            }
        }
    }

    @ParameterizedTest
    @EnumSource(
        value = TilgangsvurderingAvvistÅrsak::class,
        names = ["STRENGT_FORTROLIG", "STRENGT_FORTROLIG_UTLAND", "FORTROLIG", "SKJERMET", "UKJENT"],
    )
    fun `nektet persontilgang kan ikke omgås med en annen oppslagsmåte`(
        årsak: TilgangsvurderingAvvistÅrsak,
    ) {
        withTestApplicationContext { tac ->
            val (sak) = opprettSøknadsbehandlingKlarTilBehandling(tac = tac)
            tac.tilgangsmaskinFakeClient.leggTil(
                sak.fnr,
                Tilgangsvurdering.Avvist(
                    årsak = årsak,
                    begrunnelse = "Tilgang er avvist",
                    metadata = AvvistMetadata(type = "test", avvisningskode = "test", navIdent = "test", brukerIdent = sak.fnr),
                ),
            )

            listOf(ObjectMother.veileder(), ObjectMother.utvikler()).forEach { leserolle ->
                val søkeverdier = if (leserolle.roller.erUtvikler) {
                    listOf(sak.id.toString(), sak.saksnummer.verdi)
                } else {
                    listOf(sak.fnr.verdi, sak.id.toString(), sak.saksnummer.verdi)
                }
                søkeverdier.forEach { id ->
                    søkFnrSaksnummerOgSakIdRoute(
                        tac = tac,
                        id = id,
                        saksbehandler = leserolle,
                        forventet = ForventetRespons(403),
                    ) shouldBe null
                }
                hentSakForSaksnummer(
                    tac = tac,
                    saksnummer = sak.saksnummer,
                    saksbehandler = leserolle,
                    forventet = ForventetRespons(403),
                ) shouldBe null
            }
        }
    }

    /**
     * Brukere uten rolle som gir personinnsyn (i dag UTVIKLER) skal ikke kunne søke opp sak på fnr.
     * Se [no.nav.tiltakspenger.saksbehandling.sak.infra.routes.søkFnrSaksnummerOgSakIdRoute]
     */
    @Test
    fun `bruker uten personinnsyn kan ikke søke opp sak på fnr`() {
        withTestApplicationContextAndPostgres { tac ->
            val (sak) = opprettSøknadsbehandlingKlarTilBehandling(tac = tac)

            val sakJson = søkFnrSaksnummerOgSakIdRoute(
                tac = tac,
                id = sak.fnr.verdi,
                saksbehandler = ObjectMother.utvikler(),
                forventet = ForventetRespons(400, contentType = "application/json; charset=UTF-8"),
            )
            sakJson shouldBe null
        }
    }
}
