package no.nav.tiltakspenger.saksbehandling.klage.infra.repo

import io.kotest.matchers.shouldBe
import kotliquery.queryOf
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.felles.Ventestatus
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSakOgKlagebehandlingTilAvvisning
import org.junit.jupiter.api.Test

/**
 * Negative tester for radmappingen i [KlagebehandlingPostgresRepo]: databasetilstander prodkoden ikke kan skrive.
 *
 * Testene muterer databasen direkte, og det er selve poenget.
 * De verifiserer at repoet enten oppdager korrupt data i stedet for å mappe den til en ugyldig domenemodell, eller faller tilbake på en trygg verdi der raden er eldre enn kolonnen.
 * Dette er unntak (a) i testtaksonomien, jf. `AGENTS.md`.
 */
class KlagebehandlingPostgresRepoNegativTest {

    /**
     * `ventestatus` kom inn som `jsonb DEFAULT NULL` i V185, og radene som fantes da beholdt NULL.
     * Skrivestien setter alltid en verdi, så tilstanden kan bare bygges ved å mutere raden.
     * Leses en slik rad, skal vi få en tom ventestatus — ikke et null-felt på en domenemodell som ikke tåler det.
     */
    @Test
    fun `en rad som er eldre enn ventestatus-kolonnen leses som tom ventestatus`() {
        withTestApplicationContextAndPostgres { tac ->
            val (_, klagebehandling) = opprettSakOgKlagebehandlingTilAvvisning(tac = tac)!!

            tac.sessionFactory.withSession { session ->
                session.run(
                    queryOf(
                        "update klagebehandling set ventestatus = null where id = :id",
                        mapOf("id" to klagebehandling.id.toString()),
                    ).asUpdate,
                )
            }

            tac.klagebehandlingContext.klagebehandlingRepo
                .hentForKlagebehandlingId(klagebehandling.id)!!
                .ventestatus shouldBe Ventestatus()
        }
    }
}
