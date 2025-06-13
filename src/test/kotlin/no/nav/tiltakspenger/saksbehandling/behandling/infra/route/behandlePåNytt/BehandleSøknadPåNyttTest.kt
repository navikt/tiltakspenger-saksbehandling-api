package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.behandlePåNytt

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.instanceOf
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandlingsstatus
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Søknadsbehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.SøknadsbehandlingResultat
import no.nav.tiltakspenger.saksbehandling.behandling.domene.SøknadsbehandlingType
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContext
import no.nav.tiltakspenger.saksbehandling.infra.route.routes
import no.nav.tiltakspenger.saksbehandling.infra.setup.jacksonSerialization
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.behandleSøknadPåNytt
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandling
import org.junit.jupiter.api.Test

internal class BehandleSøknadPåNyttTest {
    @Test
    fun `kan behandle avslått søknad på nytt`() = runTest {
        with(TestApplicationContext()) {
            val tac = this
            testApplication {
                application {
                    jacksonSerialization()
                    routing { routes(tac) }
                }
                val (sak, søknad, behandling) = this.iverksettSøknadsbehandling(
                    tac = tac,
                    resultat = SøknadsbehandlingType.AVSLAG,
                )
                behandling.virkningsperiode.shouldNotBeNull()
                behandling.status shouldBe Behandlingsstatus.VEDTATT
                behandling.resultat is SøknadsbehandlingResultat.Avslag

                val (_, _, nyBehandling, _) = this.behandleSøknadPåNytt(
                    tac = tac,
                    sak = sak,
                    søknad = søknad,
                    fnr = behandling.fnr,
                    virkingsperiode = behandling.virkningsperiode,
                )

                nyBehandling.shouldNotBeNull()
                nyBehandling.status shouldBe Behandlingsstatus.UNDER_BEHANDLING
                nyBehandling shouldBe instanceOf<Søknadsbehandling>()
                nyBehandling.søknad.id shouldBe behandling.søknad.id
            }
        }
    }
}
