package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.iverksett

import arrow.core.nonEmptyListOf
import io.ktor.http.HttpStatusCode
import no.nav.tiltakspenger.libs.dato.april
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.SammenhengendePeriodisering
import no.nav.tiltakspenger.libs.periodisering.toDTO
import no.nav.tiltakspenger.saksbehandling.barnetillegg.AntallBarn
import no.nav.tiltakspenger.saksbehandling.behandling.domene.AntallDagerForMeldeperiode
import no.nav.tiltakspenger.saksbehandling.behandling.domene.BegrunnelseVilkårsvurdering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.MAKS_DAGER_MED_TILTAKSPENGER_FOR_PERIODE
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.barnetillegg.toBarnetilleggDTO
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.OppdaterRevurderingDTO
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.ValgtHjemmelForStansDTO
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContext
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.barnetillegg
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettForBehandlingId
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.oppdaterBehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.sendRevurderingInnvilgelseTilBeslutningForBehandlingId
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.sendRevurderingStansTilBeslutningForBehandlingId
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.startRevurderingInnvilgelse
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.startRevurderingStans
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.taBehanding
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.infra.route.TiltaksdeltakelsePeriodeDTO
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.infra.route.toDTO
import org.junit.jupiter.api.Test

internal class IverksettRevurderingTest {
    @Test
    fun `kan iverksette revurdering stans`() {
        withTestApplicationContext { tac ->
            val (sak, _, søknadsbehandling, revurdering) = startRevurderingStans(tac)

            oppdaterBehandling(
                tac = tac,
                sakId = sak.id,
                behandlingId = revurdering.id,
                oppdaterBehandlingDTO = OppdaterRevurderingDTO.Stans(
                    begrunnelseVilkårsvurdering = null,
                    fritekstTilVedtaksbrev = null,
                    valgteHjemler = nonEmptyListOf(ValgtHjemmelForStansDTO.Alder),
                    stansFraOgMed = søknadsbehandling.virkningsperiode!!.fraOgMed,
                ),
                forventetStatus = HttpStatusCode.OK,
            )

            sendRevurderingStansTilBeslutningForBehandlingId(
                tac,
                sak.id,
                revurdering.id,
            )
            taBehanding(tac, sak.id, revurdering.id, saksbehandler = ObjectMother.beslutter())
            iverksettForBehandlingId(tac, sak.id, revurdering.id)
        }
    }

    @Test
    fun `kan iverksette revurdering innvilgelsesperiode fremover`() {
        withTestApplicationContext { tac ->
            val søknadsbehandlingVirkningsperiode = Periode(1.april(2025), 10.april(2025))
            val revurderingInnvilgelsesperiode = søknadsbehandlingVirkningsperiode.plusTilOgMed(14L)

            val (sak, _, _, revurdering) = startRevurderingInnvilgelse(
                tac,
                søknadsbehandlingVirkningsperiode = søknadsbehandlingVirkningsperiode,
                revurderingVirkningsperiode = revurderingInnvilgelsesperiode,
            )

            val tiltaksdeltagelse = revurdering.saksopplysninger.tiltaksdeltagelser.single()

            val barnetillegg = barnetillegg(
                begrunnelse = BegrunnelseVilkårsvurdering("barnetillegg begrunnelse"),
                periode = revurderingInnvilgelsesperiode,
                antallBarn = AntallBarn(1),
            )

            val antallDager = SammenhengendePeriodisering(
                AntallDagerForMeldeperiode(MAKS_DAGER_MED_TILTAKSPENGER_FOR_PERIODE),
                revurderingInnvilgelsesperiode,
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
                            periode = tiltaksdeltagelse.periode!!.toDTO(),
                        ),
                    ),
                    innvilgelsesperiode = revurderingInnvilgelsesperiode.toDTO(),
                    barnetillegg = barnetillegg.toBarnetilleggDTO(),
                    antallDagerPerMeldeperiodeForPerioder = antallDager.toDTO(),
                ),
            )

            sendRevurderingInnvilgelseTilBeslutningForBehandlingId(
                tac,
                sak.id,
                revurdering.id,
            )
            taBehanding(tac, sak.id, revurdering.id, saksbehandler = ObjectMother.beslutter())
            iverksettForBehandlingId(tac, sak.id, revurdering.id)
        }
    }

    @Test
    fun `kan iverksette revurdering innvilgelsesperiode bakover`() {
        withTestApplicationContext { tac ->
            val søknadsbehandlingVirkningsperiode = Periode(1.april(2025), 10.april(2025))
            val revurderingInnvilgelsesperiode = søknadsbehandlingVirkningsperiode.minusFraOgMed(14L)

            val (sak, _, _, revurdering) = startRevurderingInnvilgelse(
                tac,
                søknadsbehandlingVirkningsperiode = søknadsbehandlingVirkningsperiode,
                revurderingVirkningsperiode = revurderingInnvilgelsesperiode,
            )

            val tiltaksdeltagelse = revurdering.saksopplysninger.tiltaksdeltagelser.single()

            val barnetillegg = barnetillegg(
                begrunnelse = BegrunnelseVilkårsvurdering("barnetillegg begrunnelse"),
                periode = revurderingInnvilgelsesperiode,
                antallBarn = AntallBarn(1),
            )

            val antallDager = SammenhengendePeriodisering(
                AntallDagerForMeldeperiode(MAKS_DAGER_MED_TILTAKSPENGER_FOR_PERIODE),
                revurderingInnvilgelsesperiode,
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
                            periode = tiltaksdeltagelse.periode!!.toDTO(),
                        ),
                    ),
                    innvilgelsesperiode = revurderingInnvilgelsesperiode.toDTO(),
                    barnetillegg = barnetillegg.toBarnetilleggDTO(),
                    antallDagerPerMeldeperiodeForPerioder = antallDager.toDTO(),
                ),
            )

            sendRevurderingInnvilgelseTilBeslutningForBehandlingId(
                tac,
                sak.id,
                revurdering.id,
            )
            taBehanding(tac, sak.id, revurdering.id, saksbehandler = ObjectMother.beslutter())
            iverksettForBehandlingId(tac, sak.id, revurdering.id)
        }
    }
}
