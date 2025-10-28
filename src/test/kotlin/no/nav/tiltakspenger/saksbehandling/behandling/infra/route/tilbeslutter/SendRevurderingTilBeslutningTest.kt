package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.tilbeslutter

import arrow.core.nonEmptyListOf
import com.fasterxml.jackson.module.kotlin.readValue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.ktor.http.HttpStatusCode
import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.dato.april
import no.nav.tiltakspenger.libs.json.objectMapper
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.SammenhengendePeriodisering
import no.nav.tiltakspenger.saksbehandling.barnetillegg.Barnetillegg
import no.nav.tiltakspenger.saksbehandling.behandling.domene.AntallDagerForMeldeperiode
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Revurdering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.RevurderingResultat
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.OppdaterRevurderingDTO
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.RammebehandlingResultatDTO
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.RammebehandlingsstatusDTO
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.RevurderingDTO
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.ValgtHjemmelForStansDTO
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContext
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.oppdaterBehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.sendRevurderingInnvilgelseTilBeslutning
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.sendRevurderingStansTilBeslutningForBehandlingId
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.startRevurderingStans
import org.junit.jupiter.api.Test

class SendRevurderingTilBeslutningTest {
    @Test
    fun `kan sende revurdering stans til beslutning`() {
        withTestApplicationContext { tac ->
            val (sak, _, _, revurdering) = startRevurderingStans(tac)

            val stansFraOgMed = sak.førsteDagSomGirRett!!.plusDays(1)

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
            )

            val responseBody = sendRevurderingStansTilBeslutningForBehandlingId(
                tac = tac,
                sakId = sak.id,
                behandlingId = revurdering.id,
                forventetStatus = HttpStatusCode.OK,
            )

            val oppdatertRevurdering = objectMapper.readValue<RevurderingDTO>(responseBody)
            oppdatertRevurdering.status shouldBe RammebehandlingsstatusDTO.KLAR_TIL_BESLUTNING
        }
    }

    @Test
    fun `kan sende revurdering med forlenget innvilgelse til beslutning`() {
        withTestApplicationContext { tac ->
            val søknadsbehandlingVirkningsperiode = Periode(1.april(2025), 10.april(2025))
            val revurderingInnvilgelsesperiode = søknadsbehandlingVirkningsperiode.plusTilOgMed(14L)

            val (_, _, søknadsbehandling, jsonResponse) = sendRevurderingInnvilgelseTilBeslutning(
                tac,
                søknadsbehandlingVirkningsperiode = søknadsbehandlingVirkningsperiode,
                revurderingVirkningsperiode = revurderingInnvilgelsesperiode,
            )

            val behandlingDTO = objectMapper.readValue<RevurderingDTO>(jsonResponse)

            behandlingDTO.status shouldBe RammebehandlingsstatusDTO.KLAR_TIL_BESLUTNING
            behandlingDTO.resultat shouldBe RammebehandlingResultatDTO.REVURDERING_INNVILGELSE

            val revurdering = tac.behandlingContext.behandlingRepo.hent(BehandlingId.fromString(behandlingDTO.id))

            revurdering.shouldBeInstanceOf<Revurdering>()

            revurdering.resultat shouldBe RevurderingResultat.Innvilgelse(
                valgteTiltaksdeltakelser = revurdering.valgteTiltaksdeltakelser!!,
                barnetillegg = Barnetillegg(
                    periodisering = søknadsbehandling.barnetillegg!!.periodisering.nyPeriode(
                        revurderingInnvilgelsesperiode,
                        defaultVerdiDersomDenMangler = søknadsbehandling.barnetillegg!!.periodisering.verdier.first(),
                    ),
                    begrunnelse = søknadsbehandling.barnetillegg!!.begrunnelse,
                ),
                antallDagerPerMeldeperiode = SammenhengendePeriodisering(
                    AntallDagerForMeldeperiode.default,
                    revurderingInnvilgelsesperiode,
                ),
                innvilgelsesperiode = revurderingInnvilgelsesperiode,
            )

            revurdering.virkningsperiode shouldBe revurderingInnvilgelsesperiode
        }
    }
}
