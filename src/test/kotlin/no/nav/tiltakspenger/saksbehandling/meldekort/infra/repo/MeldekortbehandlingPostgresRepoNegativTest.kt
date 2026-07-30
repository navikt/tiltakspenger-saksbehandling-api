package no.nav.tiltakspenger.saksbehandling.meldekort.infra.repo

import io.kotest.assertions.throwables.shouldThrowWithMessage
import kotliquery.queryOf
import no.nav.tiltakspenger.libs.common.TikkendeKlokke
import no.nav.tiltakspenger.libs.common.fixedClockAt
import no.nav.tiltakspenger.libs.dato.mai
import no.nav.tiltakspenger.saksbehandling.common.IsolatedDatabaseTest
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgBehandleMeldekortAutomatisk
import org.junit.jupiter.api.Test

/**
 * Negative tester for [MeldekortbehandlingPostgresRepo]: databasetilstander prodkoden ikke kan skrive.
 *
 * Testene muterer databasen direkte, og det er selve poenget.
 * De verifiserer at repoet oppdager korrupt data i stedet for å mappe den til en ugyldig domenemodell.
 * Dette er unntak (a) i testtaksonomien, jf. `AGENTS.md`.
 */
class MeldekortbehandlingPostgresRepoNegativTest {

    /**
     * En automatisk behandling er per definisjon utledet av brukers meldekort, så referansen kan aldri mangle i prod.
     * Den er heller ikke lenger beskyttet av en foreign key: `brukers_meldekort_id`-kolonnen ble droppet i V230, og referansen ligger nå kun i `meldeperioder`-jsonb-en.
     * Derfor kan raden slettes under føttene på behandlingen, og guarden er verdt å ha.
     *
     * Isolert fordi tilstanden bygges med jobben som henter brukers meldekort på tvers av alle saker.
     */
    @Test
    @IsolatedDatabaseTest
    fun `kaster når brukers meldekort bak en automatisk behandling er borte`() {
        withTestApplicationContextAndPostgres(
            clock = TikkendeKlokke(fixedClockAt(2.mai(2025).atTime(12, 0))),
            runIsolated = true,
        ) { tac ->
            val (sak, brukersMeldekort) = iverksettSøknadsbehandlingOgBehandleMeldekortAutomatisk(tac = tac)
            val meldekortbehandlingId = tac.meldekortContext.meldekortbehandlingRepo
                .hentForSakId(sak.id)!!
                .sisteGodkjenteMeldekort!!
                .id

            tac.sessionFactory.withSession { session ->
                session.run(
                    queryOf(
                        "delete from meldekort_bruker where id = :id",
                        mapOf("id" to brukersMeldekort.id.toString()),
                    ).asUpdate,
                )
            }

            shouldThrowWithMessage<IllegalArgumentException>(
                "Fant ikke brukers meldekort for automatisk meldekortbehandling $meldekortbehandlingId",
            ) {
                tac.meldekortContext.meldekortbehandlingRepo.hentForSakId(sak.id)
            }
        }
    }
}
