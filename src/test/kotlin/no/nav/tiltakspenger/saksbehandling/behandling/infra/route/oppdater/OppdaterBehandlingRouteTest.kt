package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.oppdater

import io.kotest.matchers.shouldBe
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import no.nav.tiltakspenger.libs.periodisering.SammenhengendePeriodisering
import no.nav.tiltakspenger.libs.periodisering.toDTO
import no.nav.tiltakspenger.saksbehandling.barnetillegg.AntallBarn
import no.nav.tiltakspenger.saksbehandling.behandling.domene.AntallDagerForMeldeperiode
import no.nav.tiltakspenger.saksbehandling.behandling.domene.BegrunnelseVilkårsvurdering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.MAKS_DAGER_MED_TILTAKSPENGER_FOR_PERIODE
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.barnetillegg.toBarnetilleggDTO
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.OppdaterSøknadsbehandlingDTO
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContext
import no.nav.tiltakspenger.saksbehandling.infra.route.routes
import no.nav.tiltakspenger.saksbehandling.infra.setup.jacksonSerialization
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.barnetillegg
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSøknadsbehandlingUnderBehandling
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.infra.route.TiltaksdeltakelsePeriodeDTO
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.infra.route.toDTO
import org.junit.jupiter.api.Test

class OppdaterBehandlingRouteTest {
    @Test
    fun `kan oppdatere innvilget søknadsbehandling`() {
        with(TestApplicationContext()) {
            val tac = this
            testApplication {
                application {
                    jacksonSerialization()
                    routing { routes(tac) }
                }

                val (sak, _, behandling) = opprettSøknadsbehandlingUnderBehandling(tac)

                val tiltaksdeltagelse = behandling.saksopplysninger.tiltaksdeltagelse.first()
                val nyInnvilgelsesperiode = tiltaksdeltagelse.periode!!.minusTilOgMed(1)

                val barnetillegg = barnetillegg(
                    begrunnelse = BegrunnelseVilkårsvurdering("barnetillegg begrunnelse"),
                    periode = nyInnvilgelsesperiode,
                    antallBarn = AntallBarn(1),
                )

                val antallDager = SammenhengendePeriodisering(
                    AntallDagerForMeldeperiode(MAKS_DAGER_MED_TILTAKSPENGER_FOR_PERIODE),
                    nyInnvilgelsesperiode,
                )

                oppdaterBehandling(
                    tac = tac,
                    sakId = sak.id,
                    behandlingId = behandling.id,
                    oppdaterBehandlingDTO = OppdaterSøknadsbehandlingDTO.Innvilgelse(
                        fritekstTilVedtaksbrev = "ny brevtekst",
                        begrunnelseVilkårsvurdering = "ny begrunnelse",
                        valgteTiltaksdeltakelser = listOf(
                            TiltaksdeltakelsePeriodeDTO(
                                eksternDeltagelseId = tiltaksdeltagelse.eksternDeltagelseId,
                                periode = tiltaksdeltagelse.periode!!.toDTO(),
                            ),
                        ),
                        innvilgelsesperiode = nyInnvilgelsesperiode.toDTO(),
                        barnetillegg = barnetillegg.toBarnetilleggDTO(),
                        antallDagerPerMeldeperiodeForPerioder = antallDager.toDTO(),
                    ),
                )

                val oppdatertBehandling = tac.behandlingContext.behandlingRepo.hent(behandling.id)

                oppdatertBehandling.fritekstTilVedtaksbrev!!.verdi shouldBe "ny brevtekst"
                oppdatertBehandling.begrunnelseVilkårsvurdering!!.verdi shouldBe "ny begrunnelse"
                oppdatertBehandling.virkningsperiode shouldBe nyInnvilgelsesperiode
                oppdatertBehandling.barnetillegg shouldBe barnetillegg
                oppdatertBehandling.antallDagerPerMeldeperiode shouldBe antallDager
            }
        }
    }
}
