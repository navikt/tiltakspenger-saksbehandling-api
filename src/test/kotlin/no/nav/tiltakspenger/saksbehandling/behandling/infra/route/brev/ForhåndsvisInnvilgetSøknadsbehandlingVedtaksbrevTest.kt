package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.brev

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.dato.mars
import no.nav.tiltakspenger.libs.periode.PeriodeDTO
import no.nav.tiltakspenger.libs.periode.til
import no.nav.tiltakspenger.libs.periode.toDTO
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.barnetillegg.BarnKildeDTO
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.barnetillegg.OppdaterBarnMedBarnetilleggDTO
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.barnetillegg.OppdaterBarnetilleggDTO
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.InnvilgelsesperiodeDTO
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.RammebehandlingResultatTypeDTO
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContext
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.tiltaksdeltakelse
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.forhåndsvisVedtaksbrevForBehandlingId
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSøknadsbehandlingUnderBehandling
import org.junit.jupiter.api.Test

class ForhåndsvisInnvilgetSøknadsbehandlingVedtaksbrevTest {
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
                        periode = behandling.saksopplysninger.tiltaksdeltakelser.first().lukketPeriode!!.toDTO(),
                        antallDagerPerMeldeperiode = 10,
                        internDeltakelseId = behandling.saksopplysninger.tiltaksdeltakelser.first().internDeltakelseId.toString(),
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
            val innvilgelsesperiode = 1.januar(2025) til 31.mars(2025)
            val (sak, _, behandling) = opprettSøknadsbehandlingUnderBehandling(
                tac,
                tiltaksdeltakelse = tiltaksdeltakelse(innvilgelsesperiode),
            )

            val behandlingId = behandling.id
            val fritekstTilVedtaksbrev = "some_tekst"
            val (_, _, responseJson) = forhåndsvisVedtaksbrevForBehandlingId(
                tac = tac,
                sakId = sak.id,
                behandlingId = behandlingId,
                fritekstTilVedtaksbrev = fritekstTilVedtaksbrev,
                innvilgelsesperioder = listOf(
                    InnvilgelsesperiodeDTO(
                        periode = innvilgelsesperiode.toDTO(),
                        antallDagerPerMeldeperiode = 10,
                        internDeltakelseId = behandling.saksopplysninger.tiltaksdeltakelser.first().internDeltakelseId.toString(),
                    ),
                ),
                barnetillegg = OppdaterBarnetilleggDTO(
                    barn = listOf(barnLagtTilAvSaksbehandler(fornavn = "Ola", perioder = null)),
                    begrunnelse = null,
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
            val innvilgelsesperiode = (1.januar(2025) til 31.mars(2025))
            val (sak, _, behandling) = opprettSøknadsbehandlingUnderBehandling(
                tac,
                tiltaksdeltakelse = tiltaksdeltakelse(innvilgelsesperiode),
            )
            val behandlingId = behandling.id
            val fritekstTilVedtaksbrev = "some_tekst"
            val (_, _, responseJson) = forhåndsvisVedtaksbrevForBehandlingId(
                tac = tac,
                sakId = sak.id,
                behandlingId = behandlingId,
                fritekstTilVedtaksbrev = fritekstTilVedtaksbrev,
                innvilgelsesperioder = listOf(
                    InnvilgelsesperiodeDTO(
                        periode = innvilgelsesperiode.toDTO(),
                        antallDagerPerMeldeperiode = 10,
                        internDeltakelseId = behandling.saksopplysninger.tiltaksdeltakelser.first().internDeltakelseId.toString(),
                    ),
                ),
                barnetillegg = OppdaterBarnetilleggDTO(
                    barn = listOf(
                        barnLagtTilAvSaksbehandler(fornavn = "Ola", perioder = listOf((1 til 31.januar(2025)).toDTO())),
                        barnLagtTilAvSaksbehandler(fornavn = "Kari", perioder = null),
                    ),
                    begrunnelse = null,
                ),
                resultat = RammebehandlingResultatTypeDTO.INNVILGELSE,
            )
            responseJson shouldBe "pdf"
        }
    }

    @Test
    fun `kan forhåndsvise vedtaksbrev med barnetillegg lagret som antall barn per periode`() {
        withTestApplicationContext { tac ->
            val innvilgelsesperiode = (1.januar(2025) til 31.mars(2025))
            val (sak, _, behandling) = opprettSøknadsbehandlingUnderBehandling(
                tac,
                tiltaksdeltakelse = tiltaksdeltakelse(innvilgelsesperiode),
            )
            val (_, _, responseJson) = forhåndsvisVedtaksbrevForBehandlingId(
                tac = tac,
                sakId = sak.id,
                behandlingId = behandling.id,
                innvilgelsesperioder = listOf(
                    InnvilgelsesperiodeDTO(
                        periode = innvilgelsesperiode.toDTO(),
                        antallDagerPerMeldeperiode = 10,
                        internDeltakelseId = behandling.saksopplysninger.tiltaksdeltakelser.first().internDeltakelseId.toString(),
                    ),
                ),
                barnetilleggJson = """{"begrunnelse": null, "perioder": [{"antallBarn": 1, "periode": {"fraOgMed": "2025-01-01", "tilOgMed": "2025-01-31"}}]}""",
                resultat = RammebehandlingResultatTypeDTO.INNVILGELSE,
            )
            responseJson shouldBe "pdf"
        }
    }

    private fun barnLagtTilAvSaksbehandler(fornavn: String, perioder: List<PeriodeDTO>?) = OppdaterBarnMedBarnetilleggDTO(
        fnr = null,
        fornavn = fornavn,
        mellomnavn = null,
        etternavn = "Nordmann",
        fødselsdato = 1.januar(2020),
        kilde = BarnKildeDTO.SAKSBEHANDLER,
        søknadId = null,
        perioder = perioder,
    )
}
