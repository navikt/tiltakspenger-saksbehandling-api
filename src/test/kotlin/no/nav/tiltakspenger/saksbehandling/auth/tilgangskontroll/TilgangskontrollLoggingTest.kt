package no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll

import arrow.core.left
import arrow.core.right
import io.github.oshai.kotlinlogging.Level
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.common.Loggfanger
import no.nav.tiltakspenger.saksbehandling.common.Sikkerloggfanger
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import org.junit.jupiter.api.Test

/**
 * Benken henter tilganger ved hver sidevisning, så et vellykket bulkoppslag skal ikke gi en eneste logglinje.
 * Testene bygger servicen med egne fangere per test og deler ingen tilstand, slik at de tåler å kjøre parallelt med resten av suiten.
 */
class TilgangskontrollLoggingTest {
    private val fnr = Fnr.random()
    private val fnr2 = Fnr.random()
    private val fnrs = listOf(fnr, fnr2)
    private val saksbehandler = ObjectMother.saksbehandler()

    @Test
    fun `harTilgangTilPersoner - alle godkjent - logger ingenting og lekker ingen fødselsnumre`() = runTest {
        val tilgangsmaskinClient = mockk<TilgangsmaskinClient>()
        val (service, loggfanger, sikkerloggfanger) = serviceMedFangere(tilgangsmaskinClient)
        coEvery { tilgangsmaskinClient.harTilgangTilPersoner(fnrs, any()) } returns responsMedFnrIRåstrengene(
            perFnr = mapOf(fnr to TilgangsvurderingBulk.Godkjent, fnr2 to TilgangsvurderingBulk.Godkjent),
            ukjenteAvvisningskoder = emptySet(),
        ).right()

        service.harTilgangTilPersoner(fnrs, "token", saksbehandler, CorrelationId.generate()).isRight() shouldBe true

        loggfanger.logglinjer shouldBe emptyList()
        sikkerloggfanger.sikkerlogglinjer shouldBe emptyList()
        altSomBleLogget(loggfanger, sikkerloggfanger).also { logget ->
            logget shouldNotContain fnr.verdi
            logget shouldNotContain fnr2.verdi
        }
    }

    /** Avviste rader er normal drift nå som benken viser dem, så de skal heller ikke gi logglinjer. */
    @Test
    fun `harTilgangTilPersoner - avvist med kjent årsak - logger ingenting`() = runTest {
        val tilgangsmaskinClient = mockk<TilgangsmaskinClient>()
        val (service, loggfanger, sikkerloggfanger) = serviceMedFangere(tilgangsmaskinClient)
        coEvery { tilgangsmaskinClient.harTilgangTilPersoner(fnrs, any()) } returns responsMedFnrIRåstrengene(
            perFnr = mapOf(
                fnr to TilgangsvurderingBulk.Godkjent,
                fnr2 to TilgangsvurderingBulk.Avvist(
                    årsak = TilgangsvurderingAvvistÅrsak.GEOGRAFISK,
                    begrunnelse = "Du har ikke geografisk tilgang",
                ),
            ),
            ukjenteAvvisningskoder = emptySet(),
        ).right()

        service.harTilgangTilPersoner(fnrs, "token", saksbehandler, CorrelationId.generate()).isRight() shouldBe true

        loggfanger.logglinjer shouldBe emptyList()
        sikkerloggfanger.sikkerlogglinjer shouldBe emptyList()
    }

    /**
     * En kode vi ikke kjenner, er avviket vi vil vite om.
     * Koden er Tilgangsmaskinens regelnavn og ikke en personopplysning, så linja hører hjemme i vanlig logg — uten fødselsnumre.
     */
    @Test
    fun `harTilgangTilPersoner - ukjent avvisningskode - gir én warn-linje uten fødselsnummer`() = runTest {
        val kode = "AVVIST_EN_REGEL_BARE_LOGGTESTEN_BRUKER"
        val tilgangsmaskinClient = mockk<TilgangsmaskinClient>()
        val (service, loggfanger, sikkerloggfanger) = serviceMedFangere(tilgangsmaskinClient)
        coEvery { tilgangsmaskinClient.harTilgangTilPersoner(fnrs, any()) } returns responsMedFnrIRåstrengene(
            perFnr = mapOf(
                fnr to TilgangsvurderingBulk.Godkjent,
                fnr2 to TilgangsvurderingBulk.Avvist(
                    årsak = TilgangsvurderingAvvistÅrsak.UKJENT,
                    begrunnelse = "Avvist av en regel vi ikke kjenner",
                ),
            ),
            ukjenteAvvisningskoder = setOf(kode),
        ).right()

        service.harTilgangTilPersoner(fnrs, "token", saksbehandler, CorrelationId.generate()).isRight() shouldBe true

        loggfanger.logglinjer.size shouldBe 1
        loggfanger.linjerPå(Level.WARN).single().melding!! shouldContain kode
        sikkerloggfanger.sikkerlogglinjer shouldBe emptyList()
        altSomBleLogget(loggfanger, sikkerloggfanger).also { logget ->
            logget shouldNotContain fnr.verdi
            logget shouldNotContain fnr2.verdi
        }
    }

    /**
     * Den samme ukjente koden treffer hver rad den gjelder, hver sidevisning, hele dagen.
     * Da holder det med én linje fra poden; alarmen står uansett, for telleren økes hver gang.
     */
    @Test
    fun `harTilgangTilPersoner - samme ukjente avvisningskode to ganger - logger bare første gang`() = runTest {
        val kode = "AVVIST_EN_ANNEN_REGEL_BARE_LOGGTESTEN_BRUKER"
        val tilgangsmaskinClient = mockk<TilgangsmaskinClient>()
        val (service, loggfanger, _) = serviceMedFangere(tilgangsmaskinClient)
        coEvery { tilgangsmaskinClient.harTilgangTilPersoner(fnrs, any()) } returns responsMedFnrIRåstrengene(
            perFnr = mapOf(
                fnr to TilgangsvurderingBulk.Godkjent,
                fnr2 to TilgangsvurderingBulk.Avvist(
                    årsak = TilgangsvurderingAvvistÅrsak.UKJENT,
                    begrunnelse = "Avvist av en regel vi ikke kjenner",
                ),
            ),
            ukjenteAvvisningskoder = setOf(kode),
        ).right()

        repeat(5) {
            service.harTilgangTilPersoner(fnrs, "token", saksbehandler, CorrelationId.generate()).isRight() shouldBe true
        }

        loggfanger.linjerPå(Level.WARN).size shouldBe 1
    }

    /**
     * En feil er sjelden, og da vil vi ha den: én error-linje i vanlig logg og den rå forespørselen i sikkerloggen.
     * Sikkerlogglinja beviser samtidig at den injiserte instansen brukes, og ikke companion-objektet.
     */
    @Test
    fun `harTilgangTilPersoner - teknisk feil - gir én error-linje i vanlig logg`() = runTest {
        val tilgangsmaskinClient = mockk<TilgangsmaskinClient>()
        val (service, loggfanger, sikkerloggfanger) = serviceMedFangere(tilgangsmaskinClient)
        val feil = TilgangskontrollFeil.Uventet(ObjectMother.httpKlientUventetStatus(statusCode = 500))
        coEvery { tilgangsmaskinClient.harTilgangTilPersoner(fnrs, any()) } returns feil.left()

        service.harTilgangTilPersoner(fnrs, "token", saksbehandler, CorrelationId.generate()).isLeft() shouldBe true

        loggfanger.logglinjer.size shouldBe 1
        loggfanger.linjerPå(Level.ERROR).single().melding!! shouldContain "Feil ved tilgangskontroll mot tilgangsmaskinen"
        sikkerloggfanger.linjerPå(Sikkerloggfanger.Nivå.ERROR).size shouldBe 1
    }

    private data class ServiceMedFangere(
        val service: TilgangskontrollService,
        val loggfanger: Loggfanger,
        val sikkerloggfanger: Sikkerloggfanger,
    )

    private fun serviceMedFangere(tilgangsmaskinClient: TilgangsmaskinClient): ServiceMedFangere {
        val loggfanger = Loggfanger(TilgangskontrollService::class.java.name)
        val sikkerloggfanger = Sikkerloggfanger()
        return ServiceMedFangere(
            service = TilgangskontrollService(
                tilgangsmaskinClient = tilgangsmaskinClient,
                sakService = mockk<SakService>(),
                log = loggfanger,
                sikkerlogg = sikkerloggfanger,
            ),
            loggfanger = loggfanger,
            sikkerloggfanger = sikkerloggfanger,
        )
    }

    /**
     * Fødselsnumrene ligger både i bodyen og i de rå strengene i metadataen, der `loggSuksess` pleide å hente dem fra.
     * Da fanger lekkasjetesten også en logglinje som skriver ut rå request eller respons, ikke bare en som skriver ut bodyen.
     */
    private fun responsMedFnrIRåstrengene(
        perFnr: Map<Fnr, TilgangsvurderingBulk>,
        ukjenteAvvisningskoder: Set<String>,
    ) = ObjectMother.httpKlientResponse(
        body = Tilgangsvurderinger(perFnr = perFnr, ukjenteAvvisningskoder = ukjenteAvvisningskoder),
        statusCode = 207,
        rawRequestString = """{"brukere":["${fnr.verdi}","${fnr2.verdi}"]}""",
        rawResponseString = """[{"brukerIdent":"${fnr.verdi}"},{"brukerIdent":"${fnr2.verdi}"}]""",
    )

    /**
     * Melding og årsakskjede fra begge fangerne slått sammen til én streng.
     * Årsakskjeden er med fordi et fødselsnummer også kan lekke gjennom meldingen til en nestet exception, ikke bare gjennom logglinjas egen tekst.
     */
    private fun altSomBleLogget(
        loggfanger: Loggfanger,
        sikkerloggfanger: Sikkerloggfanger,
    ): String {
        val vanlige = loggfanger.logglinjer.map { "${it.melding} ${it.årsakskjede()}" }
        val sikre = sikkerloggfanger.sikkerlogglinjer.map { "${it.melding} ${it.årsakskjede()}" }
        return (vanlige + sikre).joinToString(" | ")
    }
}
