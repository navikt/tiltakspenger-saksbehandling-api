package no.nav.tiltakspenger.saksbehandling.person.personhendelser

import arrow.core.left
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.person.pdl.leesah.adressebeskyttelse.Gradering
import no.nav.person.pdl.leesah.doedsfall.Doedsfall
import no.nav.person.pdl.leesah.forelderbarnrelasjon.ForelderBarnRelasjon
import no.nav.tiltakspenger.libs.common.personopplysning.Fnr
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.infra.setup.Configuration
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.person.Adressebeskyttelse
import no.nav.tiltakspenger.saksbehandling.person.infra.http.PersonFakeKlient
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSakOgSøknad
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSøknadsbehandlingKlarTilBehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSøknadsbehandlingUnderBehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.taRammebehandlinger
import no.nav.tiltakspenger.saksbehandling.saksbehandler.EntraGruppe
import no.nav.tiltakspenger.saksbehandling.statistikk.hentSaksstatistikk
import no.nav.tiltakspenger.saksbehandling.statistikk.saksstatistikk.SKJERMET
import no.nav.tiltakspenger.saksbehandling.statistikk.saksstatistikk.VIKAFOSSEN
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID
import no.nav.person.pdl.leesah.adressebeskyttelse.Adressebeskyttelse as LeesahAdressebeskyttelse

/**
 * Tilstanden bygges gjennom prodstiene: saken opprettes via routene, og hendelser med observerbart utfall går inn via [no.nav.tiltakspenger.saksbehandling.person.personhendelser.kafka.LeesahConsumer].
 * Hendelsene som avvises med en spesifikk grunn asserters på servicens Either direkte — consumeren sluker returverdien, så e2e når ikke den forskjellen.
 */
class PersonhendelseServiceTest {

    @Test
    fun `behandlePersonhendelse - finnes ingen sak - ignorerer`() {
        withTestApplicationContextAndPostgres { tac ->
            val fnr = Fnr.random()

            tac.personhendelseContext.personhendelseService.behandlePersonhendelse(
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

            val lagretPersonhendelse = tac.personhendelseContext.personhendelseRepo.hent(sak.id).single()
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

            tac.personhendelseContext.personhendelseService.behandlePersonhendelse(
                nyPersonhendelse(
                    fnr = fnr,
                    forelderBarnRelasjon = ForelderBarnRelasjon(ObjectMother.gyldigFnr().verdi, "BARN", "FAR"),
                    clock = tac.clock,
                ),
            ) shouldBe KunneIkkeBehandlePersonhendelse.OpplysningstypeIkkeStøttet.left()

            tac.personhendelseContext.personhendelseRepo.hent(sak.id) shouldBe emptyList()
        }
    }

    @Test
    fun `behandlePersonhendelse - adressebeskyttelse, finnes sak, adressebeskyttet i PDL - oppdaterer og lagrer`() {
        withTestApplicationContextAndPostgres { tac ->
            val fnr = Fnr.random()
            val (sak, _, _) = opprettSøknadsbehandlingUnderBehandling(tac = tac, fnr = fnr)
            (tac.personContext.personKlient as PersonFakeKlient).leggTilPersonopplysning(
                fnr = fnr,
                personopplysninger = ObjectMother.personopplysningKjedeligFyr(fnr = fnr, adressebeskyttelse = Adressebeskyttelse.STRENGT_FORTROLIG),
            )
            val personhendelse = nyPersonhendelse(
                fnr = fnr,
                adressebeskyttelse = LeesahAdressebeskyttelse(Gradering.STRENGT_FORTROLIG),
                clock = tac.clock,
            )

            tac.leesahConsumer.consume("key", personhendelse)

            val lagretPersonhendelse = tac.personhendelseContext.personhendelseRepo.hent(sak.id).single()
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
                it.opprettetAv shouldBe SKJERMET
                it.saksbehandler shouldBe SKJERMET
                it.ansvarligBeslutter shouldBe SKJERMET
                it.ansvarligenhet shouldBe SKJERMET
            }
        }
    }

    @Test
    fun `behandlePersonhendelse - adressebeskyttelse, behandlingen ligger hos Vikafossen - beholder enhet og fiktiv ident`() {
        withTestApplicationContextAndPostgres { tac ->
            val fnr = Fnr.random()
            val (sak, _, behandling) = opprettSøknadsbehandlingKlarTilBehandling(tac = tac, fnr = fnr)
            // Brukeren får strengt fortrolig adresse etter at søknaden er mottatt, og behandlingen tas så av en saksbehandler i Vikafossen.
            (tac.personContext.personKlient as PersonFakeKlient).leggTilPersonopplysning(
                fnr = fnr,
                personopplysninger = ObjectMother.personopplysningKjedeligFyr(fnr = fnr, adressebeskyttelse = Adressebeskyttelse.STRENGT_FORTROLIG),
            )
            val vikafossenSaksbehandler = ObjectMother.saksbehandler(navIdent = "Z99999")
            tac.entraProxyFakeClient.leggTil(
                navIdent = vikafossenSaksbehandler.navIdent,
                EntraGruppe(id = UUID.fromString(Configuration.roleStrengtFortroligAdresse), navn = "0000-GA-Strengt_Fortrolig_Adresse"),
            )
            taRammebehandlinger(tac = tac, behandlinger = listOf(sak.id to behandling.id), saksbehandler = vikafossenSaksbehandler)
            val raderFørHendelsen = tac.sessionFactory.hentSaksstatistikk(sak.id).size

            tac.leesahConsumer.consume(
                "key",
                nyPersonhendelse(
                    fnr = fnr,
                    adressebeskyttelse = LeesahAdressebeskyttelse(Gradering.STRENGT_FORTROLIG),
                    clock = tac.clock,
                ),
            )

            tac.personhendelseContext.personhendelseRepo.hent(sak.id).shouldNotBeEmpty()
            val saksstatistikk = tac.sessionFactory.hentSaksstatistikk(sak.id)
            saksstatistikk.size shouldBe raderFørHendelsen
            // Radene fra før saksbehandleren tok behandlingen, er skjermet av oppdateringen; den siste ble skrevet hos Vikafossen og beholdes.
            saksstatistikk.dropLast(1).shouldNotBeEmpty().forEach {
                listOf(it.opprettetAv, it.saksbehandler, it.ansvarligBeslutter, it.ansvarligenhet) shouldBe listOf(SKJERMET, SKJERMET, SKJERMET, SKJERMET)
            }
            saksstatistikk.last().let {
                listOf(it.opprettetAv, it.saksbehandler, it.ansvarligBeslutter, it.ansvarligenhet) shouldBe listOf(SKJERMET, "Z99999", null, VIKAFOSSEN)
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

            tac.personhendelseContext.personhendelseService.behandlePersonhendelse(
                nyPersonhendelse(
                    fnr = fnr,
                    adressebeskyttelse = LeesahAdressebeskyttelse(Gradering.STRENGT_FORTROLIG),
                    clock = tac.clock,
                ),
            ) shouldBe KunneIkkeBehandlePersonhendelse.IkkeKode6IPdl.left()

            tac.personhendelseContext.personhendelseRepo.hent(sak.id) shouldBe emptyList()
            val saksstatistikk = tac.sessionFactory.hentSaksstatistikk(sak.id)
            saksstatistikk.shouldNotBeEmpty()
            saksstatistikk.forEach {
                it.fnr shouldBe fnr.verdi
                it.opprettetAv shouldNotBe SKJERMET
                it.saksbehandler shouldNotBe SKJERMET
                it.ansvarligBeslutter shouldNotBe SKJERMET
            }
        }
    }

    @Test
    fun `behandlePersonhendelse - ukjent opplysningstype, finnes sak - ignorerer`() {
        withTestApplicationContextAndPostgres { tac ->
            val fnr = Fnr.random()
            val (sak, _) = opprettSakOgSøknad(tac = tac, fnr = fnr)

            tac.personhendelseContext.personhendelseService.behandlePersonhendelse(
                nyPersonhendelse(
                    fnr = fnr,
                    opplysningstype = "NAVN_V1",
                    clock = tac.clock,
                ),
            ) shouldBe KunneIkkeBehandlePersonhendelse.OpplysningstypeIkkeStøttet.left()

            tac.personhendelseContext.personhendelseRepo.hent(sak.id) shouldBe emptyList()
        }
    }

    @Test
    fun `behandlePersonhendelse - DOEDSFALL_V1 men doedsfall-felt er null - ignorerer`() {
        withTestApplicationContextAndPostgres { tac ->
            val fnr = Fnr.random()
            val (sak, _) = opprettSakOgSøknad(tac = tac, fnr = fnr)

            // DOEDSFALL_V1 uten doedsfall-payload — fanget av defensiv guard i servicen.
            tac.personhendelseContext.personhendelseService.behandlePersonhendelse(
                nyPersonhendelse(
                    fnr = fnr,
                    opplysningstype = Opplysningstype.DOEDSFALL_V1.name,
                    clock = tac.clock,
                ),
            ) shouldBe KunneIkkeBehandlePersonhendelse.PayloadMangler.left()

            tac.personhendelseContext.personhendelseRepo.hent(sak.id) shouldBe emptyList()
        }
    }

    @Test
    fun `behandlePersonhendelse - adressebeskyttelse med gradering FORTROLIG - ignorerer`() {
        withTestApplicationContextAndPostgres { tac ->
            val fnr = Fnr.random()
            val (sak, _) = opprettSakOgSøknad(tac = tac, fnr = fnr)

            // Vi bryr oss kun om STRENGT_FORTROLIG[_UTLAND] (kode 6). FORTROLIG (kode 7) skal ignoreres.
            tac.personhendelseContext.personhendelseService.behandlePersonhendelse(
                nyPersonhendelse(
                    fnr = fnr,
                    adressebeskyttelse = LeesahAdressebeskyttelse(Gradering.FORTROLIG),
                    clock = tac.clock,
                ),
            ) shouldBe KunneIkkeBehandlePersonhendelse.AdressebeskyttelseErIkkeKode6.left()

            tac.personhendelseContext.personhendelseRepo.hent(sak.id) shouldBe emptyList()
        }
    }

    @Test
    fun `behandlePersonhendelse - ingen av personidentene matcher en sak - ignorerer`() {
        withTestApplicationContextAndPostgres { tac ->
            // Sak finnes, men ikke for fnr-et i hendelsen.
            val sakFnr = Fnr.random()
            val (sak, _) = opprettSakOgSøknad(tac = tac, fnr = sakFnr)
            val ukjentFnr = Fnr.random()

            tac.personhendelseContext.personhendelseService.behandlePersonhendelse(
                nyPersonhendelse(
                    fnr = ukjentFnr,
                    doedsfall = Doedsfall(LocalDate.now(tac.clock).minusDays(1)),
                    clock = tac.clock,
                ),
            ) shouldBe KunneIkkeBehandlePersonhendelse.IngenSakForPersonidenter.left()

            tac.personhendelseContext.personhendelseRepo.hent(sak.id) shouldBe emptyList()
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

            tac.personhendelseContext.personhendelseService.behandlePersonhendelse(personhendelse) shouldBe
                KunneIkkeBehandlePersonhendelse.HendelseAlleredeLagret.left()

            tac.personhendelseContext.personhendelseRepo.hent(sak.id).size shouldBe 1
        }
    }
}
