package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.oppdater

import arrow.core.nonEmptyListOf
import arrow.core.nonEmptySetOf
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.periodisering.SammenhengendePeriodisering
import no.nav.tiltakspenger.libs.periodisering.til
import no.nav.tiltakspenger.saksbehandling.barnetillegg.AntallBarn
import no.nav.tiltakspenger.saksbehandling.barnetillegg.Barnetillegg
import no.nav.tiltakspenger.saksbehandling.behandling.domene.AntallDagerForMeldeperiode
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Avslagsgrunnlag
import no.nav.tiltakspenger.saksbehandling.behandling.domene.DEFAULT_DAGER_MED_TILTAKSPENGER_FOR_PERIODE
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus
import no.nav.tiltakspenger.saksbehandling.behandling.domene.SøknadsbehandlingResultat
import no.nav.tiltakspenger.saksbehandling.behandling.domene.SøknadsbehandlingResultat.Avslag
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContext
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.Begrunnelse
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.barnetillegg
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.innvilgelsesperioder
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.saksbehandler
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.oppdaterSøknadsbehandlingAvslag
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.oppdaterSøknadsbehandlingIkkeValgt
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.oppdaterSøknadsbehandlingInnvilgelse
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSøknadsbehandlingUnderBehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSøknadsbehandlingUnderBehandlingMedInnvilgelse
import org.json.JSONObject
import org.junit.jupiter.api.Test

class OppdaterSøknadsbehandlingRouteTest {
    @Test
    fun `kan oppdatere uten valgt resultat`() {
        withTestApplicationContext { tac ->
            val (sak, _, behandling) = opprettSøknadsbehandlingUnderBehandling(tac)

            oppdaterSøknadsbehandlingIkkeValgt(
                tac = tac,
                sakId = sak.id,
                behandlingId = behandling.id,
                fritekstTilVedtaksbrev = "ny brevtekst",
                begrunnelseVilkårsvurdering = "ny begrunnelse",
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

            val nyeInnvilgelsesperioder = innvilgelsesperioder(nyInnvilgelsesperiode, tiltaksdeltakelse)

            oppdaterSøknadsbehandlingInnvilgelse(
                tac = tac,
                sakId = sak.id,
                behandlingId = behandling.id,
                fritekstTilVedtaksbrev = "ny brevtekst",
                begrunnelseVilkårsvurdering = "ny begrunnelse",
                innvilgelsesperioder = nyeInnvilgelsesperioder,
                barnetillegg = barnetillegg,
            )

            val oppdatertBehandling = tac.behandlingContext.behandlingRepo.hent(behandling.id)

            oppdatertBehandling.resultat.shouldBeInstanceOf<SøknadsbehandlingResultat.Innvilgelse>()
            oppdatertBehandling.fritekstTilVedtaksbrev!!.verdi shouldBe "ny brevtekst"
            oppdatertBehandling.begrunnelseVilkårsvurdering!!.verdi shouldBe "ny begrunnelse"
            oppdatertBehandling.vedtaksperiode shouldBe nyInnvilgelsesperiode
            oppdatertBehandling.barnetillegg shouldBe barnetillegg
            oppdatertBehandling.antallDagerPerMeldeperiode shouldBe antallDager
            oppdatertBehandling.innvilgelsesperioder shouldBe nyeInnvilgelsesperioder
        }
    }

    @Test
    fun `kan oppdatere avslått søknadsbehandling`() {
        withTestApplicationContext { tac ->
            val (sak, _, behandling) = opprettSøknadsbehandlingUnderBehandling(tac)

            oppdaterSøknadsbehandlingAvslag(
                tac = tac,
                sakId = sak.id,
                behandlingId = behandling.id,
                fritekstTilVedtaksbrev = "ny brevtekst",
                begrunnelseVilkårsvurdering = "ny begrunnelse",
                avslagsgrunner = setOf(Avslagsgrunnlag.DeltarIkkePåArbeidsmarkedstiltak),
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

            oppdaterSøknadsbehandlingInnvilgelse(
                tac,
                sak.id,
                behandlingId,
                fritekstTilVedtaksbrev = "asdf",
                begrunnelseVilkårsvurdering = null,
                innvilgelsesperioder = innvilgelsesperioder(oppdatertTiltaksdeltakelsesPeriode),
                barnetillegg = Barnetillegg.utenBarnetillegg(oppdatertTiltaksdeltakelsesPeriode),
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

            // Perioder med hull i innvilgelsen
            val nyeInnvilgelsesperioder = innvilgelsesperioder(
                (1.januar(2026) til 15.januar(2026)),
                (17.januar(2026) til 31.januar(2026)),
            )

            val (_, _, responseJson) = oppdaterSøknadsbehandlingInnvilgelse(
                tac,
                sakId = sak.id,
                behandlingId = nesteSøknadsbehandling.id,
                fritekstTilVedtaksbrev = "asdf",
                begrunnelseVilkårsvurdering = null,
                innvilgelsesperioder = nyeInnvilgelsesperioder,
                barnetillegg = Barnetillegg.utenBarnetillegg(
                    nonEmptyListOf(
                        1.januar(2026) til 15.januar(2026),
                        17.januar(2026) til 31.januar(2026),
                    ),
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
