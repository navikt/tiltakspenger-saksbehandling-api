package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.iverksett

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.tiltakspenger.libs.dato.februar
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.dato.mars
import no.nav.tiltakspenger.libs.ktor.test.common.ForventetRespons
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.libs.periode.til
import no.nav.tiltakspenger.libs.periodisering.TomPeriodisering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.AntallDagerForMeldeperiode
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContext
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContext
import no.nav.tiltakspenger.saksbehandling.infra.route.harKode
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.innvilgelsesperioder
import no.nav.tiltakspenger.saksbehandling.omgjøring.OmgjortAvRammevedtak
import no.nav.tiltakspenger.saksbehandling.omgjøring.OmgjørRammevedtak
import no.nav.tiltakspenger.saksbehandling.omgjøring.Omgjøringsgrad
import no.nav.tiltakspenger.saksbehandling.omgjøring.Omgjøringsgrad.DELVIS
import no.nav.tiltakspenger.saksbehandling.omgjøring.Omgjøringsgrad.HELT
import no.nav.tiltakspenger.saksbehandling.omgjøring.Omgjøringsperiode
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettOmgjøringInnvilgelse
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettOmgjøringOpphør
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettRevurderingInnvilgelse
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettRevurderingStans
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.oppdaterOmgjøringOpphør
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.startRevurderingOmgjøring
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.vedtak.Rammevedtak
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.MethodSource

/**
 * Omgjøringer som går over flere vedtak, der minst ett av dem er en stans eller et opphør.
 * Hver sak har tre gjeldende vedtak, ett for hver av januar, februar og mars 2025.
 * Ett av dem er en stans eller et opphør, og det ligger i starten, i midten eller på slutten av perioden.
 */
class IverksettOmgjøringOverFlereVedtakTest {

    private val januar = 1.januar(2025) til 31.januar(2025)
    private val februar = 1.februar(2025) til 28.februar(2025)
    private val mars = 1.mars(2025) til 31.mars(2025)
    private val førsteKvartal = 1.januar(2025) til 31.mars(2025)

    enum class IkkeInnvilget { STANS, OPPHØR }

    enum class Plassering { START, MIDTEN, SLUTT }

    @ParameterizedTest(name = "{0} i {1}")
    @MethodSource("alleKombinasjoner")
    fun `kan omgjøre til innvilgelse over flere vedtak med stans eller opphør`(
        ikkeInnvilget: IkkeInnvilget,
        plassering: Plassering,
    ) {
        withTestApplicationContext { tac ->
            val (sak, vedtakene) = opprettSakMedTreVedtak(tac, ikkeInnvilget, plassering)

            val (sakMedOmgjøring, omgjøringsvedtak) = iverksettOmgjøringInnvilgelse(
                tac = tac,
                sakId = sak.id,
                rammevedtakIdSomOmgjøres = vedtakene.innvilgelse().id,
                vedtaksperiode = førsteKvartal,
                innvilgelsesperioder = innvilgelsesperioder(førsteKvartal),
            )

            omgjøringsvedtak.omgjørRammevedtak shouldBe OmgjørRammevedtak(
                vedtakene.map { Omgjøringsperiode(it.vedtak.id, it.periode, it.gradVedFullOmgjøring) },
            )
            sakMedOmgjøring.rammevedtaksliste.innvilgetTidslinje.perioder shouldBe listOf(førsteKvartal)
            vedtakene.forEach {
                sakMedOmgjøring.hentRammevedtakForId(it.vedtak.id).omgjortAvRammevedtak
                    .single { omgjortAv -> omgjortAv.rammevedtakId == omgjøringsvedtak.id } shouldBe
                    Omgjøringsperiode(omgjøringsvedtak.id, it.periode, it.gradVedFullOmgjøring)
            }
        }
    }

    @ParameterizedTest
    @EnumSource(IkkeInnvilget::class)
    fun `kan opphøre over en stans eller et opphør i midten`(ikkeInnvilget: IkkeInnvilget) {
        withTestApplicationContext { tac ->
            val (sak, vedtakene) = opprettSakMedTreVedtak(tac, ikkeInnvilget, Plassering.MIDTEN)
            val (januarvedtak, februarvedtak, marsvedtak) = vedtakene.map { it.vedtak }

            val (sakMedOpphør, opphørsvedtak) = iverksettOmgjøringOpphør(
                tac = tac,
                sakId = sak.id,
                rammevedtakIdSomOmgjøres = januarvedtak.id,
                vedtaksperiode = 15.januar(2025) til 15.mars(2025),
            )

            opphørsvedtak.omgjørRammevedtak shouldBe OmgjørRammevedtak(
                Omgjøringsperiode(januarvedtak.id, 15.januar(2025) til 31.januar(2025), DELVIS),
                Omgjøringsperiode(februarvedtak.id, februar, vedtakene[1].gradVedFullOmgjøring),
                Omgjøringsperiode(marsvedtak.id, 1.mars(2025) til 15.mars(2025), DELVIS),
            )
            sakMedOpphør.rammevedtaksliste.innvilgetTidslinje.perioder shouldBe listOf(
                1.januar(2025) til 14.januar(2025),
                16.mars(2025) til 31.mars(2025),
            )
        }
    }

    @ParameterizedTest
    @EnumSource(IkkeInnvilget::class)
    fun `kan ikke opphøre fra en stans eller et opphør i starten, men kan opphøre innvilgelsene etter`(
        ikkeInnvilget: IkkeInnvilget,
    ) {
        withTestApplicationContext { tac ->
            val (sak, vedtakene) = opprettSakMedTreVedtak(tac, ikkeInnvilget, Plassering.START)
            val (_, februarvedtak, marsvedtak) = vedtakene.map { it.vedtak }
            val (_, omgjøring) = startRevurderingOmgjøring(tac, sak.id, februarvedtak.id)!!

            oppdaterOmgjøringOpphør(
                tac = tac,
                sakId = sak.id,
                behandlingId = omgjøring.id,
                vedtaksperiode = førsteKvartal,
                forventet = ForventetRespons(400, contentType = "application/json; charset=UTF-8"),
            ).also { (_, _, response) ->
                response harKode "ugyldig_periode_for_opphør"
                response shouldContain "Perioden som opphøres må starte i en gjeldende innvilgelsesperiode"
            }

            val (_, oppdatertOmgjøring) = oppdaterOmgjøringOpphør(
                tac = tac,
                sakId = sak.id,
                behandlingId = omgjøring.id,
                vedtaksperiode = 1.februar(2025) til 31.mars(2025),
            )
            oppdatertOmgjøring.resultat!!.omgjørRammevedtak.rammevedtakIDer shouldBe listOf(februarvedtak.id, marsvedtak.id)
        }
    }

    @ParameterizedTest
    @EnumSource(IkkeInnvilget::class)
    fun `kan ikke opphøre til en stans eller et opphør på slutten, men kan opphøre innvilgelsene før`(
        ikkeInnvilget: IkkeInnvilget,
    ) {
        withTestApplicationContext { tac ->
            val (sak, vedtakene) = opprettSakMedTreVedtak(tac, ikkeInnvilget, Plassering.SLUTT)
            val (januarvedtak, februarvedtak) = vedtakene.map { it.vedtak }
            val (_, omgjøring) = startRevurderingOmgjøring(tac, sak.id, januarvedtak.id)!!

            oppdaterOmgjøringOpphør(
                tac = tac,
                sakId = sak.id,
                behandlingId = omgjøring.id,
                vedtaksperiode = førsteKvartal,
                forventet = ForventetRespons(400, contentType = "application/json; charset=UTF-8"),
            ).also { (_, _, response) ->
                response harKode "ugyldig_periode_for_opphør"
                response shouldContain "Perioden som opphøres må slutte i en gjeldende innvilgelsesperiode"
            }

            val (_, oppdatertOmgjøring) = oppdaterOmgjøringOpphør(
                tac = tac,
                sakId = sak.id,
                behandlingId = omgjøring.id,
                vedtaksperiode = 1.januar(2025) til 28.februar(2025),
            )
            oppdatertOmgjøring.resultat!!.omgjørRammevedtak.rammevedtakIDer shouldBe listOf(januarvedtak.id, februarvedtak.id)
        }
    }

    @Test
    fun `kan omgjøre en tidligere omgjøring av flere vedtak i sin helhet, sammen med et annet vedtak`() {
        withTestApplicationContext { tac ->
            val (sak, søknadsvedtak, revurderingsvedtak, tidligereOmgjøring) = opprettSakMedOmgjøringAvFlereVedtak(tac)

            val (sakMedOmgjøring, omgjøringsvedtak) = iverksettOmgjøringInnvilgelse(
                tac = tac,
                sakId = sak.id,
                rammevedtakIdSomOmgjøres = tidligereOmgjøring.id,
                vedtaksperiode = førsteKvartal,
                innvilgelsesperioder = innvilgelsesperioder(førsteKvartal),
            )

            omgjøringsvedtak.omgjørRammevedtak shouldBe OmgjørRammevedtak(
                Omgjøringsperiode(tidligereOmgjøring.id, 1.januar(2025) til 28.februar(2025), HELT),
                Omgjøringsperiode(søknadsvedtak.id, mars, DELVIS),
            )
            sakMedOmgjøring.hentRammevedtakForId(tidligereOmgjøring.id).omgjortAvRammevedtak shouldBe OmgjortAvRammevedtak(
                Omgjøringsperiode(omgjøringsvedtak.id, 1.januar(2025) til 28.februar(2025), HELT),
            )
            sakMedOmgjøring.hentRammevedtakForId(søknadsvedtak.id).omgjortAvRammevedtak shouldBe OmgjortAvRammevedtak(
                Omgjøringsperiode(tidligereOmgjøring.id, januar, DELVIS),
                Omgjøringsperiode(revurderingsvedtak.id, februar, DELVIS),
                Omgjøringsperiode(omgjøringsvedtak.id, mars, DELVIS),
            )
            sakMedOmgjøring.hentRammevedtakForId(revurderingsvedtak.id).omgjortAvRammevedtak shouldBe OmgjortAvRammevedtak(
                Omgjøringsperiode(tidligereOmgjøring.id, februar, HELT),
            )
            sakMedOmgjøring.rammevedtaksliste.innvilgetTidslinje.perioder shouldBe listOf(førsteKvartal)
        }
    }

    @Test
    fun `kan delvis omgjøre en tidligere omgjøring av flere vedtak, sammen med et annet vedtak`() {
        withTestApplicationContext { tac ->
            val (sak, søknadsvedtak, _, tidligereOmgjøring) = opprettSakMedOmgjøringAvFlereVedtak(tac)
            val vedtaksperiode = 15.januar(2025) til 15.mars(2025)

            val (sakMedOmgjøring, omgjøringsvedtak) = iverksettOmgjøringInnvilgelse(
                tac = tac,
                sakId = sak.id,
                rammevedtakIdSomOmgjøres = tidligereOmgjøring.id,
                vedtaksperiode = vedtaksperiode,
                innvilgelsesperioder = innvilgelsesperioder(vedtaksperiode),
            )

            omgjøringsvedtak.omgjørRammevedtak shouldBe OmgjørRammevedtak(
                Omgjøringsperiode(tidligereOmgjøring.id, 15.januar(2025) til 28.februar(2025), DELVIS),
                Omgjøringsperiode(søknadsvedtak.id, 1.mars(2025) til 15.mars(2025), DELVIS),
            )
            sakMedOmgjøring.hentRammevedtakForId(tidligereOmgjøring.id).omgjortAvRammevedtak shouldBe OmgjortAvRammevedtak(
                Omgjøringsperiode(omgjøringsvedtak.id, 15.januar(2025) til 28.februar(2025), DELVIS),
            )
            sakMedOmgjøring.rammevedtaksliste.tidslinje.perioderMedVerdi.map { it.verdi.id to it.periode } shouldBe listOf(
                tidligereOmgjøring.id to (1.januar(2025) til 14.januar(2025)),
                omgjøringsvedtak.id to vedtaksperiode,
                søknadsvedtak.id to (16.mars(2025) til 31.mars(2025)),
            )
        }
    }

    @Test
    fun `kan opphøre en tidligere omgjøring av flere vedtak i sin helhet, sammen med et annet vedtak`() {
        withTestApplicationContext { tac ->
            val (sak, søknadsvedtak, _, tidligereOmgjøring) = opprettSakMedOmgjøringAvFlereVedtak(tac)

            val (sakMedOpphør, opphørsvedtak) = iverksettOmgjøringOpphør(
                tac = tac,
                sakId = sak.id,
                rammevedtakIdSomOmgjøres = tidligereOmgjøring.id,
                vedtaksperiode = førsteKvartal,
            )

            opphørsvedtak.omgjørRammevedtak shouldBe OmgjørRammevedtak(
                Omgjøringsperiode(tidligereOmgjøring.id, 1.januar(2025) til 28.februar(2025), HELT),
                Omgjøringsperiode(søknadsvedtak.id, mars, DELVIS),
            )
            sakMedOpphør.rammevedtaksliste.innvilgetTidslinje shouldBe TomPeriodisering.instance()
        }
    }

    @Test
    fun `kan delvis opphøre en tidligere omgjøring av flere vedtak, sammen med et annet vedtak`() {
        withTestApplicationContext { tac ->
            val (sak, søknadsvedtak, _, tidligereOmgjøring) = opprettSakMedOmgjøringAvFlereVedtak(tac)

            val (sakMedOpphør, opphørsvedtak) = iverksettOmgjøringOpphør(
                tac = tac,
                sakId = sak.id,
                rammevedtakIdSomOmgjøres = tidligereOmgjøring.id,
                vedtaksperiode = 15.januar(2025) til 15.mars(2025),
            )

            opphørsvedtak.omgjørRammevedtak shouldBe OmgjørRammevedtak(
                Omgjøringsperiode(tidligereOmgjøring.id, 15.januar(2025) til 28.februar(2025), DELVIS),
                Omgjøringsperiode(søknadsvedtak.id, 1.mars(2025) til 15.mars(2025), DELVIS),
            )
            sakMedOpphør.rammevedtaksliste.innvilgetTidslinje.perioder shouldBe listOf(
                1.januar(2025) til 14.januar(2025),
                16.mars(2025) til 31.mars(2025),
            )
        }
    }

    @Test
    fun `kan omgjøre et tidligere opphør over flere vedtak i sin helhet, sammen med vedtakene på hver side`() {
        withTestApplicationContext { tac ->
            val (sak, vedtakene) = opprettSakMedTreVedtak(tac, IkkeInnvilget.STANS, Plassering.MIDTEN)
            val (januarvedtak, _, marsvedtak) = vedtakene.map { it.vedtak }
            val opphørsperiode = 15.januar(2025) til 15.mars(2025)

            val (_, tidligereOpphør) = iverksettOmgjøringOpphør(
                tac = tac,
                sakId = sak.id,
                rammevedtakIdSomOmgjøres = januarvedtak.id,
                vedtaksperiode = opphørsperiode,
            )

            val (sakMedOmgjøring, omgjøringsvedtak) = iverksettOmgjøringInnvilgelse(
                tac = tac,
                sakId = sak.id,
                rammevedtakIdSomOmgjøres = tidligereOpphør.id,
                vedtaksperiode = førsteKvartal,
                innvilgelsesperioder = innvilgelsesperioder(førsteKvartal),
            )

            omgjøringsvedtak.omgjørRammevedtak shouldBe OmgjørRammevedtak(
                Omgjøringsperiode(januarvedtak.id, 1.januar(2025) til 14.januar(2025), DELVIS),
                Omgjøringsperiode(tidligereOpphør.id, opphørsperiode, HELT),
                Omgjøringsperiode(marsvedtak.id, 16.mars(2025) til 31.mars(2025), DELVIS),
            )
            sakMedOmgjøring.hentRammevedtakForId(tidligereOpphør.id).omgjortAvRammevedtak shouldBe OmgjortAvRammevedtak(
                Omgjøringsperiode(omgjøringsvedtak.id, opphørsperiode, HELT),
            )
            sakMedOmgjøring.rammevedtaksliste.innvilgetTidslinje.perioder shouldBe listOf(førsteKvartal)
        }
    }

    /**
     * @param gradVedFullOmgjøring Graden vedtaket omgjøres med når hele perioden omgjøres.
     * Den er [DELVIS] når vedtaket opprinnelig gjaldt for mer enn denne måneden.
     */
    private data class GjeldendeVedtak(
        val vedtak: Rammevedtak,
        val periode: Periode,
        val gradVedFullOmgjøring: Omgjøringsgrad,
    )

    private fun List<GjeldendeVedtak>.innvilgelse(): Rammevedtak = first { !it.vedtak.erStans && !it.vedtak.erOpphør }.vedtak

    /**
     * Søknadsvedtaket innvilger hele første kvartal, og de senere vedtakene gir ett gjeldende vedtak per måned.
     * Innvilgelsene i to ulike vedtak har ulikt antall dager, slik at de faktisk endrer noe.
     */
    private suspend fun ApplicationTestBuilder.opprettSakMedTreVedtak(
        tac: TestApplicationContext,
        ikkeInnvilget: IkkeInnvilget,
        plassering: Plassering,
    ): Pair<Sak, List<GjeldendeVedtak>> {
        val (sak, _, søknadsvedtak) = iverksettSøknadsbehandling(
            tac = tac,
            innvilgelsesperioder = innvilgelsesperioder(førsteKvartal),
        )

        suspend fun revurderInnvilgelse(periode: Periode, antallDager: Int = 2): Pair<Sak, Rammevedtak> =
            iverksettRevurderingInnvilgelse(
                tac = tac,
                sakId = sak.id,
                innvilgelsesperioder = innvilgelsesperioder(
                    periode,
                    antallDagerPerMeldeperiode = AntallDagerForMeldeperiode(antallDager),
                ),
            ).let { it.first to it.second }

        suspend fun stans(fraOgMed: Periode): Pair<Sak, Rammevedtak> =
            iverksettRevurderingStans(tac = tac, sakId = sak.id, stansFraOgMed = fraOgMed.fraOgMed)
                .let { it.first to it.second }

        suspend fun opphør(periode: Periode): Pair<Sak, Rammevedtak> =
            iverksettOmgjøringOpphør(
                tac = tac,
                sakId = sak.id,
                rammevedtakIdSomOmgjøres = søknadsvedtak.id,
                vedtaksperiode = periode,
            ).let { it.first to it.second }

        return when (ikkeInnvilget to plassering) {
            IkkeInnvilget.STANS to Plassering.START -> {
                val (_, stansvedtak) = stans(januar)
                val (_, februarvedtak) = revurderInnvilgelse(1.februar(2025) til 31.mars(2025), antallDager = 10)
                val (sist, marsvedtak) = revurderInnvilgelse(mars)
                sist to listOf(
                    GjeldendeVedtak(stansvedtak, januar, DELVIS),
                    GjeldendeVedtak(februarvedtak, februar, DELVIS),
                    GjeldendeVedtak(marsvedtak, mars, HELT),
                )
            }

            IkkeInnvilget.STANS to Plassering.MIDTEN -> {
                val (_, stansvedtak) = stans(februar)
                val (sist, marsvedtak) = revurderInnvilgelse(mars)
                sist to listOf(
                    GjeldendeVedtak(søknadsvedtak, januar, DELVIS),
                    GjeldendeVedtak(stansvedtak, februar, DELVIS),
                    GjeldendeVedtak(marsvedtak, mars, HELT),
                )
            }

            IkkeInnvilget.STANS to Plassering.SLUTT -> {
                val (_, februarvedtak) = revurderInnvilgelse(februar)
                val (sist, stansvedtak) = stans(mars)
                sist to listOf(
                    GjeldendeVedtak(søknadsvedtak, januar, DELVIS),
                    GjeldendeVedtak(februarvedtak, februar, HELT),
                    GjeldendeVedtak(stansvedtak, mars, HELT),
                )
            }

            IkkeInnvilget.OPPHØR to Plassering.START -> {
                val (_, marsvedtak) = revurderInnvilgelse(mars)
                val (sist, opphørsvedtak) = opphør(januar)
                sist to listOf(
                    GjeldendeVedtak(opphørsvedtak, januar, HELT),
                    GjeldendeVedtak(søknadsvedtak, februar, DELVIS),
                    GjeldendeVedtak(marsvedtak, mars, HELT),
                )
            }

            IkkeInnvilget.OPPHØR to Plassering.MIDTEN -> {
                val (_, marsvedtak) = revurderInnvilgelse(mars)
                val (sist, opphørsvedtak) = opphør(februar)
                sist to listOf(
                    GjeldendeVedtak(søknadsvedtak, januar, DELVIS),
                    GjeldendeVedtak(opphørsvedtak, februar, HELT),
                    GjeldendeVedtak(marsvedtak, mars, HELT),
                )
            }

            IkkeInnvilget.OPPHØR to Plassering.SLUTT -> {
                val (_, februarvedtak) = revurderInnvilgelse(februar)
                val (sist, opphørsvedtak) = opphør(mars)
                sist to listOf(
                    GjeldendeVedtak(søknadsvedtak, januar, DELVIS),
                    GjeldendeVedtak(februarvedtak, februar, HELT),
                    GjeldendeVedtak(opphørsvedtak, mars, HELT),
                )
            }

            else -> error("Ukjent kombinasjon: $ikkeInnvilget i $plassering")
        }
    }

    private data class SakMedOmgjøringAvFlereVedtak(
        val sak: Sak,
        val søknadsvedtak: Rammevedtak,
        val revurderingsvedtak: Rammevedtak,
        val tidligereOmgjøring: Rammevedtak,
    )

    /**
     * Den tidligere omgjøringen omgjør januar av søknadsvedtaket og hele revurderingsvedtaket i februar.
     * Søknadsvedtaket gjelder fortsatt for mars.
     */
    private suspend fun ApplicationTestBuilder.opprettSakMedOmgjøringAvFlereVedtak(
        tac: TestApplicationContext,
    ): SakMedOmgjøringAvFlereVedtak {
        val (sak, _, søknadsvedtak) = iverksettSøknadsbehandling(
            tac = tac,
            innvilgelsesperioder = innvilgelsesperioder(førsteKvartal),
        )
        val (_, revurderingsvedtak) = iverksettRevurderingInnvilgelse(
            tac = tac,
            sakId = sak.id,
            innvilgelsesperioder = innvilgelsesperioder(februar, antallDagerPerMeldeperiode = AntallDagerForMeldeperiode(2)),
        )
        val tidligereOmgjøringsperiode = 1.januar(2025) til 28.februar(2025)
        val (sakMedOmgjøring, tidligereOmgjøring) = iverksettOmgjøringInnvilgelse(
            tac = tac,
            sakId = sak.id,
            rammevedtakIdSomOmgjøres = søknadsvedtak.id,
            vedtaksperiode = tidligereOmgjøringsperiode,
            innvilgelsesperioder = innvilgelsesperioder(tidligereOmgjøringsperiode),
        )
        tidligereOmgjøring.omgjørRammevedtak shouldBe OmgjørRammevedtak(
            Omgjøringsperiode(søknadsvedtak.id, januar, DELVIS),
            Omgjøringsperiode(revurderingsvedtak.id, februar, HELT),
        )
        return SakMedOmgjøringAvFlereVedtak(sakMedOmgjøring, søknadsvedtak, revurderingsvedtak, tidligereOmgjøring)
    }

    companion object {
        @JvmStatic
        fun alleKombinasjoner(): List<Arguments> = IkkeInnvilget.entries.flatMap { ikkeInnvilget ->
            Plassering.entries.map { Arguments.of(ikkeInnvilget, it) }
        }
    }
}
