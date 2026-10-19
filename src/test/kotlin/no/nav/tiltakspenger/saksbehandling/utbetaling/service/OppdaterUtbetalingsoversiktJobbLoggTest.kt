package no.nav.tiltakspenger.saksbehandling.utbetaling.service

import io.github.oshai.kotlinlogging.Level
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.ktor.server.testing.ApplicationTestBuilder
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import no.nav.tiltakspenger.saksbehandling.common.Loggfanger
import no.nav.tiltakspenger.saksbehandling.common.Sikkerloggfanger
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContext
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.JobberEtterIverksettelse
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettOgIverksettMeldekortbehandling
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.utbetalingsoversikt.Avgrensningsårsak
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.utbetalingsoversikt.KunneIkkeHenteUtbetalingsoversikt
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.utbetalingsoversikt.UtbetalingsoversiktMappingfeil
import org.junit.jupiter.api.Test

/**
 * Hvert oppslag skal gi én logglinje: info når det lykkes og error når det feiler.
 * Avvik i avgrensningen gir ingen egen logglinje, men telles i metrikker.
 * Testene bygger jobben med egne loggfangere og deler ingen tilstand, slik at de tåler å kjøre parallelt med resten av testsuiten.
 * At en lagringsfeil bare gir én logglinje, testes i `UtbetalingsoversiktAggregatTest`, fordi den krever en isolert database.
 */
class OppdaterUtbetalingsoversiktJobbLoggTest {

    @Test
    fun `vellykket oppslag gir én info-linje og ingenting i sikkerlogg`() {
        withTestApplicationContextAndPostgres { tac ->
            val sak = sakMedOkUtbetaling(tac)
            val (jobb, loggfanger, sikkerloggfanger) = jobbMedFangere(tac)

            jobb.oppdaterForSak(sak.id)

            val linje = loggfanger.logglinjer.single()
            linje.nivå shouldBe Level.INFO
            linje.melding!! shouldContain "saksnummer=${sak.saksnummer}"
            linje.melding!! shouldContain "fjernedeYtelserPerÅrsak={}"
            sikkerloggfanger.sikkerlogglinjer.shouldBeEmpty()
        }
    }

    @Test
    fun `fjernede ytelser telles per årsak uten egen logglinje`() {
        withTestApplicationContextAndPostgres { tac ->
            val sak = sakMedOkUtbetaling(tac)
            tac.utbetalingsoversiktFakeKlient.fjernedeYtelserPerÅrsak = mapOf(
                Avgrensningsårsak.ANNEN_YTELSESTYPE to 3,
                Avgrensningsårsak.ANNEN_RETTIGHETSHAVER to 1,
            )
            val (jobb, loggfanger, _, meterRegistry) = jobbMedFangere(tac)

            jobb.oppdaterForSak(sak.id)

            fun antall(årsak: String) =
                meterRegistry.counter("tpts_saksbehandlingapi_utbetalingsoversikt_fjernede_ytelser", "aarsak", årsak).count()
            antall("annen_ytelsestype") shouldBe 3.0
            antall("annen_rettighetshaver") shouldBe 1.0
            antall("uten_ytelsestype") shouldBe 0.0
            antall("utenfor_periodene") shouldBe 0.0
            val linje = loggfanger.logglinjer.single()
            linje.nivå shouldBe Level.INFO
            linje.melding!! shouldContain "fjernedeYtelserPerÅrsak={ANNEN_YTELSESTYPE=3, ANNEN_RETTIGHETSHAVER=1}"
        }
    }

    @Test
    fun `feilet oppslag gir én error-linje og én error-linje i sikkerlogg`() {
        withTestApplicationContextAndPostgres { tac ->
            val sak = sakMedOkUtbetaling(tac)
            tac.utbetalingsoversiktFakeKlient.leggTilFeil(
                sak.fnr,
                KunneIkkeHenteUtbetalingsoversikt.Tjenestefeil(ObjectMother.httpKlientUventetStatus(500)),
            )
            val (jobb, loggfanger, sikkerloggfanger) = jobbMedFangere(tac)

            jobb.oppdaterForSak(sak.id)

            val linje = loggfanger.logglinjer.single()
            linje.nivå shouldBe Level.ERROR
            linje.melding!! shouldContain "Feil ved henting av utbetalingsoversikt"
            linje.melding!! shouldContain "status: 500"
            linje.melding!! shouldContain "saksnummer=${sak.saksnummer}"
            linje.melding!! shouldContain sikkerloggfanger.seSikkerlogg
            sikkerloggfanger.sikkerlogglinjer.single().nivå shouldBe Sikkerloggfanger.Nivå.ERROR
        }
    }

    @Test
    fun `svar som bryter kontrakten gir én error-linje og rått svar bare i sikkerlogg`() {
        withTestApplicationContextAndPostgres { tac ->
            val sak = sakMedOkUtbetaling(tac)
            val råttSvar = "rått svar med sensitivt innhold"
            tac.utbetalingsoversiktFakeKlient.leggTilFeil(
                sak.fnr,
                KunneIkkeHenteUtbetalingsoversikt.UgyldigInnhold(
                    feil = UtbetalingsoversiktMappingfeil.PåkrevdFeltMangler("posteringsdato"),
                    metadata = ObjectMother.httpKlientResponse(body = Unit, rawResponseString = råttSvar).metadata,
                ),
            )
            val (jobb, loggfanger, sikkerloggfanger) = jobbMedFangere(tac)

            jobb.oppdaterForSak(sak.id)

            val linje = loggfanger.logglinjer.single()
            linje.nivå shouldBe Level.ERROR
            linje.melding!! shouldContain "Svaret brøt kontrakten"
            linje.melding!! shouldNotContain råttSvar
            val sikkerlogglinje = sikkerloggfanger.sikkerlogglinjer.single()
            sikkerlogglinje.nivå shouldBe Sikkerloggfanger.Nivå.ERROR
            sikkerlogglinje.melding!! shouldContain råttSvar
        }
    }

    private data class JobbMedFangere(
        val jobb: OppdaterUtbetalingsoversiktJobb,
        val loggfanger: Loggfanger,
        val sikkerloggfanger: Sikkerloggfanger,
        val meterRegistry: SimpleMeterRegistry,
    )

    private fun jobbMedFangere(tac: TestApplicationContext): JobbMedFangere {
        val loggfanger = Loggfanger(OppdaterUtbetalingsoversiktJobb::class.java.name)
        val sikkerloggfanger = Sikkerloggfanger()
        val meterRegistry = SimpleMeterRegistry()
        return JobbMedFangere(
            jobb = OppdaterUtbetalingsoversiktJobb(
                sakRepo = tac.sakContext.sakRepo,
                utbetalingsoversiktRepo = tac.utbetalingContext.utbetalingsoversiktRepo,
                utbetalingsoversiktklient = tac.utbetalingsoversiktFakeKlient,
                clock = tac.clock,
                meterRegistry = meterRegistry,
                limit = 10,
                logger = loggfanger,
                sikkerlogg = sikkerloggfanger,
            ),
            loggfanger = loggfanger,
            sikkerloggfanger = sikkerloggfanger,
            meterRegistry = meterRegistry,
        )
    }

    private suspend fun ApplicationTestBuilder.sakMedOkUtbetaling(tac: TestApplicationContext): Sak {
        val sak = iverksettSøknadsbehandling(tac, jobber = JobberEtterIverksettelse.ingen).first
        opprettOgIverksettMeldekortbehandling(
            tac = tac,
            sakId = sak.id,
            kjedeId = sak.meldeperiodeKjeder.first().kjedeId,
            jobber = JobberEtterIverksettelse(journalførVedtaksbrev = false, distribuerVedtaksbrev = false),
        )
        return tac.sakContext.sakRepo.hentForSakId(sak.id)!!
    }
}
