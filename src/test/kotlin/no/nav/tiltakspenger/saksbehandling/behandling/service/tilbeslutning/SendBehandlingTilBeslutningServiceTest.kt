package no.nav.tiltakspenger.saksbehandling.behandling.service.tilbeslutning

import arrow.core.nonEmptySetOf
import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.getOrFail
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Avslagsgrunnlag
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus
import no.nav.tiltakspenger.saksbehandling.behandling.domene.SendBehandlingTilBeslutningKommando
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.SøknadsbehandlingsresultatType
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContext
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.saksbehandler
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSøknadsbehandlingUnderBehandlingMedInnvilgelse
import org.junit.jupiter.api.Test

class SendBehandlingTilBeslutningServiceTest {

    @Test
    fun `kan sende søknadsbehandling til beslutning`() {
        withTestApplicationContext { tac ->
            val saksbehandler = saksbehandler()

            val (sak, _, behandling) = opprettSøknadsbehandlingUnderBehandlingMedInnvilgelse(
                tac = tac,
                saksbehandler = saksbehandler,
            )

            behandling.status shouldBe Rammebehandlingsstatus.UNDER_BEHANDLING

            val kommando = SendBehandlingTilBeslutningKommando(
                sakId = sak.id,
                behandlingId = behandling.id,
                saksbehandler = saksbehandler,
                correlationId = CorrelationId.generate(),
            )

            val oppdatertBehandling = tac.behandlingContext.sendBehandlingTilBeslutningService.sendTilBeslutning(
                kommando,
            ).getOrFail().second

            val oppdatertSak = tac.sakContext.sakService.hentForSakId(
                sak.id,
            )

            oppdatertBehandling.status shouldBe Rammebehandlingsstatus.KLAR_TIL_BESLUTNING
            oppdatertSak.rammebehandlinger.any { it.id == oppdatertBehandling.id } shouldBe true
        }
    }

    @Test
    fun `kan innvilge selv om det en behandling med periode som tilstøter eller overlapper`() {
        withTestApplicationContext { tac ->
            val saksbehandler = saksbehandler()
            val fnr = Fnr.fromString("12345678911")

            val (sak, _, behandling) = opprettSøknadsbehandlingUnderBehandlingMedInnvilgelse(
                tac = tac,
                fnr = fnr,
                saksbehandler = saksbehandler,
                clock = tac.clock,
            )

            val behandlingMedAvslag = ObjectMother.oppdatertSøknadsbehandling(
                sakId = sak.id,
                saksnummer = sak.saksnummer,
                fnr = fnr,
                resultat = SøknadsbehandlingsresultatType.AVSLAG,
                avslagsgrunner = nonEmptySetOf(Avslagsgrunnlag.Alder),
                clock = tac.clock,
            )

            tac.behandlingContext.rammebehandlingRepo.lagre(behandlingMedAvslag)

            val kommando = SendBehandlingTilBeslutningKommando(
                sakId = sak.id,
                behandlingId = behandling.id,
                saksbehandler = saksbehandler,
                correlationId = CorrelationId.generate(),
            )

            val innvilgetBehandlingSendtTilBeslutning = tac.behandlingContext.sendBehandlingTilBeslutningService.sendTilBeslutning(
                kommando,
            ).getOrFail().second

            val oppdatertSak = tac.sakContext.sakService.hentForSakId(
                sak.id,
            )

            innvilgetBehandlingSendtTilBeslutning.status shouldBe Rammebehandlingsstatus.KLAR_TIL_BESLUTNING
            oppdatertSak.rammebehandlinger.any { it.id == innvilgetBehandlingSendtTilBeslutning.id } shouldBe true
        }
    }
}
