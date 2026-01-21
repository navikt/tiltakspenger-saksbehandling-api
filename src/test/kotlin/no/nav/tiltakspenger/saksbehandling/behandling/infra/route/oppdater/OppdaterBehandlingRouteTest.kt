package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.oppdater

import arrow.core.nonEmptyListOf
import arrow.core.nonEmptySetOf
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.libs.dato.april
import no.nav.tiltakspenger.libs.dato.januar
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
import no.nav.tiltakspenger.saksbehandling.behandling.domene.SøknadsbehandlingResultat
import no.nav.tiltakspenger.saksbehandling.behandling.domene.SøknadsbehandlingResultat.Avslag
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.barnetillegg.toBarnetilleggDTO
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.InnvilgelsesperiodeDTO
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.OppdaterRevurderingDTO
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.OppdaterSøknadsbehandlingDTO
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.ValgtHjemmelForAvslagDTO
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.tilDTO
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContext
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.Begrunnelse
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.barnetillegg
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.innvilgelsesperioder
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.innvilgelsesperioderDTO
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.saksbehandler
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgStartRevurderingOmgjøring
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.oppdaterBehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.oppdaterSaksopplysningerForBehandlingId
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSøknadsbehandlingUnderBehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSøknadsbehandlingUnderBehandlingMedInnvilgelse
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltakDeltakerstatus
import org.json.JSONObject
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
            oppdatertBehandling.vedtaksperiode.shouldBeNull()
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
                    innvilgelsesperioder = behandling.innvilgelsesperioderDTO(
                        nyInnvilgelsesperiode,
                    ),
                    barnetillegg = barnetillegg.toBarnetilleggDTO(),
                ),
            )

            val oppdatertBehandling = tac.behandlingContext.behandlingRepo.hent(behandling.id)

            oppdatertBehandling.resultat.shouldBeInstanceOf<SøknadsbehandlingResultat.Innvilgelse>()
            oppdatertBehandling.fritekstTilVedtaksbrev!!.verdi shouldBe "ny brevtekst"
            oppdatertBehandling.begrunnelseVilkårsvurdering!!.verdi shouldBe "ny begrunnelse"
            oppdatertBehandling.vedtaksperiode shouldBe nyInnvilgelsesperiode
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
                    innvilgelsesperioder = behandling.innvilgelsesperioderDTO(
                        oppdatertTiltaksdeltakelsesPeriode,
                    ),
                    barnetillegg = Barnetillegg.utenBarnetillegg(oppdatertTiltaksdeltakelsesPeriode)
                        .toBarnetilleggDTO(),
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
    fun `revurdering til omgjøring - kan oppdatere behandlingen etter saksopplysninger har endret seg`() {
        withTestApplicationContext { tac ->
            // Omgjøringen starter med at tiltaksdeltakelsesperioden er endret siden søknadsvedtaket.
            val (sak, _, rammevedtakSøknadsbehandling, rammevedtakRevurdering) = iverksettSøknadsbehandlingOgStartRevurderingOmgjøring(
                tac,
                søknadsbehandlingInnvilgelsesperioder = innvilgelsesperioder((1.april(2025) til 10.april(2025))),
            )!!

            val tiltaksdeltakelseVedOpprettelseAvRevurdering =
                rammevedtakRevurdering.saksopplysninger.tiltaksdeltakelser.first()
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
            val barnetillegg = Barnetillegg.utenBarnetillegg((3 til 9.april(2025))).toBarnetilleggDTO()
            val innvilgelsesperioder = innvilgelsesperioder(
                nyOmgjøringsperiodeEtterOppdatering,
                tiltaksdeltakelseVedOpprettelseAvRevurdering,
            )

            val (_, oppdatertRevurdering) = oppdaterBehandling(
                tac = tac,
                sakId = sak.id,
                behandlingId = rammevedtakRevurdering.id,
                oppdaterBehandlingDTO = OppdaterRevurderingDTO.Omgjøring(
                    fritekstTilVedtaksbrev = "asdf",
                    begrunnelseVilkårsvurdering = null,
                    innvilgelsesperioder = innvilgelsesperioder.tilDTO(),
                    barnetillegg = barnetillegg,
                ),
                forventetStatus = HttpStatusCode.OK,
            )
            (oppdatertRevurdering as Revurdering).erFerdigutfylt() shouldBe true
            // Forventer at saksopplysningene er oppdatert, mens resultatet er ubesudlet.
            oppdatertRevurdering.saksopplysninger.tiltaksdeltakelser.single() shouldBe avbruttTiltaksdeltakelse
            val resultat = oppdatertRevurdering.resultat as RevurderingResultat.Omgjøring
            // Kommentar jah: Beklager for alt todomain-greiene. Her bør det expectes på eksplisitte verdier uten å bruke domenekode for mapping.
            resultat.barnetillegg shouldBe barnetillegg.tilBarnetillegg(oppdatertRevurdering.innvilgelsesperioder!!.perioder)
            resultat.antallDagerPerMeldeperiode shouldBe innvilgelsesperioder.antallDagerPerMeldeperiode
            resultat.valgteTiltaksdeltakelser shouldBe listOf(
                PeriodeMedVerdi(
                    avbruttTiltaksdeltakelse,
                    nyOmgjøringsperiodeEtterOppdatering,
                ),
            ).tilIkkeTomPeriodisering()
            oppdatertRevurdering.vedtaksperiode shouldBe rammevedtakSøknadsbehandling.behandling.vedtaksperiode
            resultat.vedtaksperiode shouldBe rammevedtakSøknadsbehandling.behandling.vedtaksperiode
            resultat.innvilgelsesperioder!!.totalPeriode shouldBe nyOmgjøringsperiodeEtterOppdatering
            oppdatertRevurdering.utbetaling shouldBe null

            // Forsikrer oss om at vi ikke har brutt noen init-regler i Sak.kt.
            tac.sakContext.sakService.hentForSakId(sakId = rammevedtakRevurdering.sakId).rammebehandlinger[1] shouldBe oppdatertRevurdering
        }
    }

    @Test
    fun `oppdatering feiler dersom søknadsbehandling opphører en tidligere innvilget periode`() = runTest {
        withTestApplicationContext { tac ->
            val saksbehandler = saksbehandler()
            val innvilgelsesperioder = innvilgelsesperioder(1.januar(2026) til 31.januar(2026))

            val (sak) = this.iverksettSøknadsbehandling(
                tac,
                innvilgelsesperioder = innvilgelsesperioder,
            )

            val (_, _, nesteSøknadsbehandling) = this.opprettSøknadsbehandlingUnderBehandlingMedInnvilgelse(
                tac,
                saksbehandler = saksbehandler,
                sakId = sak.id,
                innvilgelsesperioder = innvilgelsesperioder,
            )

            val behandlingId = nesteSøknadsbehandling.id
            val tiltaksId =
                nesteSøknadsbehandling.innvilgelsesperioder!!.valgteTiltaksdeltagelser.first().verdi.internDeltakelseId.toString()

            // Perioder med hull i innvilgelsen
            val nyePerioder = listOf(
                InnvilgelsesperiodeDTO(
                    periode = (1.januar(2026) til 15.januar(2026)).toDTO(),
                    antallDagerPerMeldeperiode = 10,
                    internDeltakelseId = tiltaksId,
                ),
                InnvilgelsesperiodeDTO(
                    periode = (17.januar(2026) til 31.januar(2026)).toDTO(),
                    antallDagerPerMeldeperiode = 10,
                    internDeltakelseId = tiltaksId,
                ),
            )

            val (_, _, responseJson) = oppdaterBehandling(
                tac,
                sakId = sak.id,
                behandlingId = nesteSøknadsbehandling.id,
                oppdaterBehandlingDTO = OppdaterSøknadsbehandlingDTO.Innvilgelse(
                    fritekstTilVedtaksbrev = "asdf",
                    begrunnelseVilkårsvurdering = null,
                    innvilgelsesperioder = nyePerioder,
                    barnetillegg = Barnetillegg.utenBarnetillegg(
                        nonEmptyListOf(
                            1.januar(2026) til 15.januar(2026),
                            17.januar(2026) til 31.januar(2026),
                        ),
                    ).toBarnetilleggDTO(),
                ),
                forventetStatus = HttpStatusCode.BadRequest,
            )

            JSONObject(responseJson).getString("kode") shouldBe "kan_ikke_opphøre"

            tac.behandlingContext.behandlingRepo.hent(behandlingId).also {
                it.innvilgelsesperioder shouldBe innvilgelsesperioder
            }
        }
    }
}
