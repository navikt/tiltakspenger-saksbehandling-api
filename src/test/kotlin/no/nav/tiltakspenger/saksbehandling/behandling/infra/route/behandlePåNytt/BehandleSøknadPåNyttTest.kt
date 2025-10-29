package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.behandlePåNytt

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.instanceOf
import io.ktor.http.HttpStatusCode
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.infra.dto.Tilgangsvurdering
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.infra.dto.TilgangsvurderingAvvistÅrsak
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Søknadsbehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.SøknadsbehandlingResultat
import no.nav.tiltakspenger.saksbehandling.behandling.domene.SøknadsbehandlingType
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContext
import no.nav.tiltakspenger.saksbehandling.infra.route.routes
import no.nav.tiltakspenger.saksbehandling.infra.setup.configureExceptions
import no.nav.tiltakspenger.saksbehandling.infra.setup.jacksonSerialization
import no.nav.tiltakspenger.saksbehandling.infra.setup.setupAuthentication
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.behandleSøknadPåNytt
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.startBehandlingAvSøknadPåNyttForSøknadId
import org.junit.jupiter.api.Test

internal class BehandleSøknadPåNyttTest {
    @Test
    fun `kan behandle avslått søknad på nytt`() = runTest {
        with(TestApplicationContext()) {
            val tac = this
            testApplication {
                application {
                    jacksonSerialization()
                    configureExceptions()
                    setupAuthentication(texasClient)
                    routing { routes(tac) }
                }
                val (sak, søknad, behandling) = this.iverksettSøknadsbehandling(
                    tac = tac,
                    resultat = SøknadsbehandlingType.AVSLAG,
                )
                behandling.virkningsperiode.shouldNotBeNull()
                behandling.status shouldBe Rammebehandlingsstatus.VEDTATT
                behandling.resultat is SøknadsbehandlingResultat.Avslag

                val (_, _, nyBehandling, _) = this.behandleSøknadPåNytt(
                    tac = tac,
                    sak = sak,
                    søknad = søknad,
                    fnr = behandling.fnr,
                    virkningsperiode = behandling.virkningsperiode,
                )

                nyBehandling.shouldNotBeNull()
                nyBehandling.status shouldBe Rammebehandlingsstatus.UNDER_BEHANDLING
                nyBehandling shouldBe instanceOf<Søknadsbehandling>()
                nyBehandling.søknad.id shouldBe behandling.søknad.id
            }
        }
    }

    @Test
    fun `må være saksbehandler for å behandle avslått søknad på nytt`() = runTest {
        with(TestApplicationContext()) {
            val tac = this
            testApplication {
                application {
                    jacksonSerialization()
                    configureExceptions()
                    setupAuthentication(texasClient)
                    routing { routes(tac) }
                }
                val (sak, søknad, behandling) = this.iverksettSøknadsbehandling(
                    tac = tac,
                    resultat = SøknadsbehandlingType.AVSLAG,
                )
                behandling.virkningsperiode.shouldNotBeNull()
                behandling.status shouldBe Rammebehandlingsstatus.VEDTATT
                behandling.resultat is SøknadsbehandlingResultat.Avslag

                val responskode = this.startBehandlingAvSøknadPåNyttForSøknadId(
                    tac = tac,
                    sakId = sak.id,
                    søknadId = søknad.id,
                    saksbehandler = ObjectMother.beslutter(),
                )

                responskode shouldBe HttpStatusCode.Forbidden
            }
        }
    }

    @Test
    fun `må ha tilgang til person for å behandle avslått søknad på nytt`() = runTest {
        with(TestApplicationContext()) {
            val tac = this
            testApplication {
                application {
                    jacksonSerialization()
                    configureExceptions()
                    setupAuthentication(texasClient)
                    routing { routes(tac) }
                }
                val (sak, søknad, behandling) = this.iverksettSøknadsbehandling(
                    tac = tac,
                    resultat = SøknadsbehandlingType.AVSLAG,
                )
                behandling.virkningsperiode.shouldNotBeNull()
                behandling.status shouldBe Rammebehandlingsstatus.VEDTATT
                behandling.resultat is SøknadsbehandlingResultat.Avslag

                tac.tilgangsmaskinFakeClient.leggTil(
                    sak.fnr,
                    Tilgangsvurdering.Avvist(
                        type = "test",
                        årsak = TilgangsvurderingAvvistÅrsak.STRENGT_FORTROLIG,
                        status = 403,
                        brukerIdent = "test",
                        navIdent = "test",
                        begrunnelse = "test",
                    ),
                )

                val responskode = this.startBehandlingAvSøknadPåNyttForSøknadId(
                    tac = tac,
                    sakId = sak.id,
                    søknadId = søknad.id,
                    saksbehandler = ObjectMother.saksbehandler(),
                )

                responskode shouldBe HttpStatusCode.Forbidden
            }
        }
    }
}
