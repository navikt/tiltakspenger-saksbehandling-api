package no.nav.tiltakspenger.saksbehandling.søknad.infra.repo

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.getOrFail
import no.nav.tiltakspenger.saksbehandling.common.IsolatedDatabaseTest
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.avbrytRammebehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.gjenåpneSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSakOgSøknad
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSøknadsbehandlingOgAvbryt
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.startBehandlingAvManueltRegistrertSøknad
import no.nav.tiltakspenger.saksbehandling.søknad.domene.InnvilgbarSøknad
import no.nav.tiltakspenger.saksbehandling.søknad.domene.SøknadshendelseTilSøknadApi
import no.nav.tiltakspenger.saksbehandling.søknad.domene.Søknadstype
import no.nav.tiltakspenger.saksbehandling.søknad.domene.Søknadsutfall
import no.nav.tiltakspenger.saksbehandling.søknad.domene.Søknadsutfallresultat
import org.junit.jupiter.api.Test

/**
 * Aggregat-test for køen av ubehandlede søknader, jf. testtaksonomien i `AGENTS.md`.
 *
 * Spørringen velger ut på tvers av alle saker, så testen bygger flere saker og asserter hele køen, uten å filtrere på `sakId`.
 * Den sorterer eldst først (`order by soknad.opprettet`), og testen asserter den rekkefølgen.
 *
 * To av utvalgskriteriene er ikke dekket her, fordi de ikke lar seg nå gjennom eksisterende route-byggere:
 * `soknad.avbrutt is null` krever en inngang for å avbryte en søknad uten behandling, og `soknadstype = DIGITAL` krever en papirsøknad.
 * Begge kommer inn her når byggerne finnes.
 *
 * Køen av avbrudd og gjenåpninger til søknad-api velger også ut på tvers av saker, og asserteres på samme måte.
 */
class SøknadAggregatTest {

    @Test
    @IsolatedDatabaseTest
    fun `køen tar kun søknader uten behandling, sorterer eldst først, respekterer limit og tømmes når behandlingen opprettes`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            val (_, eldst) = opprettSakOgSøknad(tac = tac)
            val (_, nyest) = opprettSakOgSøknad(tac = tac)

            val repo = tac.søknadContext.søknadRepo

            repo.hentUbehandledeSøknadIder(limit = 10) shouldBe listOf(eldst.id, nyest.id)

            // Limit batcher fra toppen av køen, så den eldste søknaden kommer først og ingen kan sulte.
            repo.hentUbehandledeSøknadIder(limit = 1) shouldBe listOf(eldst.id)

            tac.behandlingContext.startSøknadsbehandlingService.opprettAutomatiskSoknadsbehandling(
                soknad = eldst as InnvilgbarSøknad,
                correlationId = CorrelationId.generate(),
            ).getOrFail()

            // Søknaden forlater køen så snart den har fått en behandling.
            repo.hentUbehandledeSøknadIder(limit = 10) shouldBe listOf(nyest.id)

            tac.behandlingContext.startSøknadsbehandlingService.opprettAutomatiskSoknadsbehandling(
                soknad = nyest as InnvilgbarSøknad,
                correlationId = CorrelationId.generate(),
            ).getOrFail()

            repo.hentUbehandledeSøknadIder(limit = 10).shouldBeEmpty()
        }
    }

    @Test
    @IsolatedDatabaseTest
    fun `køen av søknadshendelser til søknad-api tar kun digitale søknader, sorterer eldst først og tar inn nye hendelser`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            val (eldstSak, _, eldstBehandling) = opprettSøknadsbehandlingOgAvbryt(tac = tac)!!
            val (nyestSak) = opprettSøknadsbehandlingOgAvbryt(tac = tac)!!

            // En avbrutt papirsøknad skal ikke til søknad-api, som bare kjenner de digitale søknadene.
            val (papirSak) = opprettSakOgSøknad(tac = tac)
            startBehandlingAvManueltRegistrertSøknad(tac = tac, saksnummer = papirSak.saksnummer)
            avbrytRammebehandling(
                tac = tac,
                saksnummer = papirSak.saksnummer,
                sakId = papirSak.id,
                rammebehandlingId = tac.sakContext.sakRepo.hentForSakId(papirSak.id)!!.rammebehandlinger.single().id,
            )

            val repo = tac.søknadContext.søknadRepo

            fun søknad(sakId: SakId) = tac.sakContext.sakRepo.hentForSakId(sakId)!!.søknader.single()

            fun forventet(sakId: SakId, resultat: Søknadsutfallresultat) = søknad(sakId).let {
                SøknadshendelseTilSøknadApi(
                    søknadsutfall = Søknadsutfall(
                        søknadId = it.id,
                        resultat = resultat,
                        tidspunkt = it.avbrutt.last().tidspunkt,
                        vedtakId = null,
                    ),
                    antallHendelser = it.avbrutt.size,
                )
            }

            tac.sakContext.sakRepo.hentForSakId(papirSak.id)!!.søknader
                .single { it.søknadstype == Søknadstype.PAPIR_SKJEMA }.erAvbrutt shouldBe true
            repo.hentSøknadshendelserTilSøknadApi(limit = 10) shouldBe listOf(
                forventet(eldstSak.id, Søknadsutfallresultat.AVBRUTT),
                forventet(nyestSak.id, Søknadsutfallresultat.AVBRUTT),
            )
            repo.hentSøknadshendelserTilSøknadApi(limit = 1) shouldBe listOf(forventet(eldstSak.id, Søknadsutfallresultat.AVBRUTT))

            repo.markerSøknadshendelserSendtTilSøknadApi(søknad(eldstSak.id).id, antallHendelser = 1)
            repo.hentSøknadshendelserTilSøknadApi(limit = 10) shouldBe listOf(forventet(nyestSak.id, Søknadsutfallresultat.AVBRUTT))

            // En ny hendelse på en søknad som allerede er sendt, legger den tilbake i køen.
            gjenåpneSøknadsbehandling(tac = tac, sakId = eldstSak.id, avbruttBehandlingId = eldstBehandling!!.id)
            repo.hentSøknadshendelserTilSøknadApi(limit = 10) shouldBe listOf(
                forventet(eldstSak.id, Søknadsutfallresultat.GJENÅPNET),
                forventet(nyestSak.id, Søknadsutfallresultat.AVBRUTT),
            )

            // En markering med et utdatert antall fjerner ikke den nye hendelsen fra køen.
            repo.markerSøknadshendelserSendtTilSøknadApi(søknad(eldstSak.id).id, antallHendelser = 1)
            repo.hentSøknadshendelserTilSøknadApi(limit = 10).map { it.søknadsutfall.søknadId } shouldBe
                listOf(søknad(eldstSak.id).id, søknad(nyestSak.id).id)

            repo.markerSøknadshendelserSendtTilSøknadApi(søknad(eldstSak.id).id, antallHendelser = 2)
            repo.markerSøknadshendelserSendtTilSøknadApi(søknad(nyestSak.id).id, antallHendelser = 1)
            repo.hentSøknadshendelserTilSøknadApi(limit = 10).shouldBeEmpty()
        }
    }
}
