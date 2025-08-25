package no.nav.tiltakspenger.saksbehandling.utbetaling

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.dato.juli
import no.nav.tiltakspenger.libs.dato.november
import no.nav.tiltakspenger.libs.json.deserialize
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.toDTO
import no.nav.tiltakspenger.saksbehandling.barnetillegg.AntallBarn
import no.nav.tiltakspenger.saksbehandling.behandling.domene.RevurderingType
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.barnetillegg.toBarnetilleggDTO
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.OppdaterRevurderingDTO
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContext
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.antallDagerPerMeldeperiodeDTO
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.barnetillegg
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.tiltaksdeltagelseDTO
import no.nav.tiltakspenger.saksbehandling.objectmothers.førsteMeldekortIverksatt
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettForBehandlingId
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.oppdaterBehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.sendRevurderingInnvilgelseTilBeslutningForBehandlingId
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.startRevurderingForSakId
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.taBehanding
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Satser.Companion.sats
import no.nav.utsjekk.kontrakter.iverksett.IverksettV2Dto
import org.junit.jupiter.api.Test

class UtbetalingerIT {
    private val virkningsperiode = Periode(7.juli(2025), 30.november(2025))
    private val satser2025 = sats(1.januar(2025))

    @Test
    fun `Skal etterbetale ved revurdering som legger til barn`() {
        withTestApplicationContext { tac ->
            val sak = tac.førsteMeldekortIverksatt(
                periode = virkningsperiode,
                fnr = Fnr.fromString("12345678911"),
            )

            val revurdering = startRevurderingForSakId(
                tac = tac,
                sakId = sak.id,
                type = RevurderingType.INNVILGELSE,
            )

            oppdaterBehandling(
                tac = tac,
                sakId = sak.id,
                behandlingId = revurdering.id,
                oppdaterBehandlingDTO = OppdaterRevurderingDTO.Innvilgelse(
                    fritekstTilVedtaksbrev = "lol",
                    begrunnelseVilkårsvurdering = "what",
                    innvilgelsesperiode = virkningsperiode.toDTO(),
                    valgteTiltaksdeltakelser = revurdering.tiltaksdeltagelseDTO(),
                    barnetillegg = barnetillegg(
                        periode = virkningsperiode,
                        antallBarn = AntallBarn(2),
                    ).toBarnetilleggDTO(),
                    antallDagerPerMeldeperiodeForPerioder = revurdering.antallDagerPerMeldeperiodeDTO(virkningsperiode),
                ),
            )

            sendRevurderingInnvilgelseTilBeslutningForBehandlingId(
                tac,
                sak.id,
                revurdering.id,
            )
            taBehanding(tac, sak.id, revurdering.id, saksbehandler = ObjectMother.beslutter())

            val (oppdatertSak) = iverksettForBehandlingId(tac, sak.id, revurdering.id)

            oppdatertSak.utbetalinger shouldBe listOf(
                oppdatertSak.meldekortVedtaksliste.first().utbetaling,
                oppdatertSak.vedtaksliste.last().utbetaling,
            )

            val revurderingUtbetalingId = oppdatertSak.vedtaksliste.last().utbetaling!!.id

            val utbetalingerSomVenter = tac.utbetalingContext.utbetalingRepo.hentForUtsjekk()
            utbetalingerSomVenter.size shouldBe 1
            utbetalingerSomVenter.first().beregning.totalBeløp shouldBe satser2025.sats * 10 + satser2025.satsBarnetillegg * 10 * 2

            tac.utbetalingContext.sendUtbetalingerService.send()

            tac.utbetalingContext.utbetalingRepo.hentForUtsjekk().size shouldBe 0
            tac.utbetalingContext.utbetalingRepo.hentUtbetalingJson(revurderingUtbetalingId)!!.let { json ->
                val iverksettDto = deserialize<IverksettV2Dto>(json)
                iverksettDto.vedtak.utbetalinger.first().beløp.toInt() shouldBe satser2025.sats
                iverksettDto.vedtak.utbetalinger.last().beløp.toInt() shouldBe satser2025.satsBarnetillegg * 2
            }
        }
    }
}
