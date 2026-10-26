package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.forlengelse

import arrow.core.nonEmptyListOf
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.dato.april
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.dato.juli
import no.nav.tiltakspenger.libs.dato.juni
import no.nav.tiltakspenger.libs.dato.mai
import no.nav.tiltakspenger.libs.dato.mars
import no.nav.tiltakspenger.libs.ktor.test.common.ForventetRespons
import no.nav.tiltakspenger.libs.periode.til
import no.nav.tiltakspenger.libs.periodisering.PeriodeMedVerdi
import no.nav.tiltakspenger.saksbehandling.barnetillegg.AntallBarn
import no.nav.tiltakspenger.saksbehandling.barnetillegg.Barnetillegg
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.forlengelse.ForlengerRammevedtak
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.Revurderingsresultat
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.RammebehandlingResultatTypeDTO
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContext
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.barnetillegg
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.barnetilleggMedIdent
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.innvilgelsesperioder
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.forhåndsvisVedtaksbrevForBehandlingId
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettForBehandlingId
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.oppdaterForlengelse
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.sendRevurderingTilBeslutningForBehandlingId
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.startForlengelse
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.taRammebehandlinger
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.søknad.domene.BarnetilleggFraSøknad
import no.nav.tiltakspenger.saksbehandling.vedtak.Rammevedtak
import org.json.JSONObject
import org.junit.jupiter.api.Test
import java.time.LocalDate

/**
 * Vedtaket som forlenges, innvilger 1. januar til 31. mars 2025.
 * Tiltaksdeltakelsen varer til 30. juni 2025, så vedtaket kan forlenges frem til da.
 */
class ForlengelseRouteTest {
    private val vedtaksperiode = 1.januar(2025) til 31.mars(2025)

    private suspend fun ApplicationTestBuilder.iverksettVedtakSomKanForlenges(
        tac: TestApplicationContext,
        barnetillegg: Barnetillegg = Barnetillegg.utenBarnetillegg(nonEmptyListOf(vedtaksperiode)),
        barnFraSøknad: List<BarnetilleggFraSøknad> = emptyList(),
    ): Pair<Sak, Rammevedtak> {
        val deltakelse = tac.tiltaksdeltakelse(1.januar(2025) til 30.juni(2025))
        val (sak, _, vedtak) = iverksettSøknadsbehandling(
            tac = tac,
            tiltaksdeltakelse = deltakelse,
            innvilgelsesperioder = innvilgelsesperioder(vedtaksperiode, deltakelse),
            barnetillegg = barnetillegg,
            barnFraSøknad = barnFraSøknad,
        )
        return sak to vedtak
    }

    private suspend fun ApplicationTestBuilder.startOgOppdaterForlengelse(
        tac: TestApplicationContext,
        sak: Sak,
        vedtak: Rammevedtak,
        barnetillegg: Barnetillegg? = null,
    ): Rammebehandling {
        val (_, forlengelse) = startForlengelse(tac, sak.id, vedtak.id)!!
        return oppdaterForlengelse(
            tac = tac,
            sakId = sak.id,
            behandlingId = forlengelse.id,
            tilOgMed = 31.mai(2025),
            barnetillegg = barnetillegg,
            begrunnelseVilkårsvurdering = "Tiltaket er forlenget",
            fritekstTilVedtaksbrev = "Vedtaket ditt er forlenget",
        ).second
    }

    private suspend fun ApplicationTestBuilder.sendTilBeslutningOgTa(tac: TestApplicationContext, sakId: SakId, behandling: Rammebehandling) {
        sendRevurderingTilBeslutningForBehandlingId(tac, sakId, behandling.id)
        taRammebehandlinger(tac, behandlinger = listOf(sakId to behandling.id), saksbehandler = ObjectMother.beslutter())
    }

    private fun Rammebehandling.barnetilleggsperioder(): List<PeriodeMedVerdi<AntallBarn>> =
        barnetillegg.shouldNotBeNull().periodisering.perioderMedVerdi.toList()

    private fun feil(status: Int) = ForventetRespons(status, contentType = "application/json; charset=UTF-8")

    private fun String.skalHaFeilkode(kode: String) {
        JSONObject(this).getString("kode") shouldBe kode
    }

    @Test
    fun `forlenger vedtaket fra dagen etter det slutter til ny til og med-dato`() {
        withTestApplicationContextAndPostgres { tac ->
            val (sak, vedtak) = iverksettVedtakSomKanForlenges(tac)

            val (_, startet, startetJson) = startForlengelse(tac, sak.id, vedtak.id)!!
            startet.resultat shouldBe Revurderingsresultat.Forlengelse.opprett(ForlengerRammevedtak(vedtak.id, 1.april(2025)))
            startetJson.getString("resultat") shouldBe "FORLENGELSE"
            startetJson.getString("forlengerRammevedtakId") shouldBe vedtak.id.toString()
            startetJson.getString("forlengesFraOgMed") shouldBe "2025-04-01"

            val (_, oppdatert, oppdatertJson) = oppdaterForlengelse(tac, sak.id, startet.id, tilOgMed = 31.mai(2025))
            JSONObject(oppdatertJson).getBoolean("harValgtBarnetillegg") shouldBe false
            oppdatert.innvilgelsesperioder.shouldNotBeNull().totalPeriode shouldBe (1.april(2025) til 31.mai(2025))
            oppdatert.barnetilleggsperioder() shouldBe listOf(PeriodeMedVerdi(AntallBarn.ZERO, 1.april(2025) til 31.mai(2025)))

            forhåndsvisVedtaksbrevForBehandlingId(
                tac = tac,
                sakId = sak.id,
                behandlingId = startet.id,
                resultat = RammebehandlingResultatTypeDTO.FORLENGELSE,
                tilOgMed = 31.mai(2025),
            ).third shouldBe "pdf"

            sendTilBeslutningOgTa(tac, sak.id, oppdatert)
            val (sakEtter, forlengelsesvedtak) = iverksettForBehandlingId(tac, sak.id, startet.id)!!

            forlengelsesvedtak.periode shouldBe (1.april(2025) til 31.mai(2025))
            sakEtter.rammevedtaksliste.innvilgetTidslinje.totalPeriode shouldBe (1.januar(2025) til 31.mai(2025))
            sakEtter.rammevedtaksliste.gyldigForlengelseskommando(sakEtter.hentRammevedtakForId(vedtak.id)).shouldBeNull()
            sakEtter.rammevedtaksliste.gyldigForlengelseskommando(forlengelsesvedtak).shouldNotBeNull().forlengesFraOgMed shouldBe 1.juni(2025)

            startForlengelse(tac, sak.id, vedtak.id, forventet = feil(400)).shouldBeNull()
        }
    }

    @Test
    fun `barnetillegget videreføres fra vedtaket og faller bort for barn som fyller 16 år`() {
        withTestApplicationContextAndPostgres { tac ->
            val (sak, vedtak) = iverksettVedtakSomKanForlenges(
                tac = tac,
                barnetillegg = barnetillegg(periode = vedtaksperiode, antallBarn = AntallBarn(2)),
                barnFraSøknad = listOf(barnetilleggMedIdent(fødselsdato = 1.januar(2015)), barnetilleggMedIdent(fødselsdato = 1.mai(2009))),
            )

            startOgOppdaterForlengelse(tac, sak, vedtak).barnetilleggsperioder() shouldBe listOf(
                PeriodeMedVerdi(AntallBarn(2), 1.april(2025) til 30.april(2025)),
                PeriodeMedVerdi(AntallBarn(1), 1.mai(2025) til 31.mai(2025)),
            )
        }
    }

    @Test
    fun `barnetillegget utledes på nytt hver gang forlengelsen lagres uten barnetillegg valgt av saksbehandler`() {
        withTestApplicationContextAndPostgres { tac ->
            val (sak, vedtak) = iverksettVedtakSomKanForlenges(
                tac = tac,
                barnetillegg = barnetillegg(periode = vedtaksperiode, antallBarn = AntallBarn(2)),
                barnFraSøknad = listOf(barnetilleggMedIdent(fødselsdato = 1.januar(2015)), barnetilleggMedIdent(fødselsdato = 1.mai(2009))),
            )
            val valgt = startOgOppdaterForlengelse(
                tac = tac,
                sak = sak,
                vedtak = vedtak,
                barnetillegg = barnetillegg(periode = 1.april(2025) til 31.mai(2025), antallBarn = AntallBarn(3)),
            )

            val (_, oppdatert, oppdatertJson) = oppdaterForlengelse(tac, sak.id, valgt.id, tilOgMed = 30.juni(2025))

            JSONObject(oppdatertJson).getBoolean("harValgtBarnetillegg") shouldBe false
            oppdatert.barnetilleggsperioder() shouldBe listOf(
                PeriodeMedVerdi(AntallBarn(2), 1.april(2025) til 30.april(2025)),
                PeriodeMedVerdi(AntallBarn(1), 1.mai(2025) til 30.juni(2025)),
            )
        }
    }

    @Test
    fun `barnetillegget i vedtaket videreføres uendret når barna ikke kan utledes fra søknaden`() {
        withTestApplicationContextAndPostgres { tac ->
            val (sak, vedtak) = iverksettVedtakSomKanForlenges(
                tac = tac,
                barnetillegg = barnetillegg(periode = vedtaksperiode, antallBarn = AntallBarn(1)),
            )

            startOgOppdaterForlengelse(tac, sak, vedtak).barnetilleggsperioder() shouldBe listOf(
                PeriodeMedVerdi(AntallBarn(1), 1.april(2025) til 31.mai(2025)),
            )
        }
    }

    @Test
    fun `barnetillegget saksbehandler velger, fylles ut med 0 barn i resten av forlengelsen`() {
        withTestApplicationContextAndPostgres { tac ->
            val (sak, vedtak) = iverksettVedtakSomKanForlenges(tac)

            val forlengelse = startOgOppdaterForlengelse(
                tac = tac,
                sak = sak,
                vedtak = vedtak,
                barnetillegg = barnetillegg(periode = 1.april(2025) til 30.april(2025), antallBarn = AntallBarn(1)),
            )
            forlengelse.barnetilleggsperioder() shouldBe listOf(
                PeriodeMedVerdi(AntallBarn(1), 1.april(2025) til 30.april(2025)),
                PeriodeMedVerdi(AntallBarn.ZERO, 1.mai(2025) til 31.mai(2025)),
            )
            tac.behandlingContext.rammebehandlingRepo.hent(forlengelse.id).resultat
                .shouldBeInstanceOf<Revurderingsresultat.Forlengelse>().harValgtBarnetillegg shouldBe true
        }
    }

    @Test
    fun `ugyldig til og med-dato eller barnetillegg avvises`() {
        withTestApplicationContextAndPostgres { tac ->
            val (sak, vedtak) = iverksettVedtakSomKanForlenges(tac)
            val (_, forlengelse) = startForlengelse(tac, sak.id, vedtak.id)!!

            suspend fun oppdater(tilOgMed: LocalDate, barnetillegg: Barnetillegg? = null) =
                oppdaterForlengelse(tac, sak.id, forlengelse.id, tilOgMed = tilOgMed, barnetillegg = barnetillegg, forventet = feil(400)).third

            oppdater(31.mars(2025)).skalHaFeilkode("forlengelse_til_og_med_må_være_etter_vedtaket")
            oppdater(1.juli(2025)).skalHaFeilkode("tiltaksdeltakelsen_dekker_ikke_forlengelsen")
            oppdater(30.april(2025), barnetillegg(periode = 1.april(2025) til 31.mai(2025)))
                .skalHaFeilkode("barnetillegget_går_utover_forlengelsen")
        }
    }

    @Test
    fun `forlengelser av et vedtak som allerede er forlenget, kan ikke oppdateres, sendes til beslutning eller iverksettes`() {
        withTestApplicationContextAndPostgres { tac ->
            val (sak, vedtak) = iverksettVedtakSomKanForlenges(tac)
            val førsteForlengelse = startOgOppdaterForlengelse(tac, sak, vedtak)
            val andreForlengelse = startOgOppdaterForlengelse(tac, sak, vedtak)
            val tredjeForlengelse = startOgOppdaterForlengelse(tac, sak, vedtak)
            sendTilBeslutningOgTa(tac, sak.id, førsteForlengelse)
            sendTilBeslutningOgTa(tac, sak.id, andreForlengelse)

            iverksettForBehandlingId(tac, sak.id, førsteForlengelse.id).shouldNotBeNull()

            iverksettForBehandlingId(
                tac = tac,
                sakId = sak.id,
                behandlingId = andreForlengelse.id,
                forventet = feil(409),
                medJsonBody = { it.skalHaFeilkode("forlengelsesgrunnlaget_er_endret") },
            ).shouldBeNull()
            sendRevurderingTilBeslutningForBehandlingId(tac, sak.id, tredjeForlengelse.id, forventet = feil(409))
                .skalHaFeilkode("forlengelsesgrunnlaget_er_endret")
            oppdaterForlengelse(tac, sak.id, tredjeForlengelse.id, tilOgMed = 31.mai(2025), forventet = feil(409))
                .third.skalHaFeilkode("vedtaket_kan_ikke_lenger_forlenges")
        }
    }
}
