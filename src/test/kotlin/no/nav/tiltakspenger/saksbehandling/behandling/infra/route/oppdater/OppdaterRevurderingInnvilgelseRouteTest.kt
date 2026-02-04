package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.oppdater

import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.tiltakspenger.libs.periodisering.SammenhengendePeriodisering
import no.nav.tiltakspenger.saksbehandling.barnetillegg.AntallBarn
import no.nav.tiltakspenger.saksbehandling.behandling.domene.AntallDagerForMeldeperiode
import no.nav.tiltakspenger.saksbehandling.behandling.domene.DEFAULT_DAGER_MED_TILTAKSPENGER_FOR_PERIODE
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.Revurderingsresultat
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContext
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.Begrunnelse
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.barnetillegg
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.innvilgelsesperioder
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgStartRevurderingInnvilgelse
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.oppdaterRevurderingInnvilgelse
import org.junit.jupiter.api.Test

class OppdaterRevurderingInnvilgelseRouteTest {

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

            oppdaterRevurderingInnvilgelse(
                tac = tac,
                sakId = sak.id,
                behandlingId = revurdering.id,
                fritekstTilVedtaksbrev = "ny brevtekst",
                begrunnelseVilkårsvurdering = "ny begrunnelse",
                innvilgelsesperioder = innvilgelsesperioder(nyInnvilgelsesperiode),
                barnetillegg = barnetillegg,
            )

            val oppdatertBehandling = tac.behandlingContext.rammebehandlingRepo.hent(revurdering.id)

            oppdatertBehandling.resultat.shouldBeInstanceOf<Revurderingsresultat.Innvilgelse>()
            oppdatertBehandling.fritekstTilVedtaksbrev!!.verdi shouldBe "ny brevtekst"
            oppdatertBehandling.begrunnelseVilkårsvurdering!!.verdi shouldBe "ny begrunnelse"
            oppdatertBehandling.vedtaksperiode shouldBe nyInnvilgelsesperiode
            oppdatertBehandling.barnetillegg shouldBe barnetillegg
            oppdatertBehandling.antallDagerPerMeldeperiode shouldBe antallDager
        }
    }
}
