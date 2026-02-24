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
import no.nav.tiltakspenger.libs.dato.november
import no.nav.tiltakspenger.libs.dato.september
import no.nav.tiltakspenger.libs.json.deserialize
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.libs.periode.til
import no.nav.tiltakspenger.libs.satser.Satser.Companion.sats
import no.nav.tiltakspenger.saksbehandling.barnetillegg.AntallBarn
import no.nav.tiltakspenger.saksbehandling.behandling.domene.HjemmelForStans
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.RevurderingsresultatType
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContext
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.barnetillegg
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.innvilgelsesperioder
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.tiltaksdeltakelse
import no.nav.tiltakspenger.saksbehandling.objectmothers.førsteMeldekortIverksatt
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettForBehandlingId
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.oppdaterOmgjøringOpphør
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.oppdaterRevurderingInnvilgelse
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.oppdaterRevurderingStans
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.sendRevurderingTilBeslutningForBehandlingId
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.startRevurderingForSakId
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.startRevurderingOmgjøring
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.taBehandling
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakerId
import no.nav.tiltakspenger.saksbehandling.utbetaling.infra.http.utsjekk.kontrakter.iverksett.IverksettV2Dto
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
                type = RevurderingsresultatType.INNVILGELSE,
            )!!

            oppdaterRevurderingInnvilgelse(
                tac = tac,
                sakId = sak.id,
                behandlingId = revurdering.id,
                fritekstTilVedtaksbrev = "lol",
                begrunnelseVilkårsvurdering = "what",
                innvilgelsesperioder = innvilgelsesperioder(vedtaksperiode),
                barnetillegg = barnetillegg(
                    periode = vedtaksperiode,
                    antallBarn = AntallBarn(2),
                ),
            )

            sendRevurderingTilBeslutningForBehandlingId(
                tac,
                sak.id,
                revurdering.id,
            )
            taBehandling(tac, sak.id, revurdering.id, saksbehandler = ObjectMother.beslutter())

            val (oppdatertSak) = iverksettForBehandlingId(tac, sak.id, revurdering.id, utførJobber = false)!!

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

            val tiltaksdeltakelse = tiltaksdeltakelse(
                førsteSøknadsperiode.fraOgMed til andreSøknadsperiode.tilOgMed,
                internDeltakelseId = TiltaksdeltakerId.random(),
            )

            val sak = tac.førsteMeldekortIverksatt(
                innvilgelsesperiode = førsteSøknadsperiode,
                fnr = Fnr.fromString("12345678911"),
            )
            val (oppdatertSak, _, _, _) = iverksettSøknadsbehandling(
                tac = tac,
                tiltaksdeltakelse = tiltaksdeltakelse,
                innvilgelsesperioder = innvilgelsesperioder(andreSøknadsperiode, tiltaksdeltakelse),
                sakId = sak.id,
                barnetillegg = barnetillegg(periode = andreSøknadsperiode, antallBarn = AntallBarn(1)),
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
                type = RevurderingsresultatType.STANS,
            )!!

            oppdaterRevurderingStans(
                tac = tac,
                sakId = sak.id,
                behandlingId = revurdering.id,
                fritekstTilVedtaksbrev = "lol",
                begrunnelseVilkårsvurdering = "what",
                valgteHjemler = setOf(HjemmelForStans.Alder),
                stansFraOgMed = vedtaksperiode.fraOgMed,
                harValgtStansFraFørsteDagSomGirRett = false,
            )

            sendRevurderingTilBeslutningForBehandlingId(
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
                type = RevurderingsresultatType.STANS,
            )!!

            oppdaterRevurderingStans(
                tac = tac,
                sakId = sak.id,
                behandlingId = revurdering.id,
                fritekstTilVedtaksbrev = "lol",
                begrunnelseVilkårsvurdering = "what",
                valgteHjemler = setOf(HjemmelForStans.Alder),
                stansFraOgMed = vedtaksperiode.fraOgMed,
                harValgtStansFraFørsteDagSomGirRett = false,
            )

            val bodyAsText = sendRevurderingTilBeslutningForBehandlingId(
                tac,
                sak.id,
                revurdering.id,
                forventetStatus = HttpStatusCode.BadRequest,
            )

            @Language("JSON")
            val expected = """
                {
                  "melding": "Behandling med feilutbetaling støttes ikke på nåværende tidspunkt.",
                  "kode": "støtter_ikke_feilutbetaling"
                }                
            """.trimIndent()

            bodyAsText shouldEqualJson expected

            tac.behandlingContext.rammebehandlingRepo.hent(revurdering.id).status shouldBe Rammebehandlingsstatus.UNDER_BEHANDLING
        }
    }

    // TODO: see above
    @Test
    fun `Behandling med feilutbetaling ved opphør over utbetalt periode skal ikke kunne sendes til beslutning`() {
        val clock = TikkendeKlokke(fixedClockAt(1.desember(2025)))
        withTestApplicationContext(clock = clock) { tac ->
            val sak = tac.førsteMeldekortIverksatt(
                innvilgelsesperiode = vedtaksperiode,
                fnr = Fnr.fromString("12345678911"),
            )

            val (_, omgjøring) = startRevurderingOmgjøring(
                tac = tac,
                sakId = sak.id,
                rammevedtakIdSomOmgjøres = sak.rammevedtaksliste.first().id,
            )!!

            oppdaterOmgjøringOpphør(
                tac = tac,
                sakId = sak.id,
                behandlingId = omgjøring.id,
                vedtaksperiode = vedtaksperiode,
            )

            val bodyAsText = sendRevurderingTilBeslutningForBehandlingId(
                tac,
                sak.id,
                omgjøring.id,
                forventetStatus = HttpStatusCode.BadRequest,
            )

            @Language("JSON")
            val expected = """
                {
                  "melding": "Behandling med feilutbetaling støttes ikke på nåværende tidspunkt.",
                  "kode": "støtter_ikke_feilutbetaling"
                }                
            """.trimIndent()

            bodyAsText shouldEqualJson expected

            tac.behandlingContext.rammebehandlingRepo.hent(omgjøring.id).status shouldBe Rammebehandlingsstatus.UNDER_BEHANDLING
        }
    }
}
