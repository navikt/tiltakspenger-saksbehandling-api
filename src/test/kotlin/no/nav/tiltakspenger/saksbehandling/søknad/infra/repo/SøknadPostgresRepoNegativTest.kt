package no.nav.tiltakspenger.saksbehandling.søknad.infra.repo

import io.kotest.assertions.throwables.shouldThrowWithMessage
import io.kotest.matchers.shouldNotBe
import kotliquery.queryOf
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSøknadsbehandlingOgAvbryt
import org.junit.jupiter.api.Test

/**
 * Negative tester for [SøknadPostgresRepo]: databasetilstander prodkoden ikke kan skrive.
 *
 * Testene muterer databasen direkte, og det er selve poenget.
 * Dette er unntak (a) i testtaksonomien, jf. `AGENTS.md`.
 */
class SøknadPostgresRepoNegativTest {

    /**
     * `lagreAvbruttSøknad` sjekker at oppdateringen faktisk traff en rad.
     * Prodstien leser søknaden i samme transaksjon som den avbryter den, så raden er der — vakten finnes for en framtidig kaller som ikke har gjort det.
     * Tilstanden bygges derfor ved å fjerne raden under føttene på en søknad som allerede er avbrutt.
     */
    @Test
    fun `kaster når søknaden som skal avbrytes ikke finnes`() {
        withTestApplicationContextAndPostgres { tac ->
            val (sak) = opprettSøknadsbehandlingOgAvbryt(tac = tac)!!
            val avbruttSøknad = tac.sakContext.sakRepo.hentForSakId(sak.id)!!.søknader.single()
            avbruttSøknad.avbrutt shouldNotBe null

            // Behandlingen, tiltaket og barnetilleggene har foreign keys til søknaden, så de må slippe taket før raden kan fjernes.
            tac.sessionFactory.withSession { session ->
                val søknadId = mapOf("id" to avbruttSøknad.id.toString())
                session.run(queryOf("update behandling set soknad_id = null where soknad_id = :id", søknadId).asUpdate)
                session.run(queryOf("delete from søknadstiltak where søknad_id = :id", søknadId).asUpdate)
                session.run(queryOf("delete from søknad_barnetillegg where søknad_id = :id", søknadId).asUpdate)
                session.run(queryOf("delete from søknad where id = :id", søknadId).asUpdate)
            }

            shouldThrowWithMessage<RuntimeException>("Kunne ikke lagre avbrutt søknad.") {
                tac.søknadContext.søknadRepo.lagreAvbruttSøknad(avbruttSøknad, null)
            }
        }
    }
}
