package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.brev

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.dato.februar
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.dato.mars
import no.nav.tiltakspenger.libs.periodisering.til
import no.nav.tiltakspenger.libs.periodisering.toDTO
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.barnetillegg.BarnetilleggPeriodeDTO
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.InnvilgelsesperiodeDTO
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.RammebehandlingResultatTypeDTO
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContext
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.forhåndsvisVedtaksbrevForBehandlingId
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSøknadsbehandlingUnderBehandling
import org.junit.jupiter.api.Test

internal class ForhåndsvisInnvilgetSøknadsbehandlingVedtaksbrevTest {
    @Test
    fun `kan forhåndsvise vedtaksbrev for innvilget søknadsbehandling uten barnetillegg`() {
        withTestApplicationContext { tac ->
            val (sak, _, behandling) = opprettSøknadsbehandlingUnderBehandling(tac)
            val behandlingId = behandling.id
            val fritekstTilVedtaksbrev = "some_tekst"
            val (_, _, responseJson) = forhåndsvisVedtaksbrevForBehandlingId(
                tac = tac,
                sakId = sak.id,
                behandlingId = behandlingId,
                fritekstTilVedtaksbrev = fritekstTilVedtaksbrev,
                innvilgelsesperioder = listOf(
                    InnvilgelsesperiodeDTO(
                        periode = (1.januar(2025) til 31.mars(2025)).toDTO(),
                        antallDagerPerMeldeperiode = 10,
                        tiltaksdeltakelseId = behandling.saksopplysninger.tiltaksdeltakelser.first().eksternDeltakelseId,
                        internDeltakelseId = behandling.saksopplysninger.tiltaksdeltakelser.first().internDeltakelseId?.toString(),
                    ),
                ),
                resultat = RammebehandlingResultatTypeDTO.INNVILGELSE,
            )
            responseJson shouldBe "pdf"
        }
    }

    @Test
    fun `kan forhåndsvise vedtaksbrev for innvilget søknadsbehandling med ett barn`() {
        withTestApplicationContext { tac ->
            val (sak, _, behandling) = opprettSøknadsbehandlingUnderBehandling(tac)
            val behandlingId = behandling.id
            val fritekstTilVedtaksbrev = "some_tekst"
            val innvilgelsesperiode = 1.januar(2025) til 31.mars(2025)
            val (_, _, responseJson) = forhåndsvisVedtaksbrevForBehandlingId(
                tac = tac,
                sakId = sak.id,
                behandlingId = behandlingId,
                fritekstTilVedtaksbrev = fritekstTilVedtaksbrev,
                innvilgelsesperioder = listOf(
                    InnvilgelsesperiodeDTO(
                        periode = innvilgelsesperiode.toDTO(),
                        antallDagerPerMeldeperiode = 10,
                        tiltaksdeltakelseId = behandling.saksopplysninger.tiltaksdeltakelser.first().eksternDeltakelseId,
                        internDeltakelseId = behandling.saksopplysninger.tiltaksdeltakelser.first().internDeltakelseId?.toString(),
                    ),
                ),
                barnetillegg = listOf(
                    BarnetilleggPeriodeDTO(
                        antallBarn = 1,
                        periode = innvilgelsesperiode.toDTO(),
                    ),
                ),
                resultat = RammebehandlingResultatTypeDTO.INNVILGELSE,
                avslagsgrunner = null,
            )
            responseJson shouldBe "pdf"
        }
    }

    @Test
    fun `kan forhåndsvise vedtaksbrev for innvilget søknadsbehandling med to barn`() {
        withTestApplicationContext { tac ->
            val (sak, _, behandling) = opprettSøknadsbehandlingUnderBehandling(tac)
            val behandlingId = behandling.id
            val fritekstTilVedtaksbrev = "some_tekst"
            val (_, _, responseJson) = forhåndsvisVedtaksbrevForBehandlingId(
                tac = tac,
                sakId = sak.id,
                behandlingId = behandlingId,
                fritekstTilVedtaksbrev = fritekstTilVedtaksbrev,
                innvilgelsesperioder = listOf(
                    InnvilgelsesperiodeDTO(
                        periode = (1.januar(2025) til 31.mars(2025)).toDTO(),
                        antallDagerPerMeldeperiode = 10,
                        tiltaksdeltakelseId = behandling.saksopplysninger.tiltaksdeltakelser.first().eksternDeltakelseId,
                        internDeltakelseId = behandling.saksopplysninger.tiltaksdeltakelser.first().internDeltakelseId?.toString(),
                    ),
                ),
                barnetillegg = listOf(
                    BarnetilleggPeriodeDTO(
                        antallBarn = 1,
                        periode = (1 til 31.januar(2025)).toDTO(),
                    ),
                    BarnetilleggPeriodeDTO(antallBarn = 1, periode = (1.februar(2025) til 31.mars(2025)).toDTO()),
                ),
                resultat = RammebehandlingResultatTypeDTO.INNVILGELSE,
            )
            responseJson shouldBe "pdf"
        }
    }
}
