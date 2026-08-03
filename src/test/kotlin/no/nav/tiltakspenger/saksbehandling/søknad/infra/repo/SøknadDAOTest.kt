package no.nav.tiltakspenger.saksbehandling.søknad.infra.repo

import io.kotest.matchers.shouldBe
import kotliquery.queryOf
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SøknadId
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.persistering.infrastruktur.PostgresSessionFactory
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.mottaSøknad
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSakOgSøknad
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

/**
 * `lagreHeleSøknaden` er idempotent: finnes søknaden fra før, lar den den ligge urørt.
 * Kafka gir «minst én gang»-levering, så den samme søknaden kan komme inn på nytt, og da skal vi ikke skrive over det som allerede står der.
 *
 * Testen leser `opprettet` og radantallet med egen SQL.
 * Domenemodellen viser ikke om raden ble skrevet på nytt med samme innhold, og det er nettopp det vakten skal hindre.
 */
class SøknadDAOTest {

    @Test
    fun `samme søknad levert to ganger lagres bare én gang`() {
        withTestApplicationContextAndPostgres { tac ->
            val fnr = Fnr.random()
            val søknadId = SøknadId.random()
            val tiltaksdeltakelse = tac.tiltaksdeltakelse()
            val (sak) = opprettSakOgSøknad(
                tac = tac,
                fnr = fnr,
                søknadId = søknadId,
                tiltaksdeltakelse = tiltaksdeltakelse,
            )
            val førsteOpprettet = tac.sessionFactory.opprettetForSøknad(søknadId)

            mottaSøknad(
                tac = tac,
                fnr = fnr,
                saksnummer = sak.saksnummer,
                søknadId = søknadId,
                tiltaksdeltakelse = tiltaksdeltakelse,
            )

            // Raden er urørt — uten vakten ville primærnøkkelen slått ut, eller `opprettet` blitt skrevet på nytt.
            tac.sessionFactory.opprettetForSøknad(søknadId) shouldBe førsteOpprettet
            tac.sessionFactory.antallRader("søknad", "id", søknadId.toString()) shouldBe 1
            tac.sessionFactory.antallRader("søknadstiltak", "søknad_id", søknadId.toString()) shouldBe 1
            tac.sakContext.sakRepo.hentForSakId(sak.id)!!.søknader.size shouldBe 1
        }
    }
}

private fun PostgresSessionFactory.opprettetForSøknad(søknadId: SøknadId): LocalDateTime? = withSession { session ->
    session.run(
        queryOf(
            "select opprettet from søknad where id = :id",
            mapOf("id" to søknadId.toString()),
        ).map { row -> row.localDateTime("opprettet") }.asSingle,
    )
}

private fun PostgresSessionFactory.antallRader(tabell: String, kolonne: String, verdi: String): Int? =
    withSession { session ->
        session.run(
            queryOf(
                "select count(*) as antall from $tabell where $kolonne = :verdi",
                mapOf("verdi" to verdi),
            ).map { row -> row.int("antall") }.asSingle,
        )
    }
