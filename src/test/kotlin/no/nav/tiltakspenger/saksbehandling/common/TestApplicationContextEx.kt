package no.nav.tiltakspenger.saksbehandling.common

import io.ktor.server.application.Application
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import no.nav.tiltakspenger.libs.common.TestSessionFactory
import no.nav.tiltakspenger.libs.common.TikkendeKlokke
import no.nav.tiltakspenger.libs.persistering.domene.SessionFactory
import no.nav.tiltakspenger.libs.texas.client.TexasClient
import no.nav.tiltakspenger.saksbehandling.auth.infra.TexasClientFake
import no.nav.tiltakspenger.saksbehandling.auth.tilgangskontroll.infra.TilgangsmaskinFakeTestClient
import no.nav.tiltakspenger.saksbehandling.fixedClock
import no.nav.tiltakspenger.saksbehandling.infra.setup.ktorSetup

fun withTestApplicationContext(
    additionalConfig: Application.() -> Unit = {},
    clock: TikkendeKlokke = TikkendeKlokke(fixedClock),
    texasClient: TexasClient = TexasClientFake(),
    sessionFactory: SessionFactory = TestSessionFactory(),
    tilgangsmaskinFakeClient: TilgangsmaskinFakeTestClient = TilgangsmaskinFakeTestClient(),
    testBlock: suspend ApplicationTestBuilder.(TestApplicationContext) -> Unit,
) {
    with(
        TestApplicationContext(
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
