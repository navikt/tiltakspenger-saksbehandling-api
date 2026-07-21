package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.brev

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.dato.februar
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.dato.mars
import no.nav.tiltakspenger.libs.periode.til
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.HjemmelForStansDTO
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.RammebehandlingResultatTypeDTO
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContext
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.innvilgelsesperioder
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.forhåndsvisVedtaksbrevForBehandlingId
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgStartRevurderingStans
import org.junit.jupiter.api.Test

internal class ForhåndsvisStansVedtaksbrevTest {
    @Test
    fun `kan forhåndsvise vedtaksbrev for stans under behandling`() {
        withTestApplicationContext { tac ->
            val (sak, _, _, revurdering) = iverksettSøknadsbehandlingOgStartRevurderingStans(
                tac = tac,
                innvilgelsesperioder = innvilgelsesperioder(1.januar(2025) til 31.mars(2025)),
            )
            val behandlingId = revurdering.id
            val fritekstTilVedtaksbrev = "some_tekst"
            val (_, _, responseJson) = forhåndsvisVedtaksbrevForBehandlingId(
                tac = tac,
                sakId = sak.id,
                behandlingId = behandlingId,
                fritekstTilVedtaksbrev = fritekstTilVedtaksbrev,
                vedtaksperiode = null,
                stansFraOgMed = 1.februar(2025),
                valgteHjemler = listOf(HjemmelForStansDTO.LønnFraAndre),
                barnetillegg = null,
                resultat = RammebehandlingResultatTypeDTO.STANS,
                avslagsgrunner = null,
            )
            responseJson shouldBe "pdf"
        }
    }
}
