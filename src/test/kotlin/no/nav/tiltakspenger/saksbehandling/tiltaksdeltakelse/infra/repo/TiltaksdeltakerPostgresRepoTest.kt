package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.repo

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import kotliquery.queryOf
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.hentEllerOpprettSakForSystembruker
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.mottaSøknad
import org.junit.jupiter.api.Test
import org.postgresql.util.PSQLException

class TiltaksdeltakerPostgresRepoTest {

    /**
     * Søknadsruta kaller `hentEllerLagre`, som oppretter tiltaksdeltakeren første gang vi ser den eksterne IDen.
     * Route-byggerne registrerer den vanligvis på forhånd for å kontrollere den interne IDen, så innsettingsgrenen nås bare når vi lar være.
     */
    @Test
    fun `mottak av søknad oppretter tiltaksdeltakeren når den eksterne IDen er ukjent`() {
        withTestApplicationContextAndPostgres { tac ->
            val fnr = ObjectMother.gyldigFnr()
            val saksnummer = hentEllerOpprettSakForSystembruker(tac = tac, fnr = fnr)
            val tiltaksdeltakelse = tac.tiltaksdeltakelse()
            val repo = tac.tiltakContext.tiltaksdeltakerRepo

            repo.hentInternId(tiltaksdeltakelse.eksternDeltakelseId) shouldBe null

            mottaSøknad(
                tac = tac,
                fnr = fnr,
                saksnummer = saksnummer,
                tiltaksdeltakelse = tiltaksdeltakelse,
            )

            val internId = repo.hentInternId(tiltaksdeltakelse.eksternDeltakelseId)
            internId shouldNotBe null
            repo.hentEksternId(internId!!, null) shouldBe tiltaksdeltakelse.eksternDeltakelseId
        }
    }

    /**
     * `hentEksternId` bruker `!!` fordi ingen prodsti sletter fra `tiltaksdeltaker`.
     * `søknadstiltak_tiltaksdeltaker_id_fkey` er den andre halvdelen av garantien, og en constraint er ikke sterkere enn migreringen som holder den i live.
     */
    @Test
    fun `en tiltaksdeltaker kan ikke slettes mens en søknad peker på den`() {
        withTestApplicationContextAndPostgres { tac ->
            val (sak) = iverksettSøknadsbehandling(tac = tac)
            val internId = sak.søknader.single().tiltak!!.tiltaksdeltakerId

            val forsøk = shouldThrow<PSQLException> {
                tac.sessionFactory.withSession { session ->
                    session.run(
                        queryOf(
                            "delete from tiltaksdeltaker where id = :id",
                            mapOf("id" to internId.toString()),
                        ).asUpdate,
                    )
                }
            }

            forsøk.message shouldContain "søknadstiltak_tiltaksdeltaker_id_fkey"
        }
    }
}
