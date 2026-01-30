package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.oppdater

import io.kotest.matchers.shouldBe
import io.ktor.http.HttpStatusCode
import no.nav.tiltakspenger.libs.dato.april
import no.nav.tiltakspenger.libs.periodisering.PeriodeMedVerdi
import no.nav.tiltakspenger.libs.periodisering.til
import no.nav.tiltakspenger.libs.periodisering.tilIkkeTomPeriodisering
import no.nav.tiltakspenger.saksbehandling.barnetillegg.Barnetillegg
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Revurdering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Revurderingsresultat
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContext
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.innvilgelsesperioder
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgStartRevurderingOmgjøring
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.oppdaterRevurderingOmgjøring
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.oppdaterSaksopplysningerForBehandlingId
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltakDeltakerstatus
import org.junit.jupiter.api.Test

class OppdaterRevurderingOmgjøringRouteTest {

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
            val barnetillegg = Barnetillegg.utenBarnetillegg((3 til 9.april(2025)))
            val innvilgelsesperioder = innvilgelsesperioder(
                nyOmgjøringsperiodeEtterOppdatering,
                tiltaksdeltakelseVedOpprettelseAvRevurdering,
            )

            val (_, oppdatertRevurdering) = oppdaterRevurderingOmgjøring(
                tac = tac,
                sakId = sak.id,
                behandlingId = rammevedtakRevurdering.id,
                fritekstTilVedtaksbrev = "asdf",
                begrunnelseVilkårsvurdering = null,
                innvilgelsesperioder = innvilgelsesperioder,
                barnetillegg = barnetillegg,
                forventetStatus = HttpStatusCode.OK,
            )
            (oppdatertRevurdering as Revurdering).erFerdigutfylt() shouldBe true
            // Forventer at saksopplysningene er oppdatert, mens resultatet er ubesudlet.
            oppdatertRevurdering.saksopplysninger.tiltaksdeltakelser.single() shouldBe avbruttTiltaksdeltakelse
            val resultat = oppdatertRevurdering.resultat as Revurderingsresultat.Omgjøring
            // Kommentar jah: Beklager for alt todomain-greiene. Her bør det expectes på eksplisitte verdier uten å bruke domenekode for mapping.
            resultat.barnetillegg shouldBe barnetillegg
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
}
