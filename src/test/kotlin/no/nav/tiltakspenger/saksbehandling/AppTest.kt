package no.nav.tiltakspenger.saksbehandling

import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopping
import io.ktor.server.application.ServerReady
import io.ktor.server.testing.testApplication
import no.nav.tiltakspenger.libs.ktor.common.oppstart.Readiness
import no.nav.tiltakspenger.libs.ktor.common.oppstart.konfigurerOppstart
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContextMedInMemoryDb
import no.nav.tiltakspenger.saksbehandling.infra.setup.ktorSetup
import org.junit.jupiter.api.Test

/**
 * Den generiske oppstarts- og livssyklusorkestreringen testes i ktor-common; her verifiserer vi kun at saksbehandling-api wirer den sammen med sitt eget [ktorSetup] og sine ekte bakgrunnsprosesser.
 */
class AppTest {
    private val log = KotlinLogging.logger { }

    /**
     * Verifiserer at registeret jobbene og Kafka-consumerne skriver målingene sine til, er det samme registeret `/metrics` skraper.
     * Det er hele poenget med at [no.nav.tiltakspenger.saksbehandling.infra.setup.ApplicationContext] eier registeret: sender vi inn et annet register i `Jobboppsett` eller i en consumer, forsvinner seriene stille, og varselreglene «Jobb har stoppet» og «Meldingsleser har stoppet» får aldri data.
     * Oppsettet hentes fra [bakgrunnsprosessoppsett], altså den samme lista som `start()` bruker, slik at en feil i komposisjonsroten også fanges her.
     *
     * Consumeren konstrueres, men startes ikke.
     * Meldingsleser-målingene registreres i konstruktøren til `ManagedKafkaConsumer`, mens `run()` ville krevd en ekte Kafka-broker.
     * Jobbmålingene registreres når skedulereren starter, altså ved [ServerReady].
     */
    @Test
    fun `jobbene og consumerne fører målingene sine i registeret metrics skraper`() = testApplication {
        val context = TestApplicationContextMedInMemoryDb()
        val readiness = Readiness()
        lateinit var app: Application
        application {
            app = this
            ktorSetup(applicationContext = context, readiness = readiness)
            konfigurerOppstart(
                log = log,
                isNais = false,
                readiness = readiness,
                // Utenfor Nais er leader election lokal, electorPath leses aldri, og consumer-lista er tom, så ingen consumer startes.
                oppsett = bakgrunnsprosessoppsett(applicationContext = context, isNais = false),
            )
        }

        // `application { }` er lat i testoppsettet, så appen må startes eksplisitt før `app` er satt.
        startApplication()

        // Konstruerer consumeren uten å starte den, slik at meldingsleser-målingene registreres på kontekstens register.
        context.tilbakekrevingConsumer

        app.monitor.raise(ServerReady, app.environment)

        client.get("/metrics").apply {
            status shouldBe HttpStatusCode.OK
            val metrikker = bodyAsText()
            metrikker shouldContain
                """tpts_bakgrunnsprosess_intervall_sekunder{prosess="saksbehandling-jobb-send-utbetalinger",type="jobb"}"""
            metrikker shouldContain
                """tpts_bakgrunnsprosess_sist_vellykket_tidspunkt_sekunder{prosess="saksbehandling-jobb-send-utbetalinger",type="jobb"}"""
            metrikker shouldContain
                """tpts_bakgrunnsprosess_intervall_sekunder{prosess="tilbake.privat-tilbakekreving-tiltakspenger",type="meldingsleser"}"""
            metrikker shouldContain
                """tpts_bakgrunnsprosess_sist_vellykket_tidspunkt_sekunder{prosess="tilbake.privat-tilbakekreving-tiltakspenger",type="meldingsleser"}"""
        }

        app.monitor.raise(ApplicationStopping, app)
    }
}
