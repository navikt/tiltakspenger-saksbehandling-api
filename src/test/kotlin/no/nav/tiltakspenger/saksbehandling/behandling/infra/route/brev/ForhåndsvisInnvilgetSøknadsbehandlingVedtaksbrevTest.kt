package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.brev

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.dato.februar
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.dato.mars
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.libs.periode.PeriodeDTO
import no.nav.tiltakspenger.libs.periode.til
import no.nav.tiltakspenger.libs.periode.toDTO
import no.nav.tiltakspenger.libs.periodisering.PeriodeMedVerdi
import no.nav.tiltakspenger.saksbehandling.barnetillegg.AntallBarn
import no.nav.tiltakspenger.saksbehandling.barnetillegg.lagreBarnetilleggSomV1
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.barnetillegg.BarnKildeDTO
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.barnetillegg.OppdaterBarnMedBarnetilleggDTO
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.barnetillegg.OppdaterBarnetilleggDTO
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.InnvilgelsesperiodeDTO
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.RammebehandlingResultatTypeDTO
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContext
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.barnetillegg
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.innvilgelsesperioder
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.tiltaksdeltakelse
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.forhåndsvisVedtaksbrevForBehandlingId
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSøknadsbehandlingUnderBehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSøknadsbehandlingUnderBehandlingMedInnvilgelse
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.sendSøknadsbehandlingTilBeslutning
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.taRammebehandlinger
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
            tac.forhåndsvistBarnetillegg.last()!!.perioderMedVerdi.toList() shouldBe listOf(
                PeriodeMedVerdi(AntallBarn(2), 1.januar(2025) til 31.januar(2025)),
                PeriodeMedVerdi(AntallBarn(1), 1.februar(2025) til 31.mars(2025)),
            )
        }
    }

    @Test
    fun `beslutter får barnetillegget som er lagret som antall barn per periode, uavhengig av skjemaet`() {
        withTestApplicationContextAndPostgres { tac ->
            val innvilgelsesperiode = 1.januar(2025) til 31.mars(2025)
            val tiltaksdeltakelse = tiltaksdeltakelse(innvilgelsesperiode)
            val (sak, _, behandlingId) = sendSøknadsbehandlingTilBeslutning(
                tac = tac,
                tiltaksdeltakelse = tiltaksdeltakelse,
                innvilgelsesperioder = innvilgelsesperioder(innvilgelsesperiode, tiltaksdeltakelse),
                barnetillegg = barnetillegg(periode = 1.januar(2025) til 31.januar(2025), antallBarn = AntallBarn(2)),
            )
            tac.lagreBarnetilleggSomV1(behandlingId)
            taRammebehandlinger(tac, listOf(sak.id to behandlingId), saksbehandler = ObjectMother.beslutter())

            forhåndsvisVedtaksbrevForBehandlingId(
                tac = tac,
                sakId = sak.id,
                behandlingId = behandlingId,
                saksbehandler = ObjectMother.beslutter(),
                innvilgelsesperioder = innvilgelsesperioderDTO(innvilgelsesperiode, tiltaksdeltakelse.internDeltakelseId.toString()),
                barnetillegg = OppdaterBarnetilleggDTO(barn = emptyList(), begrunnelse = null),
                resultat = RammebehandlingResultatTypeDTO.INNVILGELSE,
            )

            tac.forhåndsvistBarnetillegg.last()!!.perioderMedVerdi.toList() shouldBe listOf(
                PeriodeMedVerdi(AntallBarn(2), 1.januar(2025) til 31.januar(2025)),
                PeriodeMedVerdi(AntallBarn.ZERO, 1.februar(2025) til 31.mars(2025)),
            )
        }
    }

    @Test
    fun `saksbehandler som ikke eier behandlingen får det lagrede barnetillegget`() {
        withTestApplicationContext { tac ->
            val innvilgelsesperiode = 1.januar(2025) til 31.mars(2025)
            val tiltaksdeltakelse = tiltaksdeltakelse(innvilgelsesperiode)
            val (sak, _, behandling) = opprettSøknadsbehandlingUnderBehandlingMedInnvilgelse(
                tac = tac,
                tiltaksdeltakelse = tiltaksdeltakelse,
                innvilgelsesperioder = innvilgelsesperioder(innvilgelsesperiode, tiltaksdeltakelse),
                barnetillegg = barnetillegg(periode = 1.januar(2025) til 31.januar(2025), antallBarn = AntallBarn(1)),
            )

            forhåndsvisVedtaksbrevForBehandlingId(
                tac = tac,
                sakId = sak.id,
                behandlingId = behandling.id,
                saksbehandler = ObjectMother.saksbehandler(navIdent = "Z99999"),
                innvilgelsesperioder = innvilgelsesperioderDTO(innvilgelsesperiode, tiltaksdeltakelse.internDeltakelseId.toString()),
                barnetillegg = OppdaterBarnetilleggDTO(
                    barn = listOf(barnLagtTilAvSaksbehandler(fornavn = "Ola", perioder = null)),
                    begrunnelse = null,
                ),
                resultat = RammebehandlingResultatTypeDTO.INNVILGELSE,
            )

            tac.forhåndsvistBarnetillegg.last()!!.perioderMedVerdi.toList() shouldBe listOf(
                PeriodeMedVerdi(AntallBarn(1), 1.januar(2025) til 31.januar(2025)),
                PeriodeMedVerdi(AntallBarn.ZERO, 1.februar(2025) til 31.mars(2025)),
            )
        }
    }

    private fun innvilgelsesperioderDTO(periode: Periode, internDeltakelseId: String) = listOf(
        InnvilgelsesperiodeDTO(
            periode = periode.toDTO(),
            antallDagerPerMeldeperiode = 10,
            internDeltakelseId = internDeltakelseId,
        ),
    )

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
