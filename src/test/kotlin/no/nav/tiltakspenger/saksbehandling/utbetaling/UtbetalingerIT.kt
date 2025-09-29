package no.nav.tiltakspenger.saksbehandling.utbetaling

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.dato.juli
import no.nav.tiltakspenger.libs.dato.november
import no.nav.tiltakspenger.libs.dato.september
import no.nav.tiltakspenger.libs.json.deserialize
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.toDTO
import no.nav.tiltakspenger.saksbehandling.barnetillegg.AntallBarn
import no.nav.tiltakspenger.saksbehandling.behandling.domene.RevurderingType
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.barnetillegg.toBarnetilleggDTO
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.OppdaterRevurderingDTO
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.ValgtHjemmelForStansDTO
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContext
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.antallDagerPerMeldeperiodeDTO
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.barnetillegg
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.tiltaksdeltagelseDTO
import no.nav.tiltakspenger.saksbehandling.objectmothers.førsteMeldekortIverksatt
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettForBehandlingId
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.oppdaterBehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.sendRevurderingInnvilgelseTilBeslutningForBehandlingId
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.startRevurderingForSakId
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.taBehanding
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Satser.Companion.sats
import no.nav.utsjekk.kontrakter.iverksett.IverksettV2Dto
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

class UtbetalingerIT {
    // 7. juli 2025 er en lørdag.
    private val virkningsperiode = Periode(7.juli(2025), 30.november(2025))
    private val satser2025 = sats(1.januar(2025))

    @Test
    @Disabled
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

    @Test
    @Disabled
    fun `Skal etterbetale ved søknadsbehandling som legger til barn`() {
        withTestApplicationContext { tac ->
            val førsteSøknadsperiode = Periode(1.september(2025), 14.september(2025))
            val andreSøknadsperiode = Periode(7.september(2025), 28.september(2025))
            val sak = tac.førsteMeldekortIverksatt(
                periode = førsteSøknadsperiode,
                fnr = Fnr.fromString("12345678911"),
            )
            val (oppdatertSak, _, _, _) = iverksettSøknadsbehandling(
                tac = tac,
                virkningsperiode = andreSøknadsperiode,
                sakId = sak.id,
                barnetillegg = barnetillegg(periode = andreSøknadsperiode, antallBarn = AntallBarn(1)),
                tiltaksdeltagelse = ObjectMother.tiltaksdeltagelseTac(
                    eksternTiltaksdeltagelseId = "TA99999",
                    fom = andreSøknadsperiode.fraOgMed,
                    tom = andreSøknadsperiode.tilOgMed,
                ),
            )

            oppdatertSak.utbetalinger shouldBe listOf(
                oppdatertSak.meldekortVedtaksliste.first().utbetaling,
                oppdatertSak.vedtaksliste.last().utbetaling,
            )

            val revurderingUtbetalingId = oppdatertSak.vedtaksliste.last().utbetaling!!.id

            val utbetalinger = tac.sakContext.sakRepo.hentForSakId(sak.id)!!.utbetalinger
            utbetalinger.size shouldBe 2
            utbetalinger[0].beregning.totalBeløp shouldBe satser2025.sats * 10
            utbetalinger[1].beregning.totalBeløp shouldBe satser2025.sats * 10 + satser2025.satsBarnetillegg * 5

            tac.utbetalingContext.sendUtbetalingerService.send()

            tac.utbetalingContext.utbetalingRepo.hentForUtsjekk().size shouldBe 0
            tac.utbetalingContext.utbetalingRepo.hentUtbetalingJson(revurderingUtbetalingId)!!.let { json ->
                val iverksettDto = deserialize<IverksettV2Dto>(json)
                iverksettDto.vedtak.utbetalinger.first().beløp.toInt() shouldBe satser2025.sats
                iverksettDto.vedtak.utbetalinger.last().beløp.toInt() shouldBe satser2025.satsBarnetillegg
            }
        }
    }

    @Test
    @Disabled
    fun `Feilutbetaling ved stans over utbetalt periode`() {
        withTestApplicationContext { tac ->
            val sak = tac.førsteMeldekortIverksatt(
                periode = virkningsperiode,
                fnr = Fnr.fromString("12345678911"),
            )

            val revurdering = startRevurderingForSakId(
                tac = tac,
                sakId = sak.id,
                type = RevurderingType.STANS,
            )

            oppdaterBehandling(
                tac = tac,
                sakId = sak.id,
                behandlingId = revurdering.id,
                oppdaterBehandlingDTO = OppdaterRevurderingDTO.Stans(
                    fritekstTilVedtaksbrev = "lol",
                    begrunnelseVilkårsvurdering = "what",
                    valgteHjemler = listOf(ValgtHjemmelForStansDTO.Alder),
                    stansFraOgMed = virkningsperiode.fraOgMed,
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
            utbetalingerSomVenter.first().beregning.totalBeløp shouldBe 0

            tac.utbetalingContext.sendUtbetalingerService.send()

            tac.utbetalingContext.utbetalingRepo.hentForUtsjekk().size shouldBe 0
            tac.utbetalingContext.utbetalingRepo.hentUtbetalingJson(revurderingUtbetalingId)!!.let { json ->
                val iverksettDto = deserialize<IverksettV2Dto>(json)
                // Sender tom liste med utbetalinger når hele sakens periode stanses
                iverksettDto.vedtak.utbetalinger.shouldBeEmpty()
            }
        }
    }
}
