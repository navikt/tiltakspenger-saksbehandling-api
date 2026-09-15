package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.repo

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import kotliquery.queryOf
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.tiltak.TiltakResponsDTO
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.hentEllerOpprettSakForSystembruker
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.mottaSøknad
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakerId
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

    @Test
    fun `registrerUbehandletEndring setter sakId og tidspunkt, og hentMedUbehandledeEndringer filtrerer på alder`() {
        withTestApplicationContextAndPostgres { tac ->
            val fnr = ObjectMother.gyldigFnr()
            val saksnummer = hentEllerOpprettSakForSystembruker(tac = tac, fnr = fnr)
            val sakId = tac.sakContext.sakRepo.hentForSaksnummer(saksnummer)!!.id
            val repo = tac.tiltakContext.tiltaksdeltakerRepo

            fun lagreDeltaker(eksternId: String): TiltaksdeltakerId {
                val id = TiltaksdeltakerId.random()
                repo.lagre(id = id, eksternId = eksternId, tiltakstype = TiltakResponsDTO.TiltakTypeDTO.GRUPPEAMO, sakId = sakId)
                return id
            }

            val utenMarkør = lagreDeltaker("uten-markør")
            val ferskMarkør = lagreDeltaker("fersk-markør")
            val gammelMarkør = lagreDeltaker("gammel-markør")

            repo.registrerUbehandletEndring(ferskMarkør, sakId, nå(tac.clock))
            repo.registrerUbehandletEndring(gammelMarkør, sakId, nå(tac.clock).minusMinutes(20))

            val kandidater = repo.hentMedUbehandledeEndringer(nå(tac.clock).minusMinutes(15))

            kandidater.map { it.id } shouldBe listOf(gammelMarkør)
            val kandidat = kandidater.single()
            kandidat.sakId shouldBe sakId
            kandidat.sisteUbehandletEndringTidspunkt shouldNotBe null

            // Deltaker uten markør har sakId fra opprettelsen, men ingen ubehandlet endring.
            val ubehandlet = repo.hentTiltaksdeltaker("uten-markør").shouldNotBeNull()
            ubehandlet.sakId shouldBe sakId
            ubehandlet.sisteUbehandletEndringTidspunkt shouldBe null

            // utenMarkør brukes kun til å verifisere utelukkelsen over.
            utenMarkør shouldNotBe gammelMarkør
        }
    }

    @Test
    fun `markerEndringSomBehandlet nullstiller kun dersom markøren er uendret`() {
        withTestApplicationContextAndPostgres { tac ->
            val fnr = ObjectMother.gyldigFnr()
            val saksnummer = hentEllerOpprettSakForSystembruker(tac = tac, fnr = fnr)
            val sakId = tac.sakContext.sakRepo.hentForSaksnummer(saksnummer)!!.id
            val repo = tac.tiltakContext.tiltaksdeltakerRepo

            val id = TiltaksdeltakerId.random()
            repo.lagre(id = id, eksternId = "deltaker-1", tiltakstype = TiltakResponsDTO.TiltakTypeDTO.GRUPPEAMO, sakId = sakId)
            repo.registrerUbehandletEndring(id, sakId, nå(tac.clock).minusMinutes(20))

            // Les tilbake markøren slik den faktisk ble lagret (timestamptz har lavere oppløsning enn LocalDateTime).
            val lagretMarkør = repo.hentTiltaksdeltaker("deltaker-1").shouldNotBeNull().sisteUbehandletEndringTidspunkt.shouldNotBeNull()

            // En markør som ikke lenger stemmer — det har kommet en nyere hendelse underveis — skal ikke nullstilles.
            repo.markerEndringSomBehandlet(id, lagretMarkør.minusSeconds(1))
            repo.hentTiltaksdeltaker("deltaker-1").shouldNotBeNull().sisteUbehandletEndringTidspunkt shouldBe lagretMarkør

            repo.markerEndringSomBehandlet(id, lagretMarkør)
            val behandlet = repo.hentTiltaksdeltaker("deltaker-1").shouldNotBeNull()
            behandlet.sisteUbehandletEndringTidspunkt shouldBe null
            // sakId nullstilles ikke — den gjelder fortsatt deltakeren.
            behandlet.sakId shouldBe sakId
        }
    }
}
