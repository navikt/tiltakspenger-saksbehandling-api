package no.nav.tiltakspenger.saksbehandling.person.personhendelser

import arrow.core.left
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.person.pdl.leesah.adressebeskyttelse.Adressebeskyttelse
import no.nav.person.pdl.leesah.adressebeskyttelse.Gradering
import no.nav.person.pdl.leesah.doedsfall.Doedsfall
import no.nav.person.pdl.leesah.forelderbarnrelasjon.ForelderBarnRelasjon
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.person.infra.http.PersonFakeKlient
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSakOgSøknad
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSøknadsbehandlingUnderBehandling
import no.nav.tiltakspenger.saksbehandling.statistikk.hentSaksstatistikk
import org.junit.jupiter.api.Test
import java.time.LocalDate

/**
 * Tilstanden bygges gjennom prodstiene: saken opprettes via routene, og hendelser med observerbart utfall går inn via [no.nav.tiltakspenger.saksbehandling.person.personhendelser.kafka.LeesahConsumer].
 * Hendelsene som avvises med en spesifikk grunn asserters på servicens Either direkte — consumeren sluker returverdien, så e2e når ikke den forskjellen.
 */
class PersonhendelseServiceTest {

    @Test
    fun `behandlePersonhendelse - finnes ingen sak - ignorerer`() {
        withTestApplicationContextAndPostgres { tac ->
            val fnr = Fnr.random()

            tac.personhendelseService.behandlePersonhendelse(
                nyPersonhendelse(
                    fnr = fnr,
                    doedsfall = Doedsfall(LocalDate.now(tac.clock).minusDays(1)),
                    clock = tac.clock,
                ),
            ) shouldBe KunneIkkeBehandlePersonhendelse.IngenSakForPersonidenter.left()
        }
    }

    @Test
    fun `behandlePersonhendelse - dødsfall, finnes sak - lagrer`() {
        withTestApplicationContextAndPostgres { tac ->
            val fnr = Fnr.random()
            val (sak, _) = opprettSakOgSøknad(tac = tac, fnr = fnr)
            val personhendelse = nyPersonhendelse(
                fnr = fnr,
                doedsfall = Doedsfall(LocalDate.now(tac.clock).minusDays(1)),
                clock = tac.clock,
            )

            tac.leesahConsumer.consume("key", personhendelse)

            val lagretPersonhendelse = tac.personhendelseRepo.hent(sak.id).single()
            lagretPersonhendelse.fnr shouldBe fnr
            lagretPersonhendelse.hendelseId shouldBe personhendelse.hendelseId
            lagretPersonhendelse.opplysningstype shouldBe Opplysningstype.DOEDSFALL_V1
            lagretPersonhendelse.personhendelseType shouldBe PersonhendelseType.Doedsfall(
                LocalDate.now(tac.clock).minusDays(1),
            )
            lagretPersonhendelse.sakId shouldBe sak.id
        }
    }

    @Test
    fun `behandlePersonhendelse - forelderbarnrelasjon, skal ikke behandles`() {
        withTestApplicationContextAndPostgres { tac ->
            val fnr = Fnr.random()
            val (sak, _) = opprettSakOgSøknad(tac = tac, fnr = fnr)

            tac.personhendelseService.behandlePersonhendelse(
                nyPersonhendelse(
                    fnr = fnr,
                    forelderBarnRelasjon = ForelderBarnRelasjon(ObjectMother.gyldigFnr().verdi, "BARN", "FAR"),
                    clock = tac.clock,
                ),
            ) shouldBe KunneIkkeBehandlePersonhendelse.OpplysningstypeIkkeStøttet.left()

            tac.personhendelseRepo.hent(sak.id) shouldBe emptyList()
        }
    }

    @Test
    fun `behandlePersonhendelse - adressebeskyttelse, finnes sak, adressebeskyttet i PDL - oppdaterer og lagrer`() {
        withTestApplicationContextAndPostgres { tac ->
            val fnr = Fnr.random()
            val (sak, _, _) = opprettSøknadsbehandlingUnderBehandling(tac = tac, fnr = fnr)
            (tac.personContext.personKlient as PersonFakeKlient).leggTilPersonopplysning(
                fnr = fnr,
                personopplysninger = ObjectMother.personopplysningKjedeligFyr(fnr = fnr, strengtFortrolig = true),
            )
            val personhendelse = nyPersonhendelse(
                fnr = fnr,
                adressebeskyttelse = Adressebeskyttelse(Gradering.STRENGT_FORTROLIG),
                clock = tac.clock,
            )

            tac.leesahConsumer.consume("key", personhendelse)

            val lagretPersonhendelse = tac.personhendelseRepo.hent(sak.id).single()
            lagretPersonhendelse.fnr shouldBe fnr
            lagretPersonhendelse.hendelseId shouldBe personhendelse.hendelseId
            lagretPersonhendelse.opplysningstype shouldBe Opplysningstype.ADRESSEBESKYTTELSE_V1
            lagretPersonhendelse.personhendelseType shouldBe PersonhendelseType.Adressebeskyttelse(
                "STRENGT_FORTROLIG",
            )
            lagretPersonhendelse.sakId shouldBe sak.id

            val saksstatistikk = tac.sessionFactory.hentSaksstatistikk(sak.id)
            saksstatistikk.shouldNotBeEmpty()
            saksstatistikk.forEach {
                it.fnr shouldBe fnr.verdi
                it.opprettetAv shouldBe "-5"
                it.saksbehandler shouldBe "-5"
                it.ansvarligBeslutter shouldBe "-5"
            }
        }
    }

    @Test
    fun `behandlePersonhendelse - adressebeskyttelse, finnes sak, ikke adressebeskyttet i PDL - oppdaterer ikke`() {
        withTestApplicationContextAndPostgres { tac ->
            val fnr = Fnr.random()
            val (sak, _, _) = opprettSøknadsbehandlingUnderBehandling(tac = tac, fnr = fnr)
            // Registrerer en person uten gradering, siden fake-klienten ellers utleder gradering av første siffer i fnr-et.
            (tac.personContext.personKlient as PersonFakeKlient).leggTilPersonopplysning(
                fnr = fnr,
                personopplysninger = ObjectMother.personopplysningKjedeligFyr(fnr = fnr),
            )

            tac.personhendelseService.behandlePersonhendelse(
                nyPersonhendelse(
                    fnr = fnr,
                    adressebeskyttelse = Adressebeskyttelse(Gradering.STRENGT_FORTROLIG),
                    clock = tac.clock,
                ),
            ) shouldBe KunneIkkeBehandlePersonhendelse.IkkeKode6IPdl.left()

            tac.personhendelseRepo.hent(sak.id) shouldBe emptyList()
            val saksstatistikk = tac.sessionFactory.hentSaksstatistikk(sak.id)
            saksstatistikk.shouldNotBeEmpty()
            saksstatistikk.forEach {
                it.fnr shouldBe fnr.verdi
                it.opprettetAv shouldNotBe "-5"
                it.saksbehandler shouldNotBe "-5"
                it.ansvarligBeslutter shouldNotBe "-5"
            }
        }
    }

    @Test
    fun `behandlePersonhendelse - ukjent opplysningstype, finnes sak - ignorerer`() {
        withTestApplicationContextAndPostgres { tac ->
            val fnr = Fnr.random()
            val (sak, _) = opprettSakOgSøknad(tac = tac, fnr = fnr)

            tac.personhendelseService.behandlePersonhendelse(
                nyPersonhendelse(
                    fnr = fnr,
                    opplysningstype = "NAVN_V1",
                    clock = tac.clock,
                ),
            ) shouldBe KunneIkkeBehandlePersonhendelse.OpplysningstypeIkkeStøttet.left()

            tac.personhendelseRepo.hent(sak.id) shouldBe emptyList()
        }
    }

    @Test
    fun `behandlePersonhendelse - DOEDSFALL_V1 men doedsfall-felt er null - ignorerer`() {
        withTestApplicationContextAndPostgres { tac ->
            val fnr = Fnr.random()
            val (sak, _) = opprettSakOgSøknad(tac = tac, fnr = fnr)

            // DOEDSFALL_V1 uten doedsfall-payload — fanget av defensiv guard i servicen.
            tac.personhendelseService.behandlePersonhendelse(
                nyPersonhendelse(
                    fnr = fnr,
                    opplysningstype = Opplysningstype.DOEDSFALL_V1.name,
                    clock = tac.clock,
                ),
            ) shouldBe KunneIkkeBehandlePersonhendelse.PayloadMangler.left()

            tac.personhendelseRepo.hent(sak.id) shouldBe emptyList()
        }
    }

    @Test
    fun `behandlePersonhendelse - adressebeskyttelse med gradering FORTROLIG - ignorerer`() {
        withTestApplicationContextAndPostgres { tac ->
            val fnr = Fnr.random()
            val (sak, _) = opprettSakOgSøknad(tac = tac, fnr = fnr)

            // Vi bryr oss kun om STRENGT_FORTROLIG[_UTLAND] (kode 6). FORTROLIG (kode 7) skal ignoreres.
            tac.personhendelseService.behandlePersonhendelse(
                nyPersonhendelse(
                    fnr = fnr,
                    adressebeskyttelse = Adressebeskyttelse(Gradering.FORTROLIG),
                    clock = tac.clock,
                ),
            ) shouldBe KunneIkkeBehandlePersonhendelse.AdressebeskyttelseErIkkeKode6.left()

            tac.personhendelseRepo.hent(sak.id) shouldBe emptyList()
        }
    }

    @Test
    fun `behandlePersonhendelse - ingen av personidentene matcher en sak - ignorerer`() {
        withTestApplicationContextAndPostgres { tac ->
            // Sak finnes, men ikke for fnr-et i hendelsen.
            val sakFnr = Fnr.random()
            val (sak, _) = opprettSakOgSøknad(tac = tac, fnr = sakFnr)
            val ukjentFnr = Fnr.random()

            tac.personhendelseService.behandlePersonhendelse(
                nyPersonhendelse(
                    fnr = ukjentFnr,
                    doedsfall = Doedsfall(LocalDate.now(tac.clock).minusDays(1)),
                    clock = tac.clock,
                ),
            ) shouldBe KunneIkkeBehandlePersonhendelse.IngenSakForPersonidenter.left()

            tac.personhendelseRepo.hent(sak.id) shouldBe emptyList()
        }
    }

    @Test
    fun `behandlePersonhendelse - samme hendelse mottatt to ganger - lagrer kun første`() {
        withTestApplicationContextAndPostgres { tac ->
            val fnr = Fnr.random()
            val (sak, _) = opprettSakOgSøknad(tac = tac, fnr = fnr)
            val personhendelse = nyPersonhendelse(
                fnr = fnr,
                doedsfall = Doedsfall(LocalDate.now(tac.clock).minusDays(1)),
                clock = tac.clock,
            )
            tac.leesahConsumer.consume("key", personhendelse)

            tac.personhendelseService.behandlePersonhendelse(personhendelse) shouldBe
                KunneIkkeBehandlePersonhendelse.HendelseAlleredeLagret.left()

            tac.personhendelseRepo.hent(sak.id).size shouldBe 1
        }
    }
}
