package no.nav.tiltakspenger.saksbehandling.service

import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.common.TestApplicationContext
import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.objectmothers.ObjectMother
import no.nav.tiltakspenger.objectmothers.ObjectMother.beslutter
import no.nav.tiltakspenger.objectmothers.ObjectMother.saksbehandler
import no.nav.tiltakspenger.objectmothers.ObjectMother.saksbehandlerUtenTilgang
import no.nav.tiltakspenger.objectmothers.førstegangsbehandlingIverksatt
import no.nav.tiltakspenger.objectmothers.førstegangsbehandlingTilBeslutter
import no.nav.tiltakspenger.objectmothers.førstegangsbehandlingUnderBeslutning
import no.nav.tiltakspenger.saksbehandling.domene.behandling.OppdaterTilleggstekstBrevKommando
import no.nav.tiltakspenger.saksbehandling.domene.behandling.OppdaterVurderingsperiodeKommando
import no.nav.tiltakspenger.saksbehandling.domene.behandling.TilleggstekstBrev
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertNull

internal class BehandlingServiceTest {
    @Test
    fun `må ha beslutterrolle for å ta behandling som er til beslutning`() = runTest {
        with(TestApplicationContext()) {
            val sak = this.førstegangsbehandlingTilBeslutter()
            val behandlingId = sak.førstegangsbehandling.id

            this.behandlingContext.behandlingService.taBehandling(
                behandlingId,
                saksbehandlerUtenTilgang(),
                correlationId = CorrelationId.generate(),
            ).shouldBeLeft()

            val sakEksempel2Test = this.sakContext.sakRepo.hentForSakId(sak.id)!!
            println(sakEksempel2Test.førstegangsbehandling)

            this.behandlingContext.behandlingService.taBehandling(
                behandlingId,
                beslutter(),
                correlationId = CorrelationId.generate(),
            ).shouldBeRight()
        }
    }

    @Test
    fun `sjekk at man ikke kan sende tilbake uten beslutter rolle`() = runTest {
        with(TestApplicationContext()) {
            val sak = this.førstegangsbehandlingTilBeslutter()
            val behandlingId = sak.førstegangsbehandling.id
            val beslutter = beslutter()
            this.behandlingContext.behandlingService.taBehandling(
                behandlingId,
                beslutter,
                correlationId = CorrelationId.generate(),
            )

            this.behandlingContext.behandlingService.sendTilbakeTilSaksbehandler(
                behandlingId,
                saksbehandlerUtenTilgang(),
                "begrunnelse",
                correlationId = CorrelationId.generate(),
            ).shouldBeLeft()

            this.behandlingContext.behandlingService.sendTilbakeTilSaksbehandler(
                behandlingId,
                beslutter,
                "begrunnelse",
                correlationId = CorrelationId.generate(),
            ).shouldBeRight()
        }
    }

    @Nested
    inner class `Oppdatering av vurderingsperiode` {
        private fun lagKommando(
            behandlingId: BehandlingId,
            saksbehandler: Saksbehandler = saksbehandler(),
            periode: Periode = ObjectMother.vurderingsperiode(),
        ): OppdaterVurderingsperiodeKommando =
            OppdaterVurderingsperiodeKommando(
                behandlingId = behandlingId,
                correlationId = CorrelationId.generate(),
                saksbehandler = saksbehandler,
                periode = periode,
            )

        @Test
        fun `krever saksbehandler rolle`() = runTest {
            with(TestApplicationContext()) {
                val behandlingService = this.behandlingContext.behandlingService
                val sak = this.førstegangsbehandlingTilBeslutter()
                val behandlingId = sak.førstegangsbehandling.id
                behandlingService
                    .oppdaterVurderingsPeriodeForBehandling(lagKommando(behandlingId))
                    .shouldBeRight()

                assertThrows<IllegalArgumentException> {
                    behandlingService
                        .oppdaterVurderingsPeriodeForBehandling(
                            lagKommando(
                                behandlingId = behandlingId,
                                saksbehandler = saksbehandlerUtenTilgang(),
                            ),
                        )
                        .shouldBeLeft()
                }
            }
        }

        @Test
        fun `kan ikke endres etter at behandling er tildelt beslutter`() = runTest {
            with(TestApplicationContext()) {
                val behandlingService = this.behandlingContext.behandlingService
                val sak = this.førstegangsbehandlingUnderBeslutning()
                val behandlingId = sak.førstegangsbehandling.id
                behandlingService
                    .oppdaterVurderingsPeriodeForBehandling(lagKommando(behandlingId))
                    .shouldBeLeft()
            }
        }

        @Test
        fun `kan ikke endres etter at behandling er vedtatt`() = runTest {
            with(TestApplicationContext()) {
                val behandlingService = this.behandlingContext.behandlingService
                val sak = this.førstegangsbehandlingIverksatt()
                val behandlingId = sak.førstegangsbehandling.id
                behandlingService
                    .oppdaterVurderingsPeriodeForBehandling(lagKommando(behandlingId = behandlingId))
                    .shouldBeLeft()
            }
        }

        @Test
        fun `periode kan ikke utvides`() = runTest {
            with(TestApplicationContext()) {
                val behandlingService = this.behandlingContext.behandlingService
                val sak = this.førstegangsbehandlingTilBeslutter()
                val behandlingId = sak.førstegangsbehandling.id
                // fraOgMed utvides
                behandlingService.oppdaterVurderingsPeriodeForBehandling(
                    lagKommando(
                        behandlingId = behandlingId,
                        saksbehandler = saksbehandler(),
                        periode = Periode(
                            sak.førstegangsbehandling.vurderingsperiode.fraOgMed.minusDays(1),
                            sak.førstegangsbehandling.vurderingsperiode.tilOgMed,
                        ),
                    ),
                ).shouldBeLeft()
                // tilOgMed utvides
                behandlingService.oppdaterVurderingsPeriodeForBehandling(
                    lagKommando(
                        behandlingId = behandlingId,
                        saksbehandler = saksbehandler(),
                        periode = Periode(
                            sak.førstegangsbehandling.vurderingsperiode.fraOgMed,
                            sak.førstegangsbehandling.vurderingsperiode.tilOgMed.plusDays(1),
                        ),
                    ),
                ).shouldBeLeft()
                // Utvidelse begge veier
                behandlingService.oppdaterVurderingsPeriodeForBehandling(
                    lagKommando(
                        behandlingId = behandlingId,
                        saksbehandler = saksbehandler(),
                        periode = Periode(
                            sak.førstegangsbehandling.vurderingsperiode.fraOgMed.minusDays(1),
                            sak.førstegangsbehandling.vurderingsperiode.tilOgMed.plusDays(1),
                        ),
                    ),
                ).shouldBeLeft()
            }
        }
    }

    @Nested
    inner class `Oppdatering av tilleggstekst i  brev for behandling` {
        private fun lagKommando(
            behandlingId: BehandlingId,
            saksbehandler: Saksbehandler = saksbehandler(),
        ): OppdaterTilleggstekstBrevKommando =
            OppdaterTilleggstekstBrevKommando(
                behandlingId = behandlingId,
                correlationId = CorrelationId.generate(),
                saksbehandler = saksbehandler,
                subsumsjon = TilleggstekstBrev.Subsumsjon.TILTAKSDELTAGELSE,
            )

        @Test
        fun `krever saksbehandler rolle`() = runTest {
            with(TestApplicationContext()) {
                val behandlingService = this.behandlingContext.behandlingService
                val sak = this.førstegangsbehandlingTilBeslutter()
                val behandlingId = sak.førstegangsbehandling.id
                behandlingService
                    .oppdaterTilleggstekstBrevPåBehandling(lagKommando(behandlingId))
                    .shouldBeRight()

                assertThrows<IllegalArgumentException> {
                    behandlingService
                        .oppdaterTilleggstekstBrevPåBehandling(
                            lagKommando(
                                behandlingId,
                                saksbehandler = saksbehandlerUtenTilgang(),
                            ),
                        )
                        .shouldBeLeft()
                }
            }
        }

        @Test
        fun `kan ikke endres etter at behandling er tildelt beslutter`() = runTest {
            with(TestApplicationContext()) {
                val behandlingService = this.behandlingContext.behandlingService
                val sak = this.førstegangsbehandlingUnderBeslutning()
                val behandlingId = sak.førstegangsbehandling.id
                behandlingService
                    .oppdaterTilleggstekstBrevPåBehandling(lagKommando(behandlingId))
                    .shouldBeLeft()
            }
        }

        @Test
        fun `kan ikke endres etter at behandling er vedtatt`() = runTest {
            with(TestApplicationContext()) {
                val behandlingService = this.behandlingContext.behandlingService
                val sak = this.førstegangsbehandlingIverksatt()
                val behandlingId = sak.førstegangsbehandling.id
                behandlingService
                    .oppdaterTilleggstekstBrevPåBehandling(lagKommando(behandlingId))
                    .shouldBeLeft()
            }
        }

        @Test
        fun `gjør at tilleggstekst til brev blir oppdatert`() = runTest {
            with(TestApplicationContext()) {
                val behandlingService = this.behandlingContext.behandlingService
                val sak = this.førstegangsbehandlingTilBeslutter()
                val behandlingId = sak.førstegangsbehandling.id
                val saksbehandler = saksbehandler()

                val behandlingFørOppdatering =
                    behandlingService.hentBehandling(behandlingId, saksbehandler, CorrelationId.generate())
                assertNull(
                    behandlingFørOppdatering.tilleggstekstBrev,
                    "Forventet ingen begrunnelse før den blir oppdatert",
                )

                val behandling = behandlingService
                    .oppdaterTilleggstekstBrevPåBehandling(lagKommando(behandlingId))
                    .shouldBeRight()

                assertEquals(
                    TilleggstekstBrev.Subsumsjon.TILTAKSDELTAGELSE,
                    behandling.tilleggstekstBrev?.subsumsjon,
                    "Subsumsjon for tilleggstekst til brev",
                )
            }
        }
    }
}
