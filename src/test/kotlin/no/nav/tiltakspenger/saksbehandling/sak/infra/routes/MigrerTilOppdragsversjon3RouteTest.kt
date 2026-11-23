package no.nav.tiltakspenger.saksbehandling.sak.infra.routes

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.httpklient.HttpKlientError
import no.nav.tiltakspenger.libs.httpklient.HttpKlientMetadata
import no.nav.tiltakspenger.libs.httpklient.HttpKlientTidsstempler
import no.nav.tiltakspenger.libs.httpklient.Tidsgrenser
import no.nav.tiltakspenger.libs.httpklient.UriSynlighet
import no.nav.tiltakspenger.libs.jobber.TaskResultat
import no.nav.tiltakspenger.libs.ktor.test.common.ForventetRespons
import no.nav.tiltakspenger.saksbehandling.common.IsolatedDatabaseTest
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContext
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.routes.JobberEtterIverksettelse
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.migrerTilOppdragsversjon3
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettOgIverksettMeldekortbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSakOgSøknad
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.KunneIkkeSimulere
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Oppdragsmigrering
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Oppdragstilstand
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Oppdragsversjon
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Simulering
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.SimuleringMedMetadata
import org.junit.jupiter.api.Test
import java.net.URI
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds

/**
 * Kjører mot postgres fordi ruten og jobben er eneste prodsti til sperren og byttet i `UtbetalingPostgresRepo`.
 * At en sperret sak ikke sendes, testes i `UtbetalingPostgresRepoOppdragsversjonTest`.
 */
class MigrerTilOppdragsversjon3RouteTest {

    @Test
    fun `migrerer sak med kvitterte utbetalinger, jobben opphever sperren når simuleringen er uten endring, og et nytt kall endrer ingenting`() {
        withTestApplicationContextAndPostgres { tac ->
            val sak = sakMedKvittertUtbetaling(tac)

            migrerTilOppdragsversjon3(tac, sak.id) shouldBe
                """{"sakId":"${sak.id}","saksnummer":"${sak.saksnummer}","oppdragsversjon":"V3","alleredeMigrert":false}"""
            oppdragsversjon(tac, sak) shouldBe Oppdragsversjon.V3
            tac.utbetalingFakeKlient.migrerteSaksnummer shouldBe listOf(sak.saksnummer)

            tac.utbetalingFakeKlient.simulering = ingenEndring(tac)
            tac.utbetalingContext.opphevUtbetalingssperrerJobb.opphevUtbetalingssperrer() shouldBe TaskResultat.Ferdig
            tac.utbetalingContext.opphevUtbetalingssperrerJobb.opphevUtbetalingssperrer() shouldBe TaskResultat.IngenArbeid
            erSperret(tac, sak) shouldBe false

            migrerTilOppdragsversjon3(tac, sak.id) shouldBe
                """{"sakId":"${sak.id}","saksnummer":"${sak.saksnummer}","oppdragsversjon":"V3","alleredeMigrert":true}"""
            tac.utbetalingFakeKlient.migrerteSaksnummer shouldBe listOf(sak.saksnummer)
        }
    }

    @Test
    fun `opphever sperren straks etter migrering uten kontrollsimulering`() {
        withTestApplicationContextAndPostgres { tac ->
            val sak = sakMedKvittertUtbetaling(tac)
            tac.utbetalingFakeKlient.simulerFeil = KunneIkkeSimulere.Timeout(uventetStatus(408))

            migrerTilOppdragsversjon3(tac, sak.id, opphevSperrenStraks = true) shouldBe
                """{"sakId":"${sak.id}","saksnummer":"${sak.saksnummer}","oppdragsversjon":"V3","alleredeMigrert":false}"""

            oppdragsversjon(tac, sak) shouldBe Oppdragsversjon.V3
            tilstand(tac, sak).migrering shouldBe Oppdragsmigrering.FERDIG
            tilstand(tac, sak).migrertHosHelVedTidspunkt shouldNotBe null
            erSperret(tac, sak) shouldBe false
        }
    }

    @Test
    fun `jobben lar sperren stå når simuleringen viser endring, også når Hel Ved har brukt for lang tid`() {
        withTestApplicationContextAndPostgres { tac ->
            val sak = sakMedKvittertUtbetaling(tac)
            migrerTilOppdragsversjon3(tac, sak.id)

            opphevUtbetalingssperre(tac, sak) shouldBe TaskResultat.Ferdig
            erSperret(tac, sak) shouldBe true

            tac.clock.spolFrem(2.hours)
            opphevUtbetalingssperre(tac, sak) shouldBe TaskResultat.Ferdig
            erSperret(tac, sak) shouldBe true
        }
    }

    @Test
    fun `jobben lar sperren stå når simuleringen feiler`() {
        withTestApplicationContextAndPostgres { tac ->
            val sak = sakMedKvittertUtbetaling(tac)
            migrerTilOppdragsversjon3(tac, sak.id)
            tac.utbetalingFakeKlient.simulerFeil = KunneIkkeSimulere.Timeout(uventetStatus(408))

            opphevUtbetalingssperre(tac, sak) shouldBe TaskResultat.Feilet
            erSperret(tac, sak) shouldBe true
        }
    }

    @Test
    fun `sak uten utbetalinger byttes bare hos oss, og jobben opphever sperren uten simulering`() {
        withTestApplicationContextAndPostgres { tac ->
            val (sak, _) = opprettSakOgSøknad(tac)

            migrerTilOppdragsversjon3(tac, sak.id)
            oppdragsversjon(tac, sak) shouldBe Oppdragsversjon.V3
            tac.utbetalingFakeKlient.migrerteSaksnummer.shouldBeEmpty()

            opphevUtbetalingssperre(tac, sak) shouldBe TaskResultat.Ferdig
            erSperret(tac, sak) shouldBe false
        }
    }

    /** Isolert fordi statusjobben i en annen test ellers kan kvittere utbetalingen. */
    @Test
    @IsolatedDatabaseTest
    fun `utbetaling som ikke er kvittert blokkerer migreringen uten å sperre saken`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            val (søknadsvedtakSak) = iverksettSøknadsbehandling(tac = tac)
            val (sak) = opprettOgIverksettMeldekortbehandling(
                tac = tac,
                sakId = søknadsvedtakSak.id,
                kjedeId = søknadsvedtakSak.meldeperiodeKjeder[0].kjedeId,
                jobber = JobberEtterIverksettelse(oppdaterUtbetalingsstatus = false),
            )!!

            migrerTilOppdragsversjon3(
                tac,
                sak.id,
                forventet = ForventetRespons.json(
                    status = 409,
                    json = """{"melding":"Saken har utbetalinger som ikke er kvittert OK: ${sak.utbetalinger.map { it.id }}","kode":"har_utbetaling_som_ikke_er_kvittert"}""",
                ),
            )

            oppdragsversjon(tac, sak) shouldBe Oppdragsversjon.V2
            erSperret(tac, sak) shouldBe false
            tac.utbetalingFakeKlient.migrerteSaksnummer.shouldBeEmpty()
        }
    }

    @Test
    fun `423 fra Hel Ved gir 409 og lar saken stå sperret på V2, og et nytt forsøk migrerer`() {
        withTestApplicationContextAndPostgres { tac ->
            val sak = sakMedKvittertUtbetaling(tac)
            tac.utbetalingFakeKlient.migreringFeil = uventetStatus(423)

            migrerTilOppdragsversjon3(
                tac,
                sak.id,
                forventet = ForventetRespons.json(
                    status = 409,
                    json = """{"melding":"Hel Ved har ikke kvittert siste iverksetting på saken. Prøv igjen senere.","kode":"helved_har_ikke_kvittert_siste_iverksetting"}""",
                ),
            )
            oppdragsversjon(tac, sak) shouldBe Oppdragsversjon.V2

            tac.utbetalingFakeKlient.migreringFeil = null
            migrerTilOppdragsversjon3(tac, sak.id) shouldBe
                """{"sakId":"${sak.id}","saksnummer":"${sak.saksnummer}","oppdragsversjon":"V3","alleredeMigrert":false}"""
            tac.utbetalingFakeKlient.migrerteSaksnummer shouldBe listOf(sak.saksnummer, sak.saksnummer)
        }
    }

    @Test
    fun `et annet kall som migrerer saken samtidig gir 409 og lar migreringen stå`() {
        withTestApplicationContextAndPostgres { tac ->
            val sak = sakMedKvittertUtbetaling(tac)
            tac.utbetalingFakeKlient.underMigrering = {
                tac.utbetalingContext.utbetalingRepo.oppdaterTilOppdragsversjon3(sak.id, nå(tac.clock))
            }

            migrerTilOppdragsversjon3(
                tac,
                sak.id,
                forventet = ForventetRespons.json(
                    status = 409,
                    json = """{"melding":"Et annet kall endret migreringen samtidig, så saken kan hentes på nytt.","kode":"oppdragsmigreringen_står_i_en_annen_tilstand"}""",
                ),
            )
            tilstand(tac, sak).migrering shouldBe Oppdragsmigrering.MIGRERT_HOS_HEL_VED
            erSperret(tac, sak) shouldBe true
        }
    }

    @Test
    fun `annen feil fra Hel Ved gir 502 og lar saken stå sperret på V2`() {
        withTestApplicationContextAndPostgres { tac ->
            val sak = sakMedKvittertUtbetaling(tac)
            tac.utbetalingFakeKlient.migreringFeil = uventetStatus(500)

            migrerTilOppdragsversjon3(
                tac,
                sak.id,
                forventet = ForventetRespons.json(
                    status = 502,
                    json = """{"melding":"Migreringen hos Hel Ved feilet. Saken står sperret på oppdragsversjon 2, og migreringen kan kjøres på nytt.","kode":"helved_migrering_feilet"}""",
                ),
            )

            oppdragsversjon(tac, sak) shouldBe Oppdragsversjon.V2
            erSperret(tac, sak) shouldBe true
        }
    }

    private suspend fun ApplicationTestBuilder.sakMedKvittertUtbetaling(tac: TestApplicationContext): Sak {
        val (søknadsvedtakSak) = iverksettSøknadsbehandling(tac = tac)
        val (sak) = opprettOgIverksettMeldekortbehandling(
            tac = tac,
            sakId = søknadsvedtakSak.id,
            kjedeId = søknadsvedtakSak.meldeperiodeKjeder[0].kjedeId,
        )!!
        return sak
    }

    private suspend fun opphevUtbetalingssperre(tac: TestApplicationContext, sak: Sak): TaskResultat {
        return tac.utbetalingContext.opphevUtbetalingssperrerJobb.opphevUtbetalingssperre(sak.id)
    }

    private fun ingenEndring(tac: TestApplicationContext): SimuleringMedMetadata {
        return SimuleringMedMetadata(Simulering.IngenEndring(nå(tac.clock)), "{}")
    }

    private fun oppdragsversjon(tac: TestApplicationContext, sak: Sak): Oppdragsversjon {
        return tac.utbetalingContext.utbetalingRepo.hentOppdragsversjon(sak.id)
    }

    private fun tilstand(tac: TestApplicationContext, sak: Sak): Oppdragstilstand {
        return tac.utbetalingContext.utbetalingRepo.hentOppdragstilstand(sak.id)
    }

    private fun erSperret(tac: TestApplicationContext, sak: Sak): Boolean {
        return tilstand(tac, sak).sperrerUtbetalinger
    }

    private fun uventetStatus(status: Int): HttpKlientError.UventetStatus {
        return HttpKlientError.UventetStatus(
            statusCode = status,
            body = "",
            metadata = HttpKlientMetadata(
                method = "POST",
                uri = URI.create("http://helved.test/api/iverksetting/v2/migrate"),
                uriSynlighet = UriSynlighet.VanligLogg,
                tidsgrenser = Tidsgrenser(svar = 30.seconds, oppkobling = 10.seconds),
                rawRequestString = "",
                rawResponseString = null,
                requestHeaders = emptyMap(),
                responseHeaders = emptyMap(),
                statusCode = status,
                attempts = 1,
                attemptDurations = emptyList(),
                totalDuration = Duration.ZERO,
                tidsstempler = HttpKlientTidsstempler.INGEN,
            ),
        )
    }
}
