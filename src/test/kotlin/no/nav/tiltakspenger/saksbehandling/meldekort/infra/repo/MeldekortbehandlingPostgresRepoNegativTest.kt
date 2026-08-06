package no.nav.tiltakspenger.saksbehandling.meldekort.infra.repo

import io.kotest.assertions.throwables.shouldThrowWithMessage
import io.kotest.matchers.shouldBe
import kotliquery.queryOf
import no.nav.tiltakspenger.libs.common.TikkendeKlokke
import no.nav.tiltakspenger.libs.common.fixedClockAt
import no.nav.tiltakspenger.libs.dato.mai
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeId
import no.nav.tiltakspenger.saksbehandling.common.IsolatedDatabaseTest
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.felles.Ventestatus
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgBehandleMeldekortAutomatisk
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgOpprettMeldekortbehandling
import org.junit.jupiter.api.Test

/**
 * Negative tester for [MeldekortbehandlingPostgresRepo]: databasetilstander prodkoden ikke kan skrive.
 *
 * Testene muterer databasen direkte, og det er selve poenget.
 * De verifiserer at repoet enten oppdager korrupt data i stedet for å mappe den til en ugyldig domenemodell, eller faller tilbake på en trygg verdi der raden er eldre enn kolonnen.
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
                "Fant ikke brukers meldekort ${brukersMeldekort.id} for meldekortbehandling $meldekortbehandlingId",
            ) {
                tac.meldekortContext.meldekortbehandlingRepo.hentForSakId(sak.id)
            }
        }
    }

    /**
     * Referansen fra `meldeperioder`-jsonb-en til meldeperioden er ikke beskyttet av noen foreign key, siden id-en ligger inne i jsonb-dokumentet.
     * Prodkoden skriver alltid en eksisterende meldeperiode, så tilstanden kan bare bygges ved å mutere jsonb-en direkte.
     * Da skal repoet kaste i stedet for å mappe behandlingen uten meldeperiode.
     */
    @Test
    fun `kaster når meldeperioder-jsonb refererer en meldeperiode som ikke finnes`() {
        withTestApplicationContextAndPostgres { tac ->
            val (_, _, _, meldekortbehandling) = iverksettSøknadsbehandlingOgOpprettMeldekortbehandling(tac = tac)!!
            val ukjentMeldeperiodeId = MeldeperiodeId.random()

            tac.sessionFactory.withSession { session ->
                session.run(
                    queryOf(
                        "update meldekortbehandling set meldeperioder = jsonb_set(meldeperioder, '{0,meldeperiodeId}', :nyId::jsonb) where id = :id",
                        mapOf(
                            "nyId" to "\"$ukjentMeldeperiodeId\"",
                            "id" to meldekortbehandling.id.toString(),
                        ),
                    ).asUpdate,
                )
            }

            shouldThrowWithMessage<IllegalStateException>(
                "Fant ikke meldeperiode $ukjentMeldeperiodeId for meldekortbehandling ${meldekortbehandling.id}",
            ) {
                tac.meldekortContext.meldekortbehandlingRepo.hent(meldekortbehandling.id)
            }
        }
    }

    /**
     * `ventestatus` kom inn som `jsonb DEFAULT NULL` i V227, og radene som fantes da beholdt NULL.
     * Skrivestien setter alltid en verdi, så tilstanden kan bare bygges ved å mutere raden.
     * Leses en slik rad, skal vi få en tom ventestatus — ikke et null-felt på en domenemodell som ikke tåler det.
     */
    @Test
    fun `en rad som er eldre enn ventestatus-kolonnen leses som tom ventestatus`() {
        withTestApplicationContextAndPostgres { tac ->
            val (_, _, _, meldekortbehandling) = iverksettSøknadsbehandlingOgOpprettMeldekortbehandling(tac = tac)!!

            tac.sessionFactory.withSession { session ->
                session.run(
                    queryOf(
                        "update meldekortbehandling set ventestatus = null where id = :id",
                        mapOf("id" to meldekortbehandling.id.toString()),
                    ).asUpdate,
                )
            }

            tac.meldekortContext.meldekortbehandlingRepo.hent(meldekortbehandling.id)!!.ventestatus shouldBe Ventestatus()
        }
    }

    /**
     * `Begrunnelse.create` og `FritekstTilVedtaksbrev.create` gir null for en blank streng, så skrivestien kan ikke legge igjen en.
     * Eldre rader kan likevel inneholde det, og da skal en blank tekst leses som fravær av tekst — ikke som en tom fritekst i brevet.
     */
    @Test
    fun `blanke tekstkolonner leses som fravær av tekst`() {
        withTestApplicationContextAndPostgres { tac ->
            val (_, _, _, meldekortbehandling) = iverksettSøknadsbehandlingOgOpprettMeldekortbehandling(tac = tac)!!

            tac.sessionFactory.withSession { session ->
                session.run(
                    queryOf(
                        "update meldekortbehandling set begrunnelse = '   ', tekst_til_vedtaksbrev = '   ' where id = :id",
                        mapOf("id" to meldekortbehandling.id.toString()),
                    ).asUpdate,
                )
            }

            tac.meldekortContext.meldekortbehandlingRepo.hent(meldekortbehandling.id)!!.also {
                it.begrunnelse shouldBe null
                it.fritekstTilVedtaksbrev shouldBe null
            }
        }
    }
}
