package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.gjenopprett

import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.ktor.server.testing.ApplicationTestBuilder
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.libs.common.RammebehandlingId
import no.nav.tiltakspenger.libs.ktor.test.common.ForventetRespons
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContext
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.innvilgelsesperioder
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.avbrytRammebehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.gjenopprettSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.hentSakForSaksnummer
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettForBehandlingId
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.oppdaterSøknadsbehandlingInnvilgelse
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSøknadsbehandlingUnderBehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.sendSøknadsbehandlingTilBeslutningForBehandlingId
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.taRammebehandlinger
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.saksbehandler.SaksbehandlerBehandlingKommandoDTO
import no.nav.tiltakspenger.saksbehandling.søknad.domene.Søknadshendelse
import org.junit.jupiter.api.Test

/**
 * Hele livsløpet til en søknad som ble avsluttet uten vedtak og siden tatt opp igjen.
 * Vi går gjennom endepunktene i den rekkefølgen en saksbehandler ville gjort det: søknaden kommer inn, behandlingen startes og avbrytes, og søknaden gjenopprettes med en ny behandling som kan føres helt frem til vedtak.
 */
class GjenopprettSøknadsbehandlingIT {

    @Test
    fun `søknad - behandling - avbrudd - gjenoppretting - vedtak`() = runTest {
        withTestApplicationContextAndPostgres { tac ->
            val saksbehandler = ObjectMother.saksbehandler()
            val beslutter = ObjectMother.beslutter()
            val innvilgelsesperioder = innvilgelsesperioder()
            val tiltaksdeltakelse = innvilgelsesperioder.valgteTiltaksdeltagelser.verdier.distinct().single()

            // 1. Søknaden sendes inn, og saksbehandler tar behandlingen.
            val (sak, søknad, førsteBehandling) = opprettSøknadsbehandlingUnderBehandling(
                tac = tac,
                saksbehandler = saksbehandler,
                tiltaksdeltakelse = tiltaksdeltakelse,
            )
            sak.søknader.single().id shouldBe søknad.id
            søknad.erAvbrutt shouldBe false
            førsteBehandling.status shouldBe Rammebehandlingsstatus.UNDER_BEHANDLING

            // 2. Saksbehandler avbryter behandlingen, og søknaden avbrytes sammen med den.
            val (sakEtterAvbrudd, avbruttSøknad, avbruttBehandling) = avbrytRammebehandling(
                tac = tac,
                saksnummer = sak.saksnummer,
                sakId = sak.id,
                rammebehandlingId = førsteBehandling.id,
                saksbehandler = saksbehandler,
            )!!
            avbruttSøknad.erAvbrutt shouldBe true
            avbruttBehandling!!.status shouldBe Rammebehandlingsstatus.AVBRUTT
            sakEtterAvbrudd.rammebehandlinger.size shouldBe 1

            // Saksbehandler ser gjenopprett-knappen på den avbrutte behandlingen.
            gyldigeKommandoer(tac, sakEtterAvbrudd, avbruttBehandling.id) shouldContain
                SaksbehandlerBehandlingKommandoDTO.Gjenopprett.name

            // 3. Søknaden gjenopprettes, og vi får en ny behandling å jobbe videre med.
            val (sakEtterGjenoppretting, nyBehandling) = gjenopprettSøknadsbehandling(
                tac = tac,
                sakId = sak.id,
                avbruttBehandlingId = avbruttBehandling.id,
                saksbehandler = saksbehandler,
            )!!
            nyBehandling.id shouldNotBe avbruttBehandling.id
            nyBehandling.status shouldBe Rammebehandlingsstatus.UNDER_BEHANDLING
            nyBehandling.saksbehandler shouldBe saksbehandler.navIdent
            nyBehandling.søknad.id shouldBe søknad.id

            val gjenopprettetSøknad = sakEtterGjenoppretting.søknader.single()
            gjenopprettetSøknad.erAvbrutt shouldBe false
            (gjenopprettetSøknad.avbrutt.last() as Søknadshendelse.Gjenopprettet).begrunnelse!!.value shouldBe
                "søknaden ble avbrutt ved en feil"

            // Den avbrutte behandlingen står urørt ved siden av den nye.
            sakEtterGjenoppretting.rammebehandlinger.size shouldBe 2
            sakEtterGjenoppretting.hentRammebehandling(avbruttBehandling.id)!!.status shouldBe
                Rammebehandlingsstatus.AVBRUTT

            // 4. Den gjenopprettede behandlingen er en helt vanlig søknadsbehandling, og kan føres frem til vedtak.
            oppdaterSøknadsbehandlingInnvilgelse(
                tac = tac,
                sakId = sak.id,
                behandlingId = nyBehandling.id,
                saksbehandler = saksbehandler,
                innvilgelsesperioder = innvilgelsesperioder,
            )
            sendSøknadsbehandlingTilBeslutningForBehandlingId(
                tac = tac,
                sakId = sak.id,
                behandlingId = nyBehandling.id,
                saksbehandler = saksbehandler,
            )!!
            taRammebehandlinger(tac, behandlinger = listOf(Pair(sak.id, nyBehandling.id)), beslutter)!!
            val (sakEtterVedtak, rammevedtak) = iverksettForBehandlingId(
                tac = tac,
                sakId = sak.id,
                behandlingId = nyBehandling.id,
                beslutter = beslutter,
            )!!

            rammevedtak.behandlingId shouldBe nyBehandling.id
            sakEtterVedtak.hentRammebehandling(nyBehandling.id)!!.status shouldBe Rammebehandlingsstatus.VEDTATT
            sakEtterVedtak.hentRammebehandling(avbruttBehandling.id)!!.status shouldBe Rammebehandlingsstatus.AVBRUTT
            sakEtterVedtak.søknader.single().erAvbrutt shouldBe false
        }
    }

    @Test
    fun `kan ikke gjenopprette en søknad som allerede har en aktiv behandling`() = runTest {
        withTestApplicationContextAndPostgres { tac ->
            val saksbehandler = ObjectMother.saksbehandler()
            val (sak, søknad, førsteBehandling) = opprettSøknadsbehandlingUnderBehandling(
                tac = tac,
                saksbehandler = saksbehandler,
            )
            val (_, _, avbruttBehandling) = avbrytRammebehandling(
                tac = tac,
                saksnummer = sak.saksnummer,
                sakId = sak.id,
                rammebehandlingId = førsteBehandling.id,
                saksbehandler = saksbehandler,
            )!!

            // Første gjenoppretting gir søknaden en behandling som lever.
            val (_, aktivBehandling) = gjenopprettSøknadsbehandling(
                tac = tac,
                sakId = sak.id,
                avbruttBehandlingId = avbruttBehandling!!.id,
                saksbehandler = saksbehandler,
            )!!
            aktivBehandling.status shouldBe Rammebehandlingsstatus.UNDER_BEHANDLING
            aktivBehandling.søknad.id shouldBe søknad.id

            // Et nytt forsøk på å ta opp igjen den samme søknaden avvises, og lager ingen ny behandling.
            gjenopprettSøknadsbehandling(
                tac = tac,
                sakId = sak.id,
                avbruttBehandlingId = avbruttBehandling.id,
                saksbehandler = saksbehandler,
                forventet = ForventetRespons(409, contentType = "application/json; charset=UTF-8"),
            ) shouldBe null

            val sakEtterAvvistGjenoppretting = tac.sakContext.sakRepo.hentForSakId(sak.id)!!
            sakEtterAvvistGjenoppretting.rammebehandlinger.size shouldBe 2
            sakEtterAvvistGjenoppretting.søknader.single().avbrutt.toList()
                .filterIsInstance<Søknadshendelse.Gjenopprettet>().size shouldBe 1
        }
    }

    @Test
    fun `en behandling som ikke er avbrutt tilbyr ikke gjenoppretting som handling`() = runTest {
        withTestApplicationContextAndPostgres { tac ->
            val saksbehandler = ObjectMother.saksbehandler()
            val (sak, _, behandling) = opprettSøknadsbehandlingUnderBehandling(
                tac = tac,
                saksbehandler = saksbehandler,
            )

            // En behandling som ikke er avbrutt har ingenting å gjenopprette.
            gyldigeKommandoer(tac, sak, behandling.id) shouldNotContain
                SaksbehandlerBehandlingKommandoDTO.Gjenopprett.name

            gjenopprettSøknadsbehandling(
                tac = tac,
                sakId = sak.id,
                avbruttBehandlingId = behandling.id,
                saksbehandler = saksbehandler,
                forventet = ForventetRespons(400, contentType = "application/json; charset=UTF-8"),
            ) shouldBe null
        }
    }
}

/**
 * Kommandoene saksbehandler faktisk får servert for en behandling, hentet gjennom sak-endepunktet.
 */
private suspend fun ApplicationTestBuilder.gyldigeKommandoer(
    tac: TestApplicationContext,
    sak: Sak,
    behandlingId: RammebehandlingId,
): List<String> {
    val sakJson = hentSakForSaksnummer(tac, sak.saksnummer)!!
    val rammebehandlinger = sakJson.getJSONArray("rammebehandlinger")
    val behandlingJson = (0 until rammebehandlinger.length())
        .map { rammebehandlinger.getJSONObject(it) }
        .single { it.getString("id") == behandlingId.toString() }
    val kommandoer = behandlingJson.getJSONArray("gyldigeKommandoer")
    return (0 until kommandoer.length()).map { kommandoer.getString(it) }
}
