package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.tilbeslutter

import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.ktor.http.HttpStatusCode
import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.dato.april
import no.nav.tiltakspenger.libs.periode.til
import no.nav.tiltakspenger.saksbehandling.barnetillegg.Barnetillegg
import no.nav.tiltakspenger.saksbehandling.behandling.domene.AntallDagerForMeldeperiode
import no.nav.tiltakspenger.saksbehandling.behandling.domene.HjemmelForStansEllerOpphør
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Revurdering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Søknadsbehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.Omgjøringsresultat
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.Revurderingsresultat
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.RammebehandlingResultatTypeDTO
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.RammebehandlingsstatusDTO
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContext
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.innvilgelsesperioder
import no.nav.tiltakspenger.saksbehandling.omgjøring.OmgjørRammevedtak
import no.nav.tiltakspenger.saksbehandling.omgjøring.Omgjøringsgrad
import no.nav.tiltakspenger.saksbehandling.omgjøring.Omgjøringsperiode
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgStartRevurderingStans
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.oppdaterRevurderingStans
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.sendRevurderingInnvilgelseTilBeslutning
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.sendRevurderingTilBeslutningForBehandlingId
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.startRevurderingOmgjøring
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
                valgteHjemler = setOf(HjemmelForStansEllerOpphør.Alder),
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
            val søknadsbehandling = rammevedtakSøknadsbehandling.rammebehandling as Søknadsbehandling

            JSONObject(jsonResponse).getString("status") shouldBe RammebehandlingsstatusDTO.KLAR_TIL_BESLUTNING.name
            JSONObject(jsonResponse).getString("resultat") shouldBe RammebehandlingResultatTypeDTO.REVURDERING_INNVILGELSE.name

            val revurdering =
                tac.behandlingContext.rammebehandlingRepo.hent(
                    BehandlingId.fromString(
                        JSONObject(jsonResponse).getString(
                            "id",
                        ),
                    ),
                )

            revurdering.shouldBeInstanceOf<Revurdering>()
            val søknadsbehandlingsvedtak = sak.rammevedtaksliste.single()

            revurdering.resultat shouldBe Revurderingsresultat.Innvilgelse(
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

    @Test
    fun `kan ikke sende omgjøring uten resultat til beslutning`() {
        withTestApplicationContext { tac ->
            val (sak, _, søknadsvedtak) = iverksettSøknadsbehandling(
                tac,
            )

            val (_, omgjøring) = startRevurderingOmgjøring(
                tac = tac,
                sakId = sak.id,
                rammevedtakIdSomOmgjøres = søknadsvedtak.id,
            )!!

            sendRevurderingTilBeslutningForBehandlingId(
                tac = tac,
                sakId = sak.id,
                behandlingId = omgjøring.id,
                forventetStatus = HttpStatusCode.InternalServerError,
            )

            val behandling = tac.behandlingContext.rammebehandlingRepo.hent(omgjøring.id)

            behandling.status shouldBe Rammebehandlingsstatus.UNDER_BEHANDLING
            behandling.resultat.shouldBeInstanceOf<Omgjøringsresultat.OmgjøringIkkeValgt>()
        }
    }
}
