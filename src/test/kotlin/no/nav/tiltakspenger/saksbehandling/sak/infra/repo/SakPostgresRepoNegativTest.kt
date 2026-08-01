package no.nav.tiltakspenger.saksbehandling.sak.infra.repo

import arrow.core.nonEmptyListOf
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.string.shouldContain
import kotliquery.queryOf
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSakOgSøknad
import org.junit.jupiter.api.Test
import org.postgresql.util.PSQLException

/**
 * Tilstander [SakPostgresRepo] må håndtere, men som prodkoden ikke kan skrive gjennom rutene.
 * Testene bygger sakene gjennom prodstiene og går utenom domenemodellen først når den ugyldige tilstanden skal fremtvinges.
 * Dette er unntak (a) i testtaksonomien, jf. `AGENTS.md`.
 */
class SakPostgresRepoNegativTest {

    /**
     * `SakPostgresRepo.hentForFnr` returnerer én sak eller null, og den garantien hviler på `sak_fnr_unique`.
     * Testen verifiserer at constrainten faktisk finnes, ved å forsøke innsettingen den skal stoppe.
     * En constraint er ikke sterkere enn migreringen som holder den i live, jf. «Full dekning på postgres-repoene» i `../AGENTS-backend.md`.
     */
    @Test
    fun `en person kan ikke ha to saker`() {
        withTestApplicationContextAndPostgres { tac ->
            val (sak, _) = opprettSakOgSøknad(tac)

            val forsøkPåDuplikat = shouldThrow<PSQLException> {
                tac.sessionFactory.withSession { session ->
                    session.run(
                        queryOf(
                            """
                            insert into sak (id, fnr, saksnummer, sist_endret, opprettet, skal_sendes_til_meldekort_api, skal_sende_meldeperioder_til_datadeling, sendt_til_datadeling)
                            select :id, fnr, :saksnummer, sist_endret, opprettet, false, false, null from sak where id = :kildeId
                            """.trimIndent(),
                            mapOf(
                                "id" to SakId.random().toString(),
                                "saksnummer" to "202101011999",
                                "kildeId" to sak.id.toString(),
                            ),
                        ).asUpdate,
                    )
                }
            }

            forsøkPåDuplikat.message shouldContain "sak_fnr_unique"
        }
    }

    /**
     * `hentSakIdForPersonidenter` slår opp på alle identene vi kjenner til for en person, typisk etter en identhendelse.
     * Treffer den to saker, har vi to saker på det som skal være samme person, og da skal den stoppe i stedet for å velge én av dem.
     * Tilstanden er lovlig i databasen — `sak_fnr_unique` er per fnr — så den bygges med to ordinære saker.
     */
    @Test
    fun `hentSakIdForPersonidenter kaster når flere saker matcher`() {
        withTestApplicationContextAndPostgres { tac ->
            val (sak1, _) = opprettSakOgSøknad(tac)
            val (sak2, _) = opprettSakOgSøknad(tac)

            shouldThrow<IllegalStateException> {
                tac.sakContext.sakRepo.hentSakIdForPersonidenter(
                    nonEmptyListOf(sak1.fnr.verdi, sak2.fnr.verdi),
                )
            }
        }
    }
}
