package no.nav.tiltakspenger.saksbehandling.statistikk.kodeverk

import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.persistering.infra.PostgresSessionFactory
import no.nav.tiltakspenger.libs.persistering.infra.sqlQuery
import no.nav.tiltakspenger.saksbehandling.common.IsolatedDatabaseTest
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContextMedPostgres
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import org.junit.jupiter.api.Test
import java.time.Instant

/**
 * `statistikk_kodeverk_tiltakstype` skrives av oss og leses bare av DVH, så testen leser radene tilbake med egen SQL; se «Data som skrives, men aldri leses ut i domenet» i `../AGENTS-backend.md`.
 * Testene kjører isolert, fordi tabellen ikke tilhører en sak og ville blitt delt med andre tester.
 */
class StatistikkKodeverkTiltakstypeTest {

    @Test
    @IsolatedDatabaseTest
    fun `synkroniseringen fyller kodeverket fra koden én gang per pod`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            val service = tac.statistikkContext.statistikkService

            service.synkroniserTiltakstypekodeverk() shouldBe true
            service.synkroniserTiltakstypekodeverk() shouldBe false

            val rader = tac.hentTiltakstypekodeverk()
            rader.map { it.rad } shouldContainExactlyInAnyOrder gjeldendeTiltakstypekodeverk()
            rader.forEach {
                it.gyldigFra shouldBe null
                it.gyldigTil shouldBe null
                it.sistEndret shouldBe it.opprettet
            }
            rader.single { it.rad.eksternKildesystem == "Komet" && it.rad.eksternTiltakskode == "HOYERE_YRKESFAGLIG_UTDANNING" }.rad.eksternStønadstype shouldBe "FAGSKOLE"
        }
    }

    @Test
    @IsolatedDatabaseTest
    fun `endret, ny og fjernet mapping lukker og åpner rader`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            val repo = tac.statistikkContext.statistikkRepo
            val uendret = Tiltakstypekodeverkrad("Arena", "A", "ARBEIDSTRENING")
            val endret = Tiltakstypekodeverkrad("Arena", "B", "AVKLARING")
            val endretTil = endret.copy(eksternStønadstype = "OPPFØLGING")
            val ny = Tiltakstypekodeverkrad("Komet", "A", "ARBEIDSTRENING")

            repo.synkroniserTiltakstypekodeverk(setOf(uendret, endret))
            repo.synkroniserTiltakstypekodeverk(setOf(uendret, endret))
            repo.synkroniserTiltakstypekodeverk(setOf(uendret, endretTil, ny))

            val rader = tac.hentTiltakstypekodeverk()
            rader.map { it.rad } shouldContainExactlyInAnyOrder listOf(uendret, endret, endretTil, ny)
            rader.single { it.rad == uendret }.let {
                it.gyldigFra shouldBe null
                it.gyldigTil shouldBe null
            }
            val lukket = rader.single { it.rad == endret }
            lukket.gyldigFra shouldBe null
            lukket.gyldigTil.shouldNotBeNull()
            lukket.sistEndret shouldBe lukket.gyldigTil
            rader.single { it.rad == endretTil }.let {
                it.gyldigFra.shouldNotBeNull()
                it.gyldigTil shouldBe null
            }
            rader.single { it.rad == ny }.gyldigTil shouldBe null

            repo.synkroniserTiltakstypekodeverk(setOf(uendret))

            tac.hentTiltakstypekodeverk().filter { it.gyldigTil == null }.map { it.rad } shouldContainExactlyInAnyOrder listOf(uendret)
        }
    }

    private data class LagretRad(val rad: Tiltakstypekodeverkrad, val gyldigFra: Instant?, val gyldigTil: Instant?, val opprettet: Instant, val sistEndret: Instant)

    private fun TestApplicationContextMedPostgres.hentTiltakstypekodeverk(): List<LagretRad> =
        (sessionFactory as PostgresSessionFactory).withSession { session ->
            session.run(
                sqlQuery("select * from statistikk_kodeverk_tiltakstype").map {
                    LagretRad(
                        rad = Tiltakstypekodeverkrad(
                            eksternKildesystem = it.string("ekstern_kildesystem"),
                            eksternTiltakskode = it.string("ekstern_tiltakskode"),
                            eksternStønadstype = it.string("ekstern_stonadstype"),
                        ),
                        gyldigFra = it.instantOrNull("gyldig_fra"),
                        gyldigTil = it.instantOrNull("gyldig_til"),
                        opprettet = it.instant("opprettet"),
                        sistEndret = it.instant("sist_endret"),
                    )
                }.asList,
            )
        }
}
