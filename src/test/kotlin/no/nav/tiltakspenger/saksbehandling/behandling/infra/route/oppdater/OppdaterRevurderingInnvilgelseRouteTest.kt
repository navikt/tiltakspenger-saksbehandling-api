package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.oppdater

import arrow.core.nonEmptyListOf
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.tiltakspenger.libs.common.SøknadId
import no.nav.tiltakspenger.libs.ktor.test.common.ForventetRespons
import no.nav.tiltakspenger.libs.periodisering.SammenhengendePeriodisering
import no.nav.tiltakspenger.saksbehandling.barnetillegg.BarnMedBarnetillegg
import no.nav.tiltakspenger.saksbehandling.behandling.domene.AntallDagerForMeldeperiode
import no.nav.tiltakspenger.saksbehandling.behandling.domene.DEFAULT_DAGER_MED_TILTAKSPENGER_FOR_PERIODE
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Prioritering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.Revurderingsresultat
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContext
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.felles.Begrunnelse
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.barnMedBarnetillegg
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.barnetilleggV2
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.httpKlientUventetStatus
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.innvilgelsesperioder
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgStartRevurderingInnvilgelse
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.oppdaterRevurderingInnvilgelse
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettOgIverksettMeldekortbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.startRevurderingInnvilgelse
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.KunneIkkeSimulere
import org.json.JSONObject
import org.junit.jupiter.api.Test

class OppdaterRevurderingInnvilgelseRouteTest {

    @Test
    fun `kan oppdatere revurdering innvilgelse med brev`() {
        withTestApplicationContext { tac ->
            val (sak, _, _, revurdering) = iverksettSøknadsbehandlingOgStartRevurderingInnvilgelse(tac)

            val tiltaksdeltakelse = revurdering.saksopplysninger.tiltaksdeltakelser.first()
            val nyInnvilgelsesperiode = tiltaksdeltakelse.lukketPeriode!!.minusTilOgMed(1)

            val barnetillegg = barnetilleggV2(
                begrunnelse = Begrunnelse.create("barnetillegg begrunnelse"),
                innvilgelsesperioder = nonEmptyListOf(nyInnvilgelsesperiode),
                barn = listOf(barnMedBarnetillegg(fornavn = "Ola"), barnMedBarnetillegg(fornavn = "Kari", perioder = listOf(nyInnvilgelsesperiode.minusTilOgMed(7)))),
            )

            val antallDager = SammenhengendePeriodisering(
                AntallDagerForMeldeperiode(DEFAULT_DAGER_MED_TILTAKSPENGER_FOR_PERIODE),
                nyInnvilgelsesperiode,
            )

            oppdaterRevurderingInnvilgelse(
                tac = tac,
                sakId = sak.id,
                behandlingId = revurdering.id,
                fritekstTilVedtaksbrev = "ny brevtekst",
                begrunnelseVilkårsvurdering = "ny begrunnelse",
                innvilgelsesperioder = innvilgelsesperioder(nyInnvilgelsesperiode),
                barnetillegg = barnetillegg,
                skalSendeVedtaksbrev = true,
            )

            val oppdatertBehandling = tac.behandlingContext.rammebehandlingRepo.hent(revurdering.id)

            oppdatertBehandling.resultat.shouldBeInstanceOf<Revurderingsresultat.Innvilgelse>()
            oppdatertBehandling.fritekstTilVedtaksbrev!!.verdi shouldBe "ny brevtekst"
            oppdatertBehandling.begrunnelseVilkårsvurdering!!.verdi shouldBe "ny begrunnelse"
            oppdatertBehandling.vedtaksperiode shouldBe nyInnvilgelsesperiode
            oppdatertBehandling.barnetillegg shouldBe barnetillegg
            oppdatertBehandling.antallDagerPerMeldeperiode shouldBe antallDager
            oppdatertBehandling.skalSendeVedtaksbrev shouldBe true
        }
    }

    @Test
    fun `kan oppdatere revurdering innvilgelse uten brev`() {
        withTestApplicationContext { tac ->
            val (sak, _, _, revurdering) = iverksettSøknadsbehandlingOgStartRevurderingInnvilgelse(tac)

            val tiltaksdeltakelse = revurdering.saksopplysninger.tiltaksdeltakelser.first()
            val nyInnvilgelsesperiode = tiltaksdeltakelse.lukketPeriode!!.minusTilOgMed(1)

            val barnetillegg = barnetilleggV2(
                begrunnelse = Begrunnelse.create("barnetillegg begrunnelse"),
                innvilgelsesperioder = nonEmptyListOf(nyInnvilgelsesperiode),
                barn = listOf(barnMedBarnetillegg(fornavn = "Ola"), barnMedBarnetillegg(fornavn = "Kari", perioder = listOf(nyInnvilgelsesperiode.minusTilOgMed(7)))),
            )

            val antallDager = SammenhengendePeriodisering(
                AntallDagerForMeldeperiode(DEFAULT_DAGER_MED_TILTAKSPENGER_FOR_PERIODE),
                nyInnvilgelsesperiode,
            )

            oppdaterRevurderingInnvilgelse(
                tac = tac,
                sakId = sak.id,
                behandlingId = revurdering.id,
                fritekstTilVedtaksbrev = "ny brevtekst",
                begrunnelseVilkårsvurdering = "ny begrunnelse",
                innvilgelsesperioder = innvilgelsesperioder(nyInnvilgelsesperiode),
                barnetillegg = barnetillegg,
                skalSendeVedtaksbrev = false,
                prioriteringBegrunnelse = "Haster",
            )

            val oppdatertBehandling = tac.behandlingContext.rammebehandlingRepo.hent(revurdering.id)

            oppdatertBehandling.resultat.shouldBeInstanceOf<Revurderingsresultat.Innvilgelse>()
            oppdatertBehandling.fritekstTilVedtaksbrev!!.verdi shouldBe "ny brevtekst"
            oppdatertBehandling.begrunnelseVilkårsvurdering!!.verdi shouldBe "ny begrunnelse"
            oppdatertBehandling.vedtaksperiode shouldBe nyInnvilgelsesperiode
            oppdatertBehandling.barnetillegg shouldBe barnetillegg
            oppdatertBehandling.antallDagerPerMeldeperiode shouldBe antallDager
            oppdatertBehandling.skalSendeVedtaksbrev shouldBe false
            oppdatertBehandling.prioritering shouldBe Prioritering.create("Haster")
        }
    }

    @Test
    fun `barn fra en søknad i saken kan velges, men ikke barn fra en søknad som ikke finnes i saken`() {
        withTestApplicationContext { tac ->
            val (sak, søknad, _, revurdering) = iverksettSøknadsbehandlingOgStartRevurderingInnvilgelse(tac)
            val innvilgelsesperiode = revurdering.saksopplysninger.tiltaksdeltakelser.first().lukketPeriode!!

            fun barnetilleggFraSøknad(søknadId: SøknadId) = barnetilleggV2(
                innvilgelsesperioder = nonEmptyListOf(innvilgelsesperiode),
                barn = listOf(barnMedBarnetillegg(kilde = BarnMedBarnetillegg.Kilde.Søknad(søknadId))),
            )

            val (_, _, feilJson) = oppdaterRevurderingInnvilgelse(
                tac = tac,
                sakId = sak.id,
                behandlingId = revurdering.id,
                innvilgelsesperioder = innvilgelsesperioder(innvilgelsesperiode),
                barnetillegg = barnetilleggFraSøknad(SøknadId.random()),
                forventet = ForventetRespons(400, contentType = "application/json; charset=UTF-8"),
            )
            JSONObject(feilJson).getString("kode") shouldBe "ugyldig_barnetillegg"

            oppdaterRevurderingInnvilgelse(
                tac = tac,
                sakId = sak.id,
                behandlingId = revurdering.id,
                innvilgelsesperioder = innvilgelsesperioder(innvilgelsesperiode),
                barnetillegg = barnetilleggFraSøknad(søknad.id),
            )
            tac.behandlingContext.rammebehandlingRepo.hent(revurdering.id).barnetillegg shouldBe barnetilleggFraSøknad(søknad.id)
        }
    }

    /**
     * Simuleringen er best effort: feiler den, lagres behandlingen med beregning uten simulering.
     * Kjører mot postgres fordi det er lagringen og lesingen av den radformen som dekkes.
     * Behandlingen beregnes kun når saken har utbetalte meldeperioder, derav meldekortbehandlingen i oppsettet.
     */
    @Test
    fun `simuleringsfeil gir behandling med beregning uten simulering`() {
        withTestApplicationContextAndPostgres { tac ->
            val (sak, _, _) = iverksettSøknadsbehandling(tac = tac)
            opprettOgIverksettMeldekortbehandling(
                tac = tac,
                sakId = sak.id,
                kjedeId = sak.meldeperiodeKjeder.first().kjedeId,
            )!!
            val (_, revurdering, _) = startRevurderingInnvilgelse(tac = tac, sakId = sak.id)!!

            tac.utbetalingFakeKlient.simulerFeil = KunneIkkeSimulere.UkjentFeil(
                httpKlientUventetStatus(statusCode = 500, body = "simulering feilet"),
            )
            oppdaterRevurderingInnvilgelse(
                tac = tac,
                sakId = sak.id,
                behandlingId = revurdering.id,
                innvilgelsesperioder = innvilgelsesperioder(revurdering.saksopplysninger.tiltaksdeltakelser.first().lukketPeriode!!),
            )

            val oppdatertBehandling = tac.behandlingContext.rammebehandlingRepo.hent(revurdering.id)
            oppdatertBehandling.utbetaling.shouldNotBeNull().simulering.shouldBeNull()
        }
    }
}
