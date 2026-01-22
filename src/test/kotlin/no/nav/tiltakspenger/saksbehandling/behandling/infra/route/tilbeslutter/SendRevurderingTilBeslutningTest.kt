package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.tilbeslutter

import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.ktor.http.HttpStatusCode
import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.dato.april
import no.nav.tiltakspenger.libs.periodisering.til
import no.nav.tiltakspenger.saksbehandling.barnetillegg.Barnetillegg
import no.nav.tiltakspenger.saksbehandling.behandling.domene.AntallDagerForMeldeperiode
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Revurdering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.RevurderingResultat
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Søknadsbehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.ValgtHjemmelForStans
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.RammebehandlingResultatTypeDTO
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.RammebehandlingsstatusDTO
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContext
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.innvilgelsesperioder
import no.nav.tiltakspenger.saksbehandling.omgjøring.OmgjørRammevedtak
import no.nav.tiltakspenger.saksbehandling.omgjøring.Omgjøringsgrad
import no.nav.tiltakspenger.saksbehandling.omgjøring.Omgjøringsperiode
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgStartRevurderingStans
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.oppdaterRevurderingStans
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.sendRevurderingInnvilgelseTilBeslutning
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.sendRevurderingTilBeslutningForBehandlingId
import org.json.JSONObject
import org.junit.jupiter.api.Test

class SendRevurderingTilBeslutningTest {
    @Test
    fun `kan sende revurdering stans til beslutning`() {
        withTestApplicationContext { tac ->
            val (sak, _, _, revurdering) = iverksettSøknadsbehandlingOgStartRevurderingStans(tac)

            val stansFraOgMed = sak.førsteDagSomGirRett!!.plusDays(1)

            oppdaterRevurderingStans(
                tac = tac,
                sakId = sak.id,
                behandlingId = revurdering.id,
                begrunnelseVilkårsvurdering = null,
                fritekstTilVedtaksbrev = null,
                valgteHjemler = setOf(ValgtHjemmelForStans.Alder),
                stansFraOgMed = stansFraOgMed,
                harValgtStansFraFørsteDagSomGirRett = false,
            )

            val responseBody = sendRevurderingTilBeslutningForBehandlingId(
                tac = tac,
                sakId = sak.id,
                behandlingId = revurdering.id,
                forventetStatus = HttpStatusCode.OK,
            )

            JSONObject(responseBody).getString("status") shouldBe RammebehandlingsstatusDTO.KLAR_TIL_BESLUTNING.name
        }
    }

    @Test
    fun `kan sende revurdering med forlenget innvilgelse til beslutning`() {
        withTestApplicationContext { tac ->
            val søknadsbehandlingInnvilgelsesperiode = 1 til 10.april(2025)
            val revurderingInnvilgelsesperiode = søknadsbehandlingInnvilgelsesperiode.plusTilOgMed(14L)

            val (sak, _, rammevedtakSøknadsbehandling, jsonResponse) = sendRevurderingInnvilgelseTilBeslutning(
                tac,
                søknadsbehandlingInnvilgelsesperioder = innvilgelsesperioder(søknadsbehandlingInnvilgelsesperiode),
                revurderingInnvilgelsesperioder = innvilgelsesperioder(revurderingInnvilgelsesperiode),
            )
            val søknadsbehandling = rammevedtakSøknadsbehandling.behandling as Søknadsbehandling

            JSONObject(jsonResponse).getString("status") shouldBe RammebehandlingsstatusDTO.KLAR_TIL_BESLUTNING.name
            JSONObject(jsonResponse).getString("resultat") shouldBe RammebehandlingResultatTypeDTO.REVURDERING_INNVILGELSE.name

            val revurdering =
                tac.behandlingContext.behandlingRepo.hent(BehandlingId.fromString(JSONObject(jsonResponse).getString("id")))

            revurdering.shouldBeInstanceOf<Revurdering>()
            val søknadsbehandlingsvedtak = sak.rammevedtaksliste.single()

            revurdering.resultat shouldBe RevurderingResultat.Innvilgelse(
                barnetillegg = Barnetillegg(
                    periodisering = søknadsbehandling.barnetillegg!!.periodisering.nyPeriode(
                        revurderingInnvilgelsesperiode,
                        defaultVerdiDersomDenMangler = søknadsbehandling.barnetillegg.periodisering.verdier.first(),
                    ),
                    begrunnelse = søknadsbehandling.barnetillegg.begrunnelse,
                ),
                innvilgelsesperioder = innvilgelsesperioder(
                    periode = revurderingInnvilgelsesperiode,
                    valgtTiltaksdeltakelse = revurdering.valgteTiltaksdeltakelser!!.single().verdi,
                    antallDagerPerMeldeperiode = AntallDagerForMeldeperiode.default,
                ),
                omgjørRammevedtak = OmgjørRammevedtak(
                    Omgjøringsperiode(
                        rammevedtakId = søknadsbehandlingsvedtak.id,
                        periode = 1 til 10.april(2025),
                        omgjøringsgrad = Omgjøringsgrad.HELT,
                    ),
                ),
            )

            revurdering.vedtaksperiode shouldBe revurderingInnvilgelsesperiode
        }
    }
}
