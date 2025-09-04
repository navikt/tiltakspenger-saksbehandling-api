package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.oppdater

import arrow.core.nonEmptyListOf
import arrow.core.nonEmptySetOf
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.libs.dato.april
import no.nav.tiltakspenger.libs.periodisering.SammenhengendePeriodisering
import no.nav.tiltakspenger.libs.periodisering.toDTO
import no.nav.tiltakspenger.saksbehandling.barnetillegg.AntallBarn
import no.nav.tiltakspenger.saksbehandling.behandling.domene.AntallDagerForMeldeperiode
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Avslagsgrunnlag
import no.nav.tiltakspenger.saksbehandling.behandling.domene.BegrunnelseVilkårsvurdering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandlingsstatus
import no.nav.tiltakspenger.saksbehandling.behandling.domene.MAKS_DAGER_MED_TILTAKSPENGER_FOR_PERIODE
import no.nav.tiltakspenger.saksbehandling.behandling.domene.RevurderingResultat
import no.nav.tiltakspenger.saksbehandling.behandling.domene.RevurderingResultat.Stans
import no.nav.tiltakspenger.saksbehandling.behandling.domene.SøknadsbehandlingResultat
import no.nav.tiltakspenger.saksbehandling.behandling.domene.SøknadsbehandlingResultat.Avslag
import no.nav.tiltakspenger.saksbehandling.behandling.domene.ValgtHjemmelForStans
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.barnetillegg.toBarnetilleggDTO
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.OppdaterRevurderingDTO
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.OppdaterSøknadsbehandlingDTO
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.ValgtHjemmelForAvslagDTO
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.ValgtHjemmelForStansDTO
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContext
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.barnetillegg
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.saksbehandler
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.oppdaterBehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSøknadsbehandlingUnderBehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSøknadsbehandlingUnderBehandlingMedInnvilgelse
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.startRevurderingInnvilgelse
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.startRevurderingStans
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.infra.route.AntallDagerPerMeldeperiodeDTO
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.infra.route.TiltaksdeltakelsePeriodeDTO
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.infra.route.toDTO
import org.junit.jupiter.api.Test

class OppdaterBehandlingRouteTest {
    @Test
    fun `kan oppdatere uten valgt resultat`() {
        withTestApplicationContext { tac ->
            val (sak, _, behandling) = opprettSøknadsbehandlingUnderBehandling(tac)

            oppdaterBehandling(
                tac = tac,
                sakId = sak.id,
                behandlingId = behandling.id,
                oppdaterBehandlingDTO = OppdaterSøknadsbehandlingDTO.IkkeValgtResultat(
                    fritekstTilVedtaksbrev = "ny brevtekst",
                    begrunnelseVilkårsvurdering = "ny begrunnelse",
                ),
            )

            val oppdatertBehandling = tac.behandlingContext.behandlingRepo.hent(behandling.id)

            oppdatertBehandling.resultat.shouldBeNull()
            oppdatertBehandling.virkningsperiode.shouldBeNull()
            oppdatertBehandling.fritekstTilVedtaksbrev!!.verdi shouldBe "ny brevtekst"
            oppdatertBehandling.begrunnelseVilkårsvurdering!!.verdi shouldBe "ny begrunnelse"
        }
    }

    @Test
    fun `kan oppdatere innvilget søknadsbehandling`() {
        withTestApplicationContext { tac ->
            val (sak, _, behandling) = opprettSøknadsbehandlingUnderBehandling(tac)

            val tiltaksdeltagelse = behandling.saksopplysninger.tiltaksdeltagelser.first()
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
                            periode = nyInnvilgelsesperiode.toDTO(),
                        ),
                    ),
                    innvilgelsesperiode = nyInnvilgelsesperiode.toDTO(),
                    barnetillegg = barnetillegg.toBarnetilleggDTO(),
                    antallDagerPerMeldeperiodeForPerioder = antallDager.toDTO(),
                ),
            )

            val oppdatertBehandling = tac.behandlingContext.behandlingRepo.hent(behandling.id)

            oppdatertBehandling.resultat.shouldBeInstanceOf<SøknadsbehandlingResultat.Innvilgelse>()
            oppdatertBehandling.fritekstTilVedtaksbrev!!.verdi shouldBe "ny brevtekst"
            oppdatertBehandling.begrunnelseVilkårsvurdering!!.verdi shouldBe "ny begrunnelse"
            oppdatertBehandling.virkningsperiode shouldBe nyInnvilgelsesperiode
            oppdatertBehandling.barnetillegg shouldBe barnetillegg
            oppdatertBehandling.antallDagerPerMeldeperiode shouldBe antallDager
        }
    }

    @Test
    fun `kan oppdatere avslått søknadsbehandling`() {
        withTestApplicationContext { tac ->
            val (sak, _, behandling) = opprettSøknadsbehandlingUnderBehandling(tac)

            oppdaterBehandling(
                tac = tac,
                sakId = sak.id,
                behandlingId = behandling.id,
                oppdaterBehandlingDTO = OppdaterSøknadsbehandlingDTO.Avslag(
                    fritekstTilVedtaksbrev = "ny brevtekst",
                    begrunnelseVilkårsvurdering = "ny begrunnelse",
                    avslagsgrunner = listOf(ValgtHjemmelForAvslagDTO.DeltarIkkePåArbeidsmarkedstiltak),
                ),
            )

            val oppdatertBehandling = tac.behandlingContext.behandlingRepo.hent(behandling.id)

            oppdatertBehandling.resultat.shouldBeInstanceOf<Avslag>()
            oppdatertBehandling.fritekstTilVedtaksbrev!!.verdi shouldBe "ny brevtekst"
            oppdatertBehandling.begrunnelseVilkårsvurdering!!.verdi shouldBe "ny begrunnelse"
            (oppdatertBehandling.resultat as Avslag).avslagsgrunner shouldBe nonEmptySetOf(Avslagsgrunnlag.DeltarIkkePåArbeidsmarkedstiltak)
        }
    }

    @Test
    fun `kan oppdatere revurdering innvilgelse`() {
        withTestApplicationContext { tac ->
            val (sak, _, _, revurdering) = startRevurderingInnvilgelse(tac)

            val tiltaksdeltagelse = revurdering.saksopplysninger.tiltaksdeltagelser.first()
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
                behandlingId = revurdering.id,
                oppdaterBehandlingDTO = OppdaterRevurderingDTO.Innvilgelse(
                    fritekstTilVedtaksbrev = "ny brevtekst",
                    begrunnelseVilkårsvurdering = "ny begrunnelse",
                    valgteTiltaksdeltakelser = listOf(
                        TiltaksdeltakelsePeriodeDTO(
                            eksternDeltagelseId = tiltaksdeltagelse.eksternDeltagelseId,
                            periode = nyInnvilgelsesperiode.toDTO(),
                        ),
                    ),
                    innvilgelsesperiode = nyInnvilgelsesperiode.toDTO(),
                    barnetillegg = barnetillegg.toBarnetilleggDTO(),
                    antallDagerPerMeldeperiodeForPerioder = antallDager.toDTO(),
                ),
            )

            val oppdatertBehandling = tac.behandlingContext.behandlingRepo.hent(revurdering.id)

            oppdatertBehandling.resultat.shouldBeInstanceOf<RevurderingResultat.Innvilgelse>()
            oppdatertBehandling.fritekstTilVedtaksbrev!!.verdi shouldBe "ny brevtekst"
            oppdatertBehandling.begrunnelseVilkårsvurdering!!.verdi shouldBe "ny begrunnelse"
            oppdatertBehandling.virkningsperiode shouldBe nyInnvilgelsesperiode
            oppdatertBehandling.barnetillegg shouldBe barnetillegg
            oppdatertBehandling.antallDagerPerMeldeperiode shouldBe antallDager
        }
    }

    @Test
    fun `kan oppdatere revurdering stans`() {
        withTestApplicationContext { tac ->
            val (sak, _, _, revurdering) = startRevurderingStans(tac)

            oppdaterBehandling(
                tac = tac,
                sakId = sak.id,
                behandlingId = revurdering.id,
                oppdaterBehandlingDTO = OppdaterRevurderingDTO.Stans(
                    fritekstTilVedtaksbrev = "ny brevtekst",
                    begrunnelseVilkårsvurdering = "ny begrunnelse",
                    valgteHjemler = listOf(ValgtHjemmelForStansDTO.DeltarIkkePåArbeidsmarkedstiltak),
                    stansFraOgMed = 9.april(2025),
                ),
            )

            val oppdatertBehandling = tac.behandlingContext.behandlingRepo.hent(revurdering.id)

            oppdatertBehandling.resultat.shouldBeInstanceOf<Stans>()
            oppdatertBehandling.fritekstTilVedtaksbrev!!.verdi shouldBe "ny brevtekst"
            oppdatertBehandling.begrunnelseVilkårsvurdering!!.verdi shouldBe "ny begrunnelse"
            oppdatertBehandling.virkningsperiode!!.fraOgMed shouldBe 9.april(2025)
            (oppdatertBehandling.resultat as Stans).valgtHjemmel shouldBe listOf(ValgtHjemmelForStans.DeltarIkkePåArbeidsmarkedstiltak)
        }
    }

    @Test
    fun `kan oppdatere revurdering stans over utbetalte perioder`() {
        withTestApplicationContext { tac ->
            val (sak, _, _, revurdering) = startRevurderingStans(tac)

            oppdaterBehandling(
                tac = tac,
                sakId = sak.id,
                behandlingId = revurdering.id,
                oppdaterBehandlingDTO = OppdaterRevurderingDTO.Stans(
                    fritekstTilVedtaksbrev = "ny brevtekst",
                    begrunnelseVilkårsvurdering = "ny begrunnelse",
                    valgteHjemler = listOf(ValgtHjemmelForStansDTO.DeltarIkkePåArbeidsmarkedstiltak),
                    stansFraOgMed = 9.april(2025),
                ),
            )

            val oppdatertBehandling = tac.behandlingContext.behandlingRepo.hent(revurdering.id)

            oppdatertBehandling.resultat.shouldBeInstanceOf<Stans>()
            oppdatertBehandling.fritekstTilVedtaksbrev!!.verdi shouldBe "ny brevtekst"
            oppdatertBehandling.begrunnelseVilkårsvurdering!!.verdi shouldBe "ny begrunnelse"
            oppdatertBehandling.virkningsperiode!!.fraOgMed shouldBe 9.april(2025)
            (oppdatertBehandling.resultat as Stans).valgtHjemmel shouldBe listOf(ValgtHjemmelForStans.DeltarIkkePåArbeidsmarkedstiltak)
        }
    }

    @Test
    fun `oppdatering feiler hvis behandlingsperioden er utenfor deltakelsesperioden`() = runTest {
        withTestApplicationContext { tac ->
            val saksbehandler = saksbehandler()
            val (sak, _, behandling) = this.opprettSøknadsbehandlingUnderBehandlingMedInnvilgelse(
                tac,
                saksbehandler = saksbehandler,
            )
            val behandlingId = behandling.id

            tac.behandlingContext.behandlingRepo.hent(behandlingId).also {
                it.status shouldBe Behandlingsstatus.UNDER_BEHANDLING
                it.saksbehandler shouldBe saksbehandler.navIdent
                it.beslutter shouldBe null
            }

            val tiltaksdeltagelse = behandling.saksopplysninger.tiltaksdeltagelser.single()
            val tiltaksdeltakelsePeriode = tiltaksdeltagelse.periode!!

            oppdaterBehandling(
                tac,
                sak.id,
                behandlingId,
                oppdaterBehandlingDTO = OppdaterSøknadsbehandlingDTO.Innvilgelse(
                    fritekstTilVedtaksbrev = "asdf",
                    begrunnelseVilkårsvurdering = null,
                    valgteTiltaksdeltakelser = listOf(
                        TiltaksdeltakelsePeriodeDTO(
                            eksternDeltagelseId = tiltaksdeltagelse.eksternDeltagelseId,
                            periode = tiltaksdeltakelsePeriode.toDTO(),
                        ),
                    ),
                    innvilgelsesperiode = tiltaksdeltakelsePeriode.minusFraOgMed(7).toDTO(),
                    barnetillegg = null,
                    antallDagerPerMeldeperiodeForPerioder = listOf(
                        AntallDagerPerMeldeperiodeDTO(
                            periode = tiltaksdeltakelsePeriode.toDTO(),
                            antallDagerPerMeldeperiode = 10,
                        ),
                    ),
                ),
                forventetStatus = HttpStatusCode.InternalServerError,
            )

            tac.behandlingContext.behandlingRepo.hent(behandlingId).also {
                it.status shouldBe Behandlingsstatus.UNDER_BEHANDLING
                it.saksbehandler shouldBe saksbehandler.navIdent
                it.beslutter shouldBe null
                it.fritekstTilVedtaksbrev.shouldBeNull()
            }
        }
    }

    @Test
    fun `oppdater revurdering stans feiler hvis stansdato er før innvilgelsesperioden`() {
        withTestApplicationContext { tac ->
            val (sak, _, _, revurdering) = startRevurderingStans(tac)

            val stansdato = sak.vedtaksliste.førsteDagSomGirRett!!.minusDays(2)

            oppdaterBehandling(
                tac = tac,
                sakId = sak.id,
                behandlingId = revurdering.id,
                oppdaterBehandlingDTO = OppdaterRevurderingDTO.Stans(
                    begrunnelseVilkårsvurdering = null,
                    fritekstTilVedtaksbrev = null,
                    valgteHjemler = nonEmptyListOf(ValgtHjemmelForStansDTO.Alder),
                    stansFraOgMed = stansdato,
                ),
                forventetStatus = HttpStatusCode.InternalServerError,
            )
        }
    }

    @Test
    fun `send revurdering stans til beslutning feiler hvis stansdato er etter innvilgelsesperioden`() {
        withTestApplicationContext { tac ->
            val (sak, _, _, revurdering) = startRevurderingStans(tac)
            val stansdato = sak.sisteDagSomGirRett!!.plusDays(2)

            oppdaterBehandling(
                tac = tac,
                sakId = sak.id,
                behandlingId = revurdering.id,
                oppdaterBehandlingDTO = OppdaterRevurderingDTO.Stans(
                    begrunnelseVilkårsvurdering = null,
                    fritekstTilVedtaksbrev = null,
                    valgteHjemler = nonEmptyListOf(ValgtHjemmelForStansDTO.Alder),
                    stansFraOgMed = stansdato,
                ),
                forventetStatus = HttpStatusCode.InternalServerError,
            )
        }
    }
}
