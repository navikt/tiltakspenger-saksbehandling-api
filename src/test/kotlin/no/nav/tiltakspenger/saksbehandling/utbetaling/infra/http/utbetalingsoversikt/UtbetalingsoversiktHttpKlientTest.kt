package no.nav.tiltakspenger.saksbehandling.utbetaling.infra.http.utbetalingsoversikt

import io.kotest.assertions.json.shouldEqualJson
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotContain
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.libs.common.AccessToken
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.getOrFail
import no.nav.tiltakspenger.libs.common.personopplysning.Fnr
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.dato.august
import no.nav.tiltakspenger.libs.dato.desember
import no.nav.tiltakspenger.libs.dato.februar
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.dato.mars
import no.nav.tiltakspenger.libs.dato.oktober
import no.nav.tiltakspenger.libs.dato.september
import no.nav.tiltakspenger.libs.httpklient.HttpKlientError
import no.nav.tiltakspenger.libs.httpklient.infra.kall.AuthTokenProvider
import no.nav.tiltakspenger.libs.httpklient.infra.transport.FakeHttpTransport
import no.nav.tiltakspenger.libs.json.deserialize
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.utbetalingsoversikt.Aktør
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.utbetalingsoversikt.Avgrensningsårsak
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.utbetalingsoversikt.KunneIkkeHenteUtbetalingsoversikt
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.utbetalingsoversikt.Oppslagsperiodetype
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.utbetalingsoversikt.RegistrertUtbetaling
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.utbetalingsoversikt.RegistrertYtelse
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.utbetalingsoversikt.Skattetrekk
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.utbetalingsoversikt.Trekk
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.utbetalingsoversikt.UtbetalingsoversiktMappingfeil
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.utbetalingsoversikt.Utbetalingsoversiktavgrensning
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.utbetalingsoversikt.Utbetalingsoversiktgrunnlag
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.utbetalingsoversikt.Ytelseskomponent
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.utbetalingsoversikt.sha256
import org.junit.jupiter.api.Test
import java.io.IOException
import java.time.Instant

/** Klienten kjøres over [FakeHttpTransport], så auth, statusregel, Jackson og metadata er med i testen. */
class UtbetalingsoversiktHttpKlientTest {
    private val baseUrl = "http://utbetaldata.test"
    private val correlationId = CorrelationId.generate()
    private val fnr = Fnr.random()
    private val periode = Periode(fraOgMed = 1.august(2025), tilOgMed = 30.september(2025))
    private val grunnlag = Utbetalingsoversiktgrunnlag(fnr, listOf(periode))

    private val authTokenProvider = object : AuthTokenProvider {
        override suspend fun hentToken(skipCache: Boolean) = AccessToken("token", Instant.MAX)
    }

    private fun klient(
        transport: FakeHttpTransport,
        authTokenProvider: AuthTokenProvider = this.authTokenProvider,
    ) = UtbetalingsoversiktHttpKlient(
        baseUrl = baseUrl,
        clock = ObjectMother.clock,
        authTokenProvider = authTokenProvider,
        transport = transport,
    )

    private fun transportMed(json: String) = FakeHttpTransport().apply { leggIKøJson(json, statusCode = 200) }

    private fun transportMedStatus(statusCode: Int) = FakeHttpTransport().apply {
        leggIKøStatus(statusCode = statusCode, body = "avvist", contentType = "text/plain")
    }

    private suspend fun hent(
        transport: FakeHttpTransport,
        periodetype: Oppslagsperiodetype = Oppslagsperiodetype.UTBETALINGSPERIODE,
        klient: UtbetalingsoversiktHttpKlient = klient(transport),
        grunnlag: Utbetalingsoversiktgrunnlag = this.grunnlag,
    ) = klient.hent(grunnlag = grunnlag, periode = periode, periodetype = periodetype, correlationId = correlationId)

    @Test
    fun `klienten kan bygges med standardtransporten`() {
        UtbetalingsoversiktHttpKlient(baseUrl = baseUrl, clock = ObjectMother.clock, authTokenProvider = authTokenProvider)
    }

    /**
     * Hele lista sammenlignes, så et felt som kommer til eller faller bort, må vurderes på nytt.
     * Den andre utbetalingen mister ytelsen «Økonomisk sosialhjelp» og ytelsen uten type, og dermed totalbeløpet.
     */
    @Test
    fun `fullt svar gir utbetalingene med alle felt, uten kontonummer, navn og ytelser utenfor grunnlaget`() {
        val transport = transportMed(UtbetalingsoversiktDtoTestEx.riktSvar(fnr))

        runTest {
            hent(transport).getOrFail().utbetalinger shouldBe listOf(
                RegistrertUtbetaling(
                    utbetaltTil = Aktør.Person(fnr),
                    utbetalingsmetode = "Til konto",
                    utbetalingsstatus = "Utbetalt",
                    posteringsdato = 24.august(2025),
                    forfallsdato = 24.august(2025),
                    utbetalingsdato = 15.august(2025),
                    nettobeløp = "900.1".toBigDecimal(),
                    melding = "En eller annen melding",
                    ytelser = listOf(
                        RegistrertYtelse(
                            ytelsestype = "Tiltakspenger",
                            periode = Periode(1.august(2025), 14.august(2025)),
                            nettobeløp = "900.1".toBigDecimal(),
                            rettighetshaver = Aktør.Person(fnr),
                            skattesum = "-99.9".toBigDecimal(),
                            trekksum = "-900.75".toBigDecimal(),
                            komponentsum = "1900.75".toBigDecimal(),
                            komponenter = listOf(
                                Ytelseskomponent(
                                    type = "Tiltakspenger",
                                    satsbeløp = "500.25".toBigDecimal(),
                                    satstype = "Dag",
                                    satsantall = 3.0,
                                    beløp = "1500.75".toBigDecimal(),
                                ),
                                Ytelseskomponent(
                                    type = "Barnetillegg",
                                    satsbeløp = null,
                                    satstype = null,
                                    satsantall = null,
                                    beløp = "400".toBigDecimal(),
                                ),
                            ),
                            trekk = listOf(
                                Trekk(type = "Skatt", beløp = "-300.25".toBigDecimal(), kreditor = "Skatteetaten"),
                                Trekk(type = "Annet trekk", beløp = "-600.50".toBigDecimal(), kreditor = "Namsmannen"),
                            ),
                            skattetrekk = listOf(Skattetrekk(beløp = "-99.9".toBigDecimal())),
                            bilagsnummer = "84172491",
                            refundertFor = null,
                        ),
                    ),
                ),
                RegistrertUtbetaling(
                    utbetaltTil = Aktør.Person(fnr),
                    utbetalingsmetode = "Til konto",
                    utbetalingsstatus = "something",
                    posteringsdato = 9.september(2025),
                    forfallsdato = 19.september(2025),
                    utbetalingsdato = null,
                    nettobeløp = null,
                    melding = null,
                    ytelser = listOf(
                        RegistrertYtelse(
                            ytelsestype = "Tiltakspenger",
                            periode = Periode(1.september(2025), 14.september(2025)),
                            nettobeløp = "2850".toBigDecimal(),
                            rettighetshaver = Aktør.Person(fnr),
                            skattesum = "0".toBigDecimal(),
                            trekksum = "0".toBigDecimal(),
                            komponentsum = "2850".toBigDecimal(),
                            komponenter = listOf(
                                Ytelseskomponent(
                                    type = "Tiltakspenger",
                                    satsbeløp = "285".toBigDecimal(),
                                    satstype = "Dag",
                                    satsantall = 10.0,
                                    beløp = "2850".toBigDecimal(),
                                ),
                            ),
                            trekk = emptyList(),
                            skattetrekk = emptyList(),
                            bilagsnummer = null,
                            refundertFor = null,
                        ),
                        RegistrertYtelse(
                            ytelsestype = "Dagpenger",
                            periode = Periode(1.september(2025), 30.september(2025)),
                            nettobeløp = "4200".toBigDecimal(),
                            rettighetshaver = Aktør.Person(fnr),
                            skattesum = "0".toBigDecimal(),
                            trekksum = "0".toBigDecimal(),
                            komponentsum = "4200".toBigDecimal(),
                            komponenter = listOf(
                                Ytelseskomponent(
                                    type = "Dagpenger",
                                    satsbeløp = null,
                                    satstype = null,
                                    satsantall = null,
                                    beløp = "4200".toBigDecimal(),
                                ),
                            ),
                            trekk = emptyList(),
                            skattetrekk = emptyList(),
                            bilagsnummer = null,
                            refundertFor = null,
                        ),
                    ),
                ),
            )
        }
    }

    @Test
    fun `avgrenset svar, avgrensning og metadata følger med ut`() {
        val svar = UtbetalingsoversiktDtoTestEx.svarUtenValgfrieFelt(fnr)

        runTest {
            val resultat = hent(transportMed(svar)).getOrFail()

            resultat.metadata.statusCode shouldBe 200
            resultat.metadata.rawRequestString.substringAfter("\n\n") shouldEqualJson forventetRequest("UTBETALINGSPERIODE")
            deserialize<List<UtbetalingsoversiktDto>>(resultat.avgrensetSvar) shouldBe deserialize<List<UtbetalingsoversiktDto>>(svar).utenNavn()
            resultat.avgrensning shouldBe Utbetalingsoversiktavgrensning(
                regelversjon = 1,
                perioder = listOf(periode),
                antallUtbetalingerMottatt = 1,
                antallYtelserMottatt = 1,
                fjernedeYtelserPerÅrsak = emptyMap(),
                antallFjernedeUtbetalinger = 0,
                mottattSvarSha256 = svar.sha256(),
                mottattSvarLengde = svar.length,
            )
        }
    }

    /**
     * Første utbetaling har én ytelse innenfor, én uten type, én av en type vi ikke samordner med, én utenfor periodene og én med annen rettighetshaver.
     * Andre utbetaling har bare én ytelse uten type.
     */
    @Test
    fun `ytelser utenfor grunnlaget telles per årsak og fjernes, og tomme utbetalinger tas bort`() {
        val annenPerson = Fnr.random()
        val svar = UtbetalingsoversiktDtoTestEx.svarMedYtelserUtenforGrunnlaget(fnr, annenPerson)

        runTest {
            val resultat = hent(transportMed(svar)).getOrFail()

            resultat.utbetalinger.map { it.ytelser.map { ytelse -> ytelse.ytelsestype } } shouldBe listOf(listOf("Dagpenger"))
            resultat.utbetalinger.single().nettobeløp shouldBe null
            resultat.avgrensning.antallUtbetalingerMottatt shouldBe 2
            resultat.avgrensning.antallYtelserMottatt shouldBe 6
            resultat.avgrensning.antallFjernedeUtbetalinger shouldBe 1
            resultat.avgrensning.fjernedeYtelserPerÅrsak shouldBe mapOf(
                Avgrensningsårsak.UTEN_YTELSESTYPE to 2,
                Avgrensningsårsak.ANNEN_YTELSESTYPE to 1,
                Avgrensningsårsak.UTENFOR_PERIODENE to 1,
                Avgrensningsårsak.ANNEN_RETTIGHETSHAVER to 1,
            )
            resultat.avgrensetSvar shouldEqualJson UtbetalingsoversiktDtoTestEx.avgrensetSvarMedYtelserUtenforGrunnlaget(fnr)
            resultat.avgrensetSvar shouldNotContain annenPerson.verdi
            resultat.avgrensetSvar shouldNotContain "navn\":\"F"
            resultat.avgrensetSvar shouldNotContain "kontonummer"
        }
    }

    /** Hele lista sammenlignes, så endringer i hvordan et ekte svar leses, blir synlige. */
    @Test
    fun `tre tidligere måneder i én utbetaling gir alle ytelsene med beløp, komponenter og satsfelt som er 0`() {
        val grunnlag = Utbetalingsoversiktgrunnlag(fnr, listOf(Periode(1.januar(2026), 31.oktober(2026))))
        val svar = UtbetalingsoversiktDtoTestEx.svarMedTreTidligereMånederIEnUtbetaling(fnr)

        fun ytelse(periode: Periode, nettobeløp: String, vararg komponenter: Pair<String, String>) = RegistrertYtelse(
            ytelsestype = "Tiltakspenger",
            periode = periode,
            nettobeløp = nettobeløp.toBigDecimal(),
            rettighetshaver = Aktør.Person(fnr),
            skattesum = "0.00".toBigDecimal(),
            trekksum = "0.00".toBigDecimal(),
            komponentsum = nettobeløp.toBigDecimal(),
            komponenter = komponenter.map { (type, beløp) ->
                Ytelseskomponent(type = type, satsbeløp = "0.00".toBigDecimal(), satstype = "Dag", satsantall = 0.0, beløp = beløp.toBigDecimal())
            },
            trekk = emptyList(),
            skattetrekk = emptyList(),
            bilagsnummer = UtbetalingsoversiktDtoTestEx.SYNTETISK_BILAGSNUMMER,
            refundertFor = null,
        )

        runTest {
            val resultat = hent(transportMed(svar), grunnlag = grunnlag).getOrFail()

            resultat.utbetalinger shouldBe listOf(
                RegistrertUtbetaling(
                    utbetaltTil = Aktør.Person(fnr),
                    utbetalingsmetode = "Norsk bankkonto",
                    utbetalingsstatus = "Utbetalt",
                    posteringsdato = 28.september(2026),
                    forfallsdato = 28.september(2026),
                    utbetalingsdato = 28.september(2026),
                    nettobeløp = "19928.00".toBigDecimal(),
                    melding = "010126 - 060326 Per dag  424,00",
                    ytelser = listOf(
                        ytelse(Periode(1.januar(2026), 30.januar(2026)), "9328.00", "Barnetillegg" to "2464.00", "Tiltakspenger" to "6864.00"),
                        ytelse(Periode(2.februar(2026), 27.februar(2026)), "8480.00", "Tiltakspenger" to "6240.00", "Barnetillegg" to "2240.00"),
                        ytelse(Periode(2.mars(2026), 6.mars(2026)), "2120.00", "Tiltakspenger" to "1560.00", "Barnetillegg" to "560.00"),
                    ),
                ),
            )
            resultat.avgrensning.antallYtelserMottatt shouldBe 3
            resultat.avgrensning.fjernedeYtelserPerÅrsak shouldBe emptyMap()
        }
    }

    @Test
    fun `en ytelse som begynner dagen før grunnlaget, beholdes med totalbeløpet`() {
        val grunnlag = Utbetalingsoversiktgrunnlag(fnr, listOf(Periode(6.desember(2025), 31.august(2026))))
        val svar = UtbetalingsoversiktDtoTestEx.svarMedYtelseSomBegynnerFørGrunnlaget(fnr)

        runTest {
            val utbetaling = hent(transportMed(svar), grunnlag = grunnlag).getOrFail().utbetalinger.single()

            utbetaling.ytelser.single().periode shouldBe Periode(5.desember(2025), 26.desember(2025))
            utbetaling.nettobeløp shouldBe "6854.00".toBigDecimal()
        }
    }

    @Test
    fun `totalbeløpet beholdes bare når alle ytelsene i utbetalingen beholdes`() {
        runTest {
            hent(transportMed(UtbetalingsoversiktDtoTestEx.riktSvar(fnr))).getOrFail().utbetalinger.map { it.nettobeløp } shouldBe
                listOf("900.1".toBigDecimal(), null)
        }
    }

    @Test
    fun `en ytelse med ugyldig periode slipper gjennom avgrensningen og stoppes i mappingen`() {
        val svar = UtbetalingsoversiktDtoTestEx.svarUtenValgfrieFelt(fnr).replace(""""tom": "2025-08-14"""", """"tom": "2025-07-31"""")

        runTest {
            hent(transportMed(svar)).leftOrNull().shouldNotBeNull().shouldBeInstanceOf<KunneIkkeHenteUtbetalingsoversikt.UgyldigInnhold>()
                .feil shouldBe UtbetalingsoversiktMappingfeil.UgyldigPeriode("ytelsesperiode")
        }
    }

    /** Kilden utelater tomme lister og felt uten verdi fra json-en. */
    @Test
    fun `utelatte lister blir tomme og negative summer bevares`() {
        runTest {
            val utbetaling = hent(transportMed(UtbetalingsoversiktDtoTestEx.svarUtenValgfrieFelt(fnr))).getOrFail().utbetalinger.single()

            utbetaling.nettobeløp shouldBe null
            utbetaling.forfallsdato shouldBe null
            utbetaling.utbetalingsdato shouldBe null
            utbetaling.melding shouldBe null

            val ytelse = utbetaling.ytelser.single()
            ytelse.komponenter shouldBe emptyList()
            ytelse.trekk shouldBe emptyList()
            ytelse.skattetrekk shouldBe emptyList()
            ytelse.bilagsnummer shouldBe null
            ytelse.refundertFor shouldBe null
            ytelse.skattesum shouldBe "-99.9".toBigDecimal()
            ytelse.trekksum shouldBe "-900.75".toBigDecimal()
        }
    }

    @Test
    fun `aktoerId leses som ident`() {
        runTest {
            hent(transportMed(UtbetalingsoversiktDtoTestEx.svarMedAktoerIdAlias(fnr))).getOrFail().utbetalinger.single().utbetaltTil shouldBe
                Aktør.Person(fnr)
        }
    }

    @Test
    fun `tomt array gir tom liste`() {
        runTest {
            hent(transportMed("[]")).getOrFail().utbetalinger shouldBe emptyList()
        }
    }

    @Test
    fun `svar som bryter kontrakten gir UgyldigInnhold med feilen og responsens metadata`() {
        val svar = UtbetalingsoversiktDtoTestEx.svarUtenValgfrieFelt(fnr).replace("\"PERSON\"", "\"ROBOT\"")

        runTest {
            val feil = hent(transportMed(svar)).leftOrNull().shouldNotBeNull()
                .shouldBeInstanceOf<KunneIkkeHenteUtbetalingsoversikt.UgyldigInnhold>()

            feil.feil shouldBe UtbetalingsoversiktMappingfeil.UkjentAktørtype("utbetaltTil")
            feil.metadata.statusCode shouldBe 200
            feil.metadata.rawResponseString.shouldNotBeNull() shouldEqualJson svar
        }
    }

    /** httpklient gjør ett forsøk til med ferskt token når tjenesten svarer 401. */
    @Test
    fun `401 gir TilgangAvvist etter ett nytt forsøk med ferskt token`() {
        val transport = FakeHttpTransport().apply {
            leggIKøStatusForAlleForsøk(statusCode = 401, body = "avvist", contentType = "text/plain", maksForsøk = 2)
        }

        runTest {
            hent(transport).leftOrNull().shouldNotBeNull()
                .shouldBeInstanceOf<KunneIkkeHenteUtbetalingsoversikt.TilgangAvvist>()
                .httpKlientError.statusCode shouldBe 401
            transport.mottatteKall.size shouldBe 2
        }
    }

    @Test
    fun `403 gir TilgangAvvist`() {
        val transport = transportMedStatus(403)

        runTest {
            hent(transport).leftOrNull().shouldNotBeNull()
                .shouldBeInstanceOf<KunneIkkeHenteUtbetalingsoversikt.TilgangAvvist>()
                .httpKlientError.statusCode shouldBe 403
            transport.mottatteKall.size shouldBe 1
        }
    }

    @Test
    fun `400 gir SakAvvist`() {
        runTest {
            hent(transportMedStatus(400)).leftOrNull().shouldNotBeNull()
                .shouldBeInstanceOf<KunneIkkeHenteUtbetalingsoversikt.SakAvvist>()
                .httpKlientError.statusCode shouldBe 400
        }
    }

    @Test
    fun `andre statuser gir Tjenestefeil`() {
        listOf(404, 429, 500, 503).forEach { status ->
            withClue(status) {
                runTest {
                    hent(transportMedStatus(status)).leftOrNull().shouldNotBeNull()
                        .shouldBeInstanceOf<KunneIkkeHenteUtbetalingsoversikt.Tjenestefeil>()
                        .httpKlientError.shouldBeInstanceOf<HttpKlientError.UventetStatus>()
                        .statusCode shouldBe status
                }
            }
        }
    }

    @Test
    fun `json som ikke lar seg lese gir UleseligSvar`() {
        runTest {
            hent(transportMed("""{"ikke": "en liste"}""")).leftOrNull().shouldNotBeNull()
                .shouldBeInstanceOf<KunneIkkeHenteUtbetalingsoversikt.UleseligSvar>()
        }
    }

    @Test
    fun `nettverksfeil gir Tjenestefeil`() {
        val transport = FakeHttpTransport().apply { leggIKøKast(IOException("forbindelsen ble brutt")) }

        runTest {
            hent(transport).leftOrNull().shouldNotBeNull()
                .shouldBeInstanceOf<KunneIkkeHenteUtbetalingsoversikt.Tjenestefeil>()
                .httpKlientError.shouldBeInstanceOf<HttpKlientError.IngenRespons>()
        }
    }

    @Test
    fun `feil ved henting av token gir Tjenestefeil uten at kallet sendes`() {
        val transport = FakeHttpTransport()
        val utenToken = object : AuthTokenProvider {
            override suspend fun hentToken(skipCache: Boolean): AccessToken = throw IllegalStateException("ingen token")
        }

        runTest {
            hent(transport, klient = klient(transport, utenToken)).leftOrNull().shouldNotBeNull()
                .shouldBeInstanceOf<KunneIkkeHenteUtbetalingsoversikt.Tjenestefeil>()
                .httpKlientError.shouldBeInstanceOf<HttpKlientError.RequestIkkeSendt>()
            transport.mottatteKall shouldBe emptyList()
        }
    }

    @Test
    fun `sender ident, rolle, periode og periodetype`() {
        listOf(
            Oppslagsperiodetype.UTBETALINGSPERIODE to "UTBETALINGSPERIODE",
            Oppslagsperiodetype.YTELSESPERIODE to "YTELSESPERIODE",
        ).forEach { (periodetype, forventetVerdi) ->
            withClue(periodetype.toString()) {
                val transport = transportMed("[]")

                runTest { hent(transport, periodetype) }

                val kall = transport.mottatteKall.single()
                kall.metode shouldBe "POST"
                kall.uri.toString() shouldBe "$baseUrl/utbetaldata/api/v2/hent-utbetalingsinformasjon/intern"
                kall.request.headers().firstValue("Nav-Call-Id").get() shouldBe correlationId.value
                kall.bodyTekst shouldEqualJson forventetRequest(forventetVerdi)
            }
        }
    }

    private fun List<UtbetalingsoversiktDto>.utenNavn() = map { utbetaling ->
        utbetaling.copy(
            utbetaltTil = utbetaling.utbetaltTil?.copy(navn = null),
            ytelseListe = utbetaling.ytelseListe.map { it.copy(rettighetshaver = it.rettighetshaver?.copy(navn = null)) },
        )
    }

    //language=json
    private fun forventetRequest(periodetype: String) = """
        {
          "ident": "${fnr.verdi}",
          "rolle": "RETTIGHETSHAVER",
          "periode": {
            "fom": "2025-08-01",
            "tom": "2025-09-30"
          },
          "periodetype": "$periodetype"
        }
    """.trimIndent()
}
