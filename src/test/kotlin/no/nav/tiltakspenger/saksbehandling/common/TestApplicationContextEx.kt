package no.nav.tiltakspenger.saksbehandling.common

import io.ktor.server.application.Application
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import no.nav.tiltakspenger.libs.common.TestSessionFactory
import no.nav.tiltakspenger.libs.common.TikkendeKlokke
import no.nav.tiltakspenger.libs.dato.mai
import no.nav.tiltakspenger.libs.texas.client.TexasClient
import no.nav.tiltakspenger.saksbehandling.auth.infra.TexasClientFake
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.infra.TilgangsmaskinFakeTestClient
import no.nav.tiltakspenger.saksbehandling.fixedClockAt
import no.nav.tiltakspenger.saksbehandling.infra.repo.TestDatabaseManager
import no.nav.tiltakspenger.saksbehandling.infra.setup.ktorSetup

private val dbManager = TestDatabaseManager()

fun withTestApplicationContextAndPostgres(
    additionalConfig: Application.() -> Unit = {},
    clock: TikkendeKlokke = TikkendeKlokke(fixedClockAt(1.mai(2025))),
    texasClient: TexasClient = TexasClientFake(clock),
    tilgangsmaskinFakeClient: TilgangsmaskinFakeTestClient = TilgangsmaskinFakeTestClient(),
    runIsolated: Boolean = false,
    testBlock: suspend ApplicationTestBuilder.(TestApplicationContextMedPostgres) -> Unit,
) {
    dbManager.withMigratedDb(runIsolated = runIsolated, clock = clock) { testDataHelper ->
        with(
            TestApplicationContextMedPostgres(
                clock = clock,
                texasClient = texasClient,
                sessionFactory = testDataHelper.sessionFactory,
                tilgangsmaskinFakeClient = tilgangsmaskinFakeClient,
            ),
        ) {
            val tac = this
            testApplication {
                application {
                    ktorSetup(tac)
                    additionalConfig()
                }
                testBlock(this@with)
            }
        }
    }
}

/**
 * @param clock Merk at vi ikke kan behandle et meldekort før vi har passert meldekortets første dag. Derfor er det viktig at [clock] er satt til en dato etter den første meldekortdagen i testene som bruker denne.
 */
fun withTestApplicationContext(
    additionalConfig: Application.() -> Unit = {},
    clock: TikkendeKlokke = TikkendeKlokke(fixedClockAt(1.mai(2025))),
    texasClient: TexasClient = TexasClientFake(clock),
    sessionFactory: TestSessionFactory = TestSessionFactory(),
    tilgangsmaskinFakeClient: TilgangsmaskinFakeTestClient = TilgangsmaskinFakeTestClient(),
    testBlock: suspend ApplicationTestBuilder.(TestApplicationContextMedInMemoryDb) -> Unit,
) {
    with(
        TestApplicationContextMedInMemoryDb(
            clock = clock,
            texasClient = texasClient,
            sessionFactory = sessionFactory,
            tilgangsmaskinFakeClient = tilgangsmaskinFakeClient,
        ),
    ) {
        val tac = this
        testApplication {
            application {
                ktorSetup(tac)
                additionalConfig()
            }
            testBlock(this@with)
        }
    }
}
