package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.brev

import io.kotest.matchers.shouldBe
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import no.nav.tiltakspenger.libs.dato.februar
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.dato.mars
import no.nav.tiltakspenger.libs.periodisering.til
import no.nav.tiltakspenger.libs.periodisering.toDTO
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.barnetillegg.BarnetilleggPeriodeDTO
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.BehandlingResultatDTO
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContext
import no.nav.tiltakspenger.saksbehandling.infra.route.routes
import no.nav.tiltakspenger.saksbehandling.infra.setup.jacksonSerialization
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.forhåndsvisVedtaksbrevForBehandlingId
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSøknadsbehandlingUnderBehandling
import org.junit.jupiter.api.Test

internal class ForhåndsvisInnvilgetSøknadsbehandlingVedtaksbrevTest {
    @Test
    fun `kan forhåndsvise vedtaksbrev for innvilget søknadsbehandling uten barnetillegg`() {
        with(TestApplicationContext()) {
            val tac = this
            testApplication {
                application {
                    jacksonSerialization()
                    routing { routes(tac) }
                }
                val (sak, _, behandling) = opprettSøknadsbehandlingUnderBehandling(tac)
                val behandlingId = behandling.id
                val fritekstTilVedtaksbrev = "some_tekst"
                val (oppdatertSak, oppdatertBehandling, responseJson) = forhåndsvisVedtaksbrevForBehandlingId(
                    tac = tac,
                    sakId = sak.id,
                    behandlingId = behandlingId,
                    fritekstTilVedtaksbrev = fritekstTilVedtaksbrev,
                    virkningsperiode = 1.januar(2025) til 31.mars(2025),
                    stansDato = null,
                    valgteHjemler = null,
                    barnetillegg = null,
                    resultat = BehandlingResultatDTO.INNVILGELSE,
                    avslagsgrunner = null,
                )
                responseJson shouldBe "pdf"
            }
        }
    }

    @Test
    fun `kan forhåndsvise vedtaksbrev for innvilget søknadsbehandling med ett barn`() {
        with(TestApplicationContext()) {
            val tac = this
            testApplication {
                application {
                    jacksonSerialization()
                    routing { routes(tac) }
                }
                val (sak, _, behandling) = opprettSøknadsbehandlingUnderBehandling(tac)
                val behandlingId = behandling.id
                val fritekstTilVedtaksbrev = "some_tekst"
                val virkningsperiode = 1.januar(2025) til 31.mars(2025)
                val (oppdatertSak, oppdatertBehandling, responseJson) = forhåndsvisVedtaksbrevForBehandlingId(
                    tac = tac,
                    sakId = sak.id,
                    behandlingId = behandlingId,
                    fritekstTilVedtaksbrev = fritekstTilVedtaksbrev,
                    virkningsperiode = virkningsperiode,
                    stansDato = null,
                    valgteHjemler = null,
                    barnetillegg = listOf(BarnetilleggPeriodeDTO(antallBarn = 1, periode = virkningsperiode.toDTO())),
                    resultat = BehandlingResultatDTO.INNVILGELSE,
                    avslagsgrunner = null,
                )
                responseJson shouldBe "pdf"
            }
        }
    }

    @Test
    fun `kan forhåndsvise vedtaksbrev for innvilget søknadsbehandling med to barn`() {
        with(TestApplicationContext()) {
            val tac = this
            testApplication {
                application {
                    jacksonSerialization()
                    routing { routes(tac) }
                }
                val (sak, _, behandling) = opprettSøknadsbehandlingUnderBehandling(tac)
                val behandlingId = behandling.id
                val fritekstTilVedtaksbrev = "some_tekst"
                val (_, _, responseJson) = forhåndsvisVedtaksbrevForBehandlingId(
                    tac = tac,
                    sakId = sak.id,
                    behandlingId = behandlingId,
                    fritekstTilVedtaksbrev = fritekstTilVedtaksbrev,
                    virkningsperiode = 1.januar(2025) til 31.mars(2025),
                    stansDato = null,
                    valgteHjemler = null,
                    barnetillegg = listOf(
                        BarnetilleggPeriodeDTO(
                            antallBarn = 1,
                            periode = (1 til 31.januar(2025)).toDTO(),
                        ),
                        BarnetilleggPeriodeDTO(antallBarn = 1, periode = (1.februar(2025) til 31.mars(2025)).toDTO()),
                    ),
                    resultat = BehandlingResultatDTO.INNVILGELSE,
                    avslagsgrunner = null,
                )
                responseJson shouldBe "pdf"
            }
        }
    }
}
