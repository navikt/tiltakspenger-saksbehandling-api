package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.oppdater

import arrow.core.nonEmptyListOf
import arrow.core.nonEmptySetOf
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.libs.dato.april
import no.nav.tiltakspenger.libs.periodisering.PeriodeMedVerdi
import no.nav.tiltakspenger.libs.periodisering.SammenhengendePeriodisering
import no.nav.tiltakspenger.libs.periodisering.til
import no.nav.tiltakspenger.libs.periodisering.tilIkkeTomPeriodisering
import no.nav.tiltakspenger.libs.periodisering.toDTO
import no.nav.tiltakspenger.saksbehandling.barnetillegg.AntallBarn
import no.nav.tiltakspenger.saksbehandling.barnetillegg.Barnetillegg
import no.nav.tiltakspenger.saksbehandling.behandling.domene.AntallDagerForMeldeperiode
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Avslagsgrunnlag
import no.nav.tiltakspenger.saksbehandling.behandling.domene.DEFAULT_DAGER_MED_TILTAKSPENGER_FOR_PERIODE
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Revurdering
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
import no.nav.tiltakspenger.saksbehandling.infra.route.AntallDagerPerMeldeperiodeDTO
import no.nav.tiltakspenger.saksbehandling.infra.route.tilAntallDagerPerMeldeperiodeDTO
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.Begrunnelse
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.barnetillegg
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.saksbehandler
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgStartRevurderingInnvilgelse
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgStartRevurderingOmgjøring
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgStartRevurderingStans
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.oppdaterBehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.oppdaterSaksopplysningerForBehandlingId
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSøknadsbehandlingUnderBehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSøknadsbehandlingUnderBehandlingMedInnvilgelse
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltakDeltakerstatus
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.route.TiltaksdeltakelsePeriodeDTO
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

            val tiltaksdeltakelse = behandling.saksopplysninger.tiltaksdeltakelser.first()
            val nyInnvilgelsesperiode = tiltaksdeltakelse.periode!!.minusTilOgMed(1)

            val barnetillegg = barnetillegg(
                begrunnelse = Begrunnelse.create("barnetillegg begrunnelse"),
                periode = nyInnvilgelsesperiode,
                antallBarn = AntallBarn(1),
            )

            val antallDager = SammenhengendePeriodisering(
                AntallDagerForMeldeperiode(DEFAULT_DAGER_MED_TILTAKSPENGER_FOR_PERIODE),
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
                            eksternDeltagelseId = tiltaksdeltakelse.eksternDeltakelseId,
                            periode = nyInnvilgelsesperiode.toDTO(),
                        ),
                    ),
                    innvilgelsesperiode = nyInnvilgelsesperiode.toDTO(),
                    barnetillegg = barnetillegg.toBarnetilleggDTO(),
                    antallDagerPerMeldeperiodeForPerioder = antallDager.tilAntallDagerPerMeldeperiodeDTO(),
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
            val (sak, _, _, revurdering) = iverksettSøknadsbehandlingOgStartRevurderingInnvilgelse(tac)

            val tiltaksdeltakelse = revurdering.saksopplysninger.tiltaksdeltakelser.first()
            val nyInnvilgelsesperiode = tiltaksdeltakelse.periode!!.minusTilOgMed(1)

            val barnetillegg = barnetillegg(
                begrunnelse = Begrunnelse.create("barnetillegg begrunnelse"),
                periode = nyInnvilgelsesperiode,
                antallBarn = AntallBarn(1),
            )

            val antallDager = SammenhengendePeriodisering(
                AntallDagerForMeldeperiode(DEFAULT_DAGER_MED_TILTAKSPENGER_FOR_PERIODE),
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
                            eksternDeltagelseId = tiltaksdeltakelse.eksternDeltakelseId,
                            periode = nyInnvilgelsesperiode.toDTO(),
                        ),
                    ),
                    innvilgelsesperiode = nyInnvilgelsesperiode.toDTO(),
                    barnetillegg = barnetillegg.toBarnetilleggDTO(),
                    antallDagerPerMeldeperiodeForPerioder = antallDager.tilAntallDagerPerMeldeperiodeDTO(),
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
            val (sak, _, _, revurdering) = iverksettSøknadsbehandlingOgStartRevurderingStans(tac)

            oppdaterBehandling(
                tac = tac,
                sakId = sak.id,
                behandlingId = revurdering.id,
                oppdaterBehandlingDTO = OppdaterRevurderingDTO.Stans(
                    fritekstTilVedtaksbrev = "ny brevtekst",
                    begrunnelseVilkårsvurdering = "ny begrunnelse",
                    valgteHjemler = listOf(ValgtHjemmelForStansDTO.DeltarIkkePåArbeidsmarkedstiltak),
                    stansFraOgMed = 9.april(2025),
                    stansTilOgMed = null,
                    harValgtStansFraFørsteDagSomGirRett = false,
                    harValgtStansTilSisteDagSomGirRett = true,
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
            val (sak, _, _, revurdering) = iverksettSøknadsbehandlingOgStartRevurderingStans(tac)

            oppdaterBehandling(
                tac = tac,
                sakId = sak.id,
                behandlingId = revurdering.id,
                oppdaterBehandlingDTO = OppdaterRevurderingDTO.Stans(
                    fritekstTilVedtaksbrev = "ny brevtekst",
                    begrunnelseVilkårsvurdering = "ny begrunnelse",
                    valgteHjemler = listOf(ValgtHjemmelForStansDTO.DeltarIkkePåArbeidsmarkedstiltak),
                    stansFraOgMed = 9.april(2025),
                    stansTilOgMed = null,
                    harValgtStansFraFørsteDagSomGirRett = false,
                    harValgtStansTilSisteDagSomGirRett = true,
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
                it.status shouldBe Rammebehandlingsstatus.UNDER_BEHANDLING
                it.saksbehandler shouldBe saksbehandler.navIdent
                it.beslutter shouldBe null
            }

            val tiltaksdeltakelse = behandling.saksopplysninger.tiltaksdeltakelser.single()
            val tiltaksdeltakelsePeriode = tiltaksdeltakelse.periode!!

            val oppdatertTiltaksdeltakelsesPeriode = tiltaksdeltakelsePeriode.minusFraOgMed(7)

            oppdaterBehandling(
                tac,
                sak.id,
                behandlingId,
                oppdaterBehandlingDTO = OppdaterSøknadsbehandlingDTO.Innvilgelse(
                    fritekstTilVedtaksbrev = "asdf",
                    begrunnelseVilkårsvurdering = null,
                    valgteTiltaksdeltakelser = listOf(
                        TiltaksdeltakelsePeriodeDTO(
                            eksternDeltagelseId = tiltaksdeltakelse.eksternDeltakelseId,
                            periode = tiltaksdeltakelsePeriode.toDTO(),
                        ),
                    ),
                    innvilgelsesperiode = oppdatertTiltaksdeltakelsesPeriode.toDTO(),
                    barnetillegg = Barnetillegg.utenBarnetillegg(oppdatertTiltaksdeltakelsesPeriode)
                        .toBarnetilleggDTO(),
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
                it.status shouldBe Rammebehandlingsstatus.UNDER_BEHANDLING
                it.saksbehandler shouldBe saksbehandler.navIdent
                it.beslutter shouldBe null
                it.fritekstTilVedtaksbrev.shouldBeNull()
            }
        }
    }

    @Test
    fun `oppdater revurdering stans feiler hvis stansFraOgMed er før innvilgelsesperioden`() {
        withTestApplicationContext { tac ->
            val (sak, _, _, revurdering) = iverksettSøknadsbehandlingOgStartRevurderingStans(tac)

            val stansFraOgMed = sak.førsteDagSomGirRett!!.minusDays(2)

            oppdaterBehandling(
                tac = tac,
                sakId = sak.id,
                behandlingId = revurdering.id,
                oppdaterBehandlingDTO = OppdaterRevurderingDTO.Stans(
                    begrunnelseVilkårsvurdering = null,
                    fritekstTilVedtaksbrev = null,
                    valgteHjemler = nonEmptyListOf(ValgtHjemmelForStansDTO.Alder),
                    stansFraOgMed = stansFraOgMed,
                    stansTilOgMed = null,
                    harValgtStansFraFørsteDagSomGirRett = false,
                    harValgtStansTilSisteDagSomGirRett = true,
                ),
                forventetStatus = HttpStatusCode.InternalServerError,
            )
        }
    }

    @Test
    fun `send revurdering stans til beslutning feiler hvis stansFraOgMed er etter innvilgelsesperioden`() {
        withTestApplicationContext { tac ->
            val (sak, _, _, revurdering) = iverksettSøknadsbehandlingOgStartRevurderingStans(tac)
            val stansFraOgMed = sak.sisteDagSomGirRett!!.plusDays(2)

            oppdaterBehandling(
                tac = tac,
                sakId = sak.id,
                behandlingId = revurdering.id,
                oppdaterBehandlingDTO = OppdaterRevurderingDTO.Stans(
                    begrunnelseVilkårsvurdering = null,
                    fritekstTilVedtaksbrev = null,
                    valgteHjemler = nonEmptyListOf(ValgtHjemmelForStansDTO.Alder),
                    stansFraOgMed = stansFraOgMed,
                    stansTilOgMed = null,
                    harValgtStansFraFørsteDagSomGirRett = false,
                    harValgtStansTilSisteDagSomGirRett = true,
                ),
                forventetStatus = HttpStatusCode.InternalServerError,
            )
        }
    }

    @Test
    fun `revurdering til omgjøring - kan oppdatere behandlingen etter saksopplysninger har endret seg`() {
        withTestApplicationContext { tac ->
            // Omgjøringen starter med at tiltaksdeltakelsesperioden er endret siden søknadsvedtaket.
            val (sak, _, rammevedtakSøknadsbehandling, rammevedtakRevurdering) = iverksettSøknadsbehandlingOgStartRevurderingOmgjøring(tac)!!
            val tiltaksdeltakelseVedOpprettelseAvRevurdering = rammevedtakRevurdering.saksopplysninger.tiltaksdeltakelser.first()
            val nyOmgjøringsperiodeEtterOppdatering = (3 til 9.april(2025))
            val avbruttTiltaksdeltakelse = tiltaksdeltakelseVedOpprettelseAvRevurdering.copy(
                deltakelseFraOgMed = tiltaksdeltakelseVedOpprettelseAvRevurdering.deltakelseFraOgMed!!,
                deltakelseTilOgMed = tiltaksdeltakelseVedOpprettelseAvRevurdering.deltakelseTilOgMed!!.minusDays(1),
                deltakelseStatus = TiltakDeltakerstatus.Avbrutt,
            )
            // Under behandlingen endrer tiltaksdeltakelsen seg igjen.
            tac.oppdaterTiltaksdeltakelse(fnr = sak.fnr, tiltaksdeltakelse = avbruttTiltaksdeltakelse)
            val (_, revurderingMedOppdatertSaksopplysninger: Rammebehandling) = oppdaterSaksopplysningerForBehandlingId(
                tac,
                sak.id,
                rammevedtakRevurdering.id,
            )
            (revurderingMedOppdatertSaksopplysninger as Revurdering).erFerdigutfylt() shouldBe true
            val antallDagerPerMeldeperiodeForPerioder = listOf(
                AntallDagerPerMeldeperiodeDTO(
                    periode = nyOmgjøringsperiodeEtterOppdatering.toDTO(),
                    antallDagerPerMeldeperiode = 10,
                ),
            )
            val barnetillegg = Barnetillegg.utenBarnetillegg((3 til 9.april(2025))).toBarnetilleggDTO()
            val valgteTiltaksdeltakelser = listOf(
                TiltaksdeltakelsePeriodeDTO(
                    eksternDeltagelseId = avbruttTiltaksdeltakelse.eksternDeltakelseId,
                    periode = nyOmgjøringsperiodeEtterOppdatering.toDTO(),
                ),
            )
            val (_, oppdatertRevurdering) = oppdaterBehandling(
                tac = tac,
                sakId = sak.id,
                behandlingId = rammevedtakRevurdering.id,
                oppdaterBehandlingDTO = OppdaterRevurderingDTO.Omgjøring(
                    fritekstTilVedtaksbrev = "asdf",
                    begrunnelseVilkårsvurdering = null,
                    valgteTiltaksdeltakelser = valgteTiltaksdeltakelser,
                    innvilgelsesperiode = nyOmgjøringsperiodeEtterOppdatering.toDTO(),
                    barnetillegg = barnetillegg,
                    antallDagerPerMeldeperiodeForPerioder = antallDagerPerMeldeperiodeForPerioder,
                ),
                forventetStatus = HttpStatusCode.OK,
            )
            (oppdatertRevurdering as Revurdering).erFerdigutfylt() shouldBe true
            // Forventer at saksopplysningene er oppdatert, mens resultatet er ubesudlet.
            oppdatertRevurdering.saksopplysninger.tiltaksdeltakelser.single() shouldBe avbruttTiltaksdeltakelse
            val resultat = oppdatertRevurdering.resultat as RevurderingResultat.Omgjøring
            // Kommentar jah: Beklager for alt todomain-greiene. Her bør det expectes på eksplisitte verdier uten å bruke domenekode for mapping.
            resultat.barnetillegg shouldBe barnetillegg.tilBarnetillegg(oppdatertRevurdering.innvilgelsesperioder!!.totalPeriode)
            resultat.antallDagerPerMeldeperiode shouldBe antallDagerPerMeldeperiodeForPerioder.map {
                PeriodeMedVerdi(
                    AntallDagerForMeldeperiode(it.antallDagerPerMeldeperiode),
                    it.periode.toDomain(),
                )
            }.tilIkkeTomPeriodisering()
            resultat.valgteTiltaksdeltakelser shouldBe listOf(
                PeriodeMedVerdi(
                    avbruttTiltaksdeltakelse,
                    nyOmgjøringsperiodeEtterOppdatering,
                ),
            ).tilIkkeTomPeriodisering()
            oppdatertRevurdering.virkningsperiode shouldBe rammevedtakSøknadsbehandling.behandling.virkningsperiode
            resultat.virkningsperiode shouldBe rammevedtakSøknadsbehandling.behandling.virkningsperiode
            resultat.innvilgelsesperioder.totalPeriode shouldBe nyOmgjøringsperiodeEtterOppdatering
            oppdatertRevurdering.utbetaling shouldBe null

            // Forsikrer oss om at vi ikke har brutt noen init-regler i Sak.kt.
            tac.sakContext.sakService.hentForSakId(sakId = rammevedtakRevurdering.sakId).rammebehandlinger[1] shouldBe oppdatertRevurdering
        }
    }
}
