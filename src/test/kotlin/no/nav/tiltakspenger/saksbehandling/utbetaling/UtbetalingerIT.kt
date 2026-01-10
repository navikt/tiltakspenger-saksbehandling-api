package no.nav.tiltakspenger.saksbehandling.utbetaling

import io.kotest.assertions.json.shouldEqualJson
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.ktor.http.HttpStatusCode
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.TikkendeKlokke
import no.nav.tiltakspenger.libs.common.fixedClockAt
import no.nav.tiltakspenger.libs.dato.desember
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.dato.juli
import no.nav.tiltakspenger.libs.dato.mars
import no.nav.tiltakspenger.libs.dato.november
import no.nav.tiltakspenger.libs.dato.september
import no.nav.tiltakspenger.libs.json.deserialize
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.satser.Satser.Companion.sats
import no.nav.tiltakspenger.saksbehandling.barnetillegg.AntallBarn
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus
import no.nav.tiltakspenger.saksbehandling.behandling.domene.RevurderingType
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.barnetillegg.toBarnetilleggDTO
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.OppdaterRevurderingDTO
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.ValgtHjemmelForStansDTO
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContext
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.barnetillegg
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.innvilgelsesperioderDTO
import no.nav.tiltakspenger.saksbehandling.objectmothers.førsteMeldekortIverksatt
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettForBehandlingId
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.oppdaterBehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.sendRevurderingInnvilgelseTilBeslutningForBehandlingId
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.startRevurderingForSakId
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.taBehandling
import no.nav.utsjekk.kontrakter.iverksett.IverksettV2Dto
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

class UtbetalingerIT {
    // 7. juli 2025 er en lørdag.
    private val vedtaksperiode = Periode(7.juli(2025), 30.november(2025))
    private val satser2025 = sats(1.januar(2025))

    @Test
    fun `Skal etterbetale ved revurdering som legger til barn`() {
        val clock = TikkendeKlokke(fixedClockAt(1.desember(2025)))
        withTestApplicationContext(clock = clock) { tac ->
            val sak = tac.førsteMeldekortIverksatt(
                innvilgelsesperiode = vedtaksperiode,
                fnr = Fnr.fromString("12345678911"),
            )

            val (_, revurdering, _) = startRevurderingForSakId(
                tac = tac,
                sakId = sak.id,
                type = RevurderingType.INNVILGELSE,
            )!!

            oppdaterBehandling(
                tac = tac,
                sakId = sak.id,
                behandlingId = revurdering.id,
                oppdaterBehandlingDTO = OppdaterRevurderingDTO.Innvilgelse(
                    fritekstTilVedtaksbrev = "lol",
                    begrunnelseVilkårsvurdering = "what",
                    innvilgelsesperioder = revurdering.innvilgelsesperioderDTO(vedtaksperiode),
                    barnetillegg = barnetillegg(
                        periode = vedtaksperiode,
                        antallBarn = AntallBarn(2),
                    ).toBarnetilleggDTO(),
                ),
            )

            sendRevurderingInnvilgelseTilBeslutningForBehandlingId(
                tac,
                sak.id,
                revurdering.id,
            )
            taBehandling(tac, sak.id, revurdering.id, saksbehandler = ObjectMother.beslutter())

            val (oppdatertSak) = iverksettForBehandlingId(tac, sak.id, revurdering.id)!!

            oppdatertSak.utbetalinger shouldBe listOf(
                oppdatertSak.meldekortvedtaksliste.first().utbetaling,
                oppdatertSak.rammevedtaksliste.last().utbetaling,
            )

            val revurderingUtbetalingId = oppdatertSak.rammevedtaksliste.last().utbetaling!!.id

            val utbetalingerSomVenter = tac.utbetalingContext.utbetalingRepo.hentForUtsjekk()
            utbetalingerSomVenter.size shouldBe 1
            utbetalingerSomVenter.first().beregning.totalBeløp shouldBe satser2025.sats * 10 + satser2025.satsBarnetillegg * 10 * 2

            tac.utbetalingContext.sendUtbetalingerService.sendUtbetalingerTilHelved()

            tac.utbetalingContext.utbetalingRepo.hentForUtsjekk().size shouldBe 0
            tac.utbetalingContext.utbetalingRepo.hentUtbetalingJson(revurderingUtbetalingId)!!.let { json ->
                val iverksettDto = deserialize<IverksettV2Dto>(json)
                iverksettDto.vedtak.utbetalinger.first().beløp.toInt() shouldBe satser2025.sats
                iverksettDto.vedtak.utbetalinger.last().beløp.toInt() shouldBe satser2025.satsBarnetillegg * 2
            }
        }
    }

    @Test
    fun `Skal etterbetale ved søknadsbehandling som legger til barn`() {
        val clock = TikkendeKlokke(fixedClockAt(1.desember(2025)))
        withTestApplicationContext(clock = clock) { tac ->
            val førsteSøknadsperiode = Periode(1.september(2025), 14.september(2025))
            val andreSøknadsperiode = Periode(7.september(2025), 28.september(2025))
            val sak = tac.førsteMeldekortIverksatt(
                innvilgelsesperiode = førsteSøknadsperiode,
                fnr = Fnr.fromString("12345678911"),
            )
            val (oppdatertSak, _, _, _) = iverksettSøknadsbehandling(
                tac = tac,
                vedtaksperiode = andreSøknadsperiode,
                sakId = sak.id,
                barnetillegg = barnetillegg(periode = andreSøknadsperiode, antallBarn = AntallBarn(1)),
                tiltaksdeltakelse = ObjectMother.tiltaksdeltakelseTac(
                    eksternTiltaksdeltakelseId = "TA99999",
                    fom = andreSøknadsperiode.fraOgMed,
                    tom = andreSøknadsperiode.tilOgMed,
                ),
            )

            oppdatertSak.utbetalinger shouldBe listOf(
                oppdatertSak.meldekortvedtaksliste.first().utbetaling,
                oppdatertSak.rammevedtaksliste.last().utbetaling,
            )

            val revurderingUtbetalingId = oppdatertSak.rammevedtaksliste.last().utbetaling!!.id

            val utbetalinger = tac.sakContext.sakRepo.hentForSakId(sak.id)!!.utbetalinger
            utbetalinger.size shouldBe 2
            utbetalinger[0].beregning.totalBeløp shouldBe satser2025.sats * 10
            utbetalinger[1].beregning.totalBeløp shouldBe satser2025.sats * 10 + satser2025.satsBarnetillegg * 5

            tac.utbetalingContext.sendUtbetalingerService.sendUtbetalingerTilHelved()

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
                innvilgelsesperiode = vedtaksperiode,
                fnr = Fnr.fromString("12345678911"),
            )

            val (_, revurdering, _) = startRevurderingForSakId(
                tac = tac,
                sakId = sak.id,
                type = RevurderingType.STANS,
            )!!

            oppdaterBehandling(
                tac = tac,
                sakId = sak.id,
                behandlingId = revurdering.id,
                oppdaterBehandlingDTO = OppdaterRevurderingDTO.Stans(
                    fritekstTilVedtaksbrev = "lol",
                    begrunnelseVilkårsvurdering = "what",
                    valgteHjemler = listOf(ValgtHjemmelForStansDTO.Alder),
                    stansFraOgMed = vedtaksperiode.fraOgMed,
                    stansTilOgMed = null,
                    harValgtStansFraFørsteDagSomGirRett = false,
                    harValgtStansTilSisteDagSomGirRett = true,
                ),
            )

            sendRevurderingInnvilgelseTilBeslutningForBehandlingId(
                tac,
                sak.id,
                revurdering.id,
            )
            taBehandling(tac, sak.id, revurdering.id, saksbehandler = ObjectMother.beslutter())

            val (oppdatertSak) = iverksettForBehandlingId(tac, sak.id, revurdering.id)!!

            oppdatertSak.utbetalinger shouldBe listOf(
                oppdatertSak.meldekortvedtaksliste.first().utbetaling,
                oppdatertSak.rammevedtaksliste.last().utbetaling,
            )

            val revurderingUtbetalingId = oppdatertSak.rammevedtaksliste.last().utbetaling!!.id

            val utbetalingerSomVenter = tac.utbetalingContext.utbetalingRepo.hentForUtsjekk()
            utbetalingerSomVenter.size shouldBe 1
            utbetalingerSomVenter.first().beregning.totalBeløp shouldBe 0

            tac.utbetalingContext.sendUtbetalingerService.sendUtbetalingerTilHelved()

            tac.utbetalingContext.utbetalingRepo.hentForUtsjekk().size shouldBe 0
            tac.utbetalingContext.utbetalingRepo.hentUtbetalingJson(revurderingUtbetalingId)!!.let { json ->
                val iverksettDto = deserialize<IverksettV2Dto>(json)
                // Sender tom liste med utbetalinger når hele sakens periode stanses
                iverksettDto.vedtak.utbetalinger.shouldBeEmpty()
            }
        }
    }

    // TODO: fjern denne og enable den forrige når feilutbetaling støttes igjen
    @Test
    fun `Behandling med feilutbetaling ved stans over utbetalt periode skal ikke kunne sendes til beslutning`() {
        val clock = TikkendeKlokke(fixedClockAt(1.desember(2025)))
        withTestApplicationContext(clock = clock) { tac ->
            val sak = tac.førsteMeldekortIverksatt(
                innvilgelsesperiode = vedtaksperiode,
                fnr = Fnr.fromString("12345678911"),
            )

            val (_, revurdering, _) = startRevurderingForSakId(
                tac = tac,
                sakId = sak.id,
                type = RevurderingType.STANS,
            )!!

            oppdaterBehandling(
                tac = tac,
                sakId = sak.id,
                behandlingId = revurdering.id,
                oppdaterBehandlingDTO = OppdaterRevurderingDTO.Stans(
                    fritekstTilVedtaksbrev = "lol",
                    begrunnelseVilkårsvurdering = "what",
                    valgteHjemler = listOf(ValgtHjemmelForStansDTO.Alder),
                    stansFraOgMed = vedtaksperiode.fraOgMed,
                    stansTilOgMed = null,
                    harValgtStansFraFørsteDagSomGirRett = false,
                    harValgtStansTilSisteDagSomGirRett = true,
                ),
            )

            val bodyAsText = sendRevurderingInnvilgelseTilBeslutningForBehandlingId(
                tac,
                sak.id,
                revurdering.id,
                forventetStatus = HttpStatusCode.BadRequest,
            )

            @Language("JSON")
            val expected = """
                {
                  "melding": "Behandling med feilutbetaling støttes ikke på nåværende tidspunkt",
                  "kode": "støtter_ikke_feilutbetaling"
                }                
            """.trimIndent()

            bodyAsText shouldEqualJson expected

            tac.behandlingContext.behandlingRepo.hent(revurdering.id).status shouldBe Rammebehandlingsstatus.UNDER_BEHANDLING
        }
    }
}
