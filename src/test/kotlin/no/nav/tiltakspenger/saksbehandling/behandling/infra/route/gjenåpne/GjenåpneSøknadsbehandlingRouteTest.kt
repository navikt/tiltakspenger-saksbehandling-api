package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.gjenåpne

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.libs.ktor.test.common.ForventetRespons
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.gjenåpneSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSøknadsbehandlingOgAvbryt
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSøknadsbehandlingUnderBehandling
import no.nav.tiltakspenger.saksbehandling.søknad.domene.Søknadshendelse
import org.junit.jupiter.api.Test

/**
 * Gjenåpning av en søknad som ble avsluttet.
 * Saksbehandler peker på den avbrutte søknadsbehandlingen, men det er søknaden som tas opp igjen - den avbrutte behandlingen står urørt og erstattes av en ny.
 */
class GjenåpneSøknadsbehandlingRouteTest {

    @Test
    fun `gjenåpner søknaden og oppretter en ny søknadsbehandling`() = runTest {
        withTestApplicationContextAndPostgres { tac ->
            val (sak, søknad, avbruttBehandling) = opprettSøknadsbehandlingOgAvbryt(tac = tac)!!
            søknad.erAvbrutt shouldBe true
            avbruttBehandling!!.status shouldBe Rammebehandlingsstatus.AVBRUTT

            val (oppdatertSak, nyBehandling) = gjenåpneSøknadsbehandling(
                tac = tac,
                sakId = sak.id,
                avbruttBehandlingId = avbruttBehandling.id,
            )!!

            val gjenåpnetSøknad = oppdatertSak.søknader.single()
            gjenåpnetSøknad.id shouldBe søknad.id
            gjenåpnetSøknad.avbrutt.toList().map { it::class } shouldContainExactly listOf(
                Søknadshendelse.Avbrutt::class,
                Søknadshendelse.Gjenåpnet::class,
            )
            (gjenåpnetSøknad.avbrutt.last() as Søknadshendelse.Gjenåpnet).begrunnelse!!.value shouldBe
                "søknaden ble avbrutt ved en feil"

            nyBehandling.status shouldBe Rammebehandlingsstatus.UNDER_BEHANDLING
            nyBehandling.søknad.id shouldBe søknad.id
            nyBehandling.søknad.erAvbrutt shouldBe false

            // Den avbrutte behandlingen står urørt.
            oppdatertSak.hentRammebehandling(avbruttBehandling.id)!!.status shouldBe Rammebehandlingsstatus.AVBRUTT
        }
    }

    @Test
    fun `søknaden er plukkbar for automatisk behandling igjen etter gjenåpning`() = runTest {
        withTestApplicationContextAndPostgres { tac ->
            val (sak, søknad, avbruttBehandling) = opprettSøknadsbehandlingOgAvbryt(tac = tac)!!
            tac.søknadContext.søknadRepo.hentUbehandletSøknad(søknad.id) shouldBe null

            gjenåpneSøknadsbehandling(
                tac = tac,
                sakId = sak.id,
                avbruttBehandlingId = avbruttBehandling!!.id,
            )!!

            // Søknaden har fått en ny behandling, så den er fortsatt ikke ubehandlet - men avbruddet er borte.
            tac.sakContext.sakRepo.hentForSakId(sak.id)!!.søknader.single().erAvbrutt shouldBe false
        }
    }

    @Test
    fun `kan ikke gjenåpne en behandling som ikke er avbrutt`() = runTest {
        withTestApplicationContextAndPostgres { tac ->
            val (sak, _, søknadsbehandling) = opprettSøknadsbehandlingUnderBehandling(tac = tac)

            gjenåpneSøknadsbehandling(
                tac = tac,
                sakId = sak.id,
                avbruttBehandlingId = søknadsbehandling.id,
                forventet = ForventetRespons(400, contentType = "application/json; charset=UTF-8"),
            ) shouldBe null
        }
    }

    @Test
    fun `må være saksbehandler for å gjenåpne`() = runTest {
        withTestApplicationContextAndPostgres { tac ->
            val (sak, _, avbruttBehandling) = opprettSøknadsbehandlingOgAvbryt(tac = tac)!!

            gjenåpneSøknadsbehandling(
                tac = tac,
                sakId = sak.id,
                avbruttBehandlingId = avbruttBehandling!!.id,
                saksbehandler = ObjectMother.beslutter(),
                forventet = ForventetRespons(403, contentType = "application/json; charset=UTF-8"),
            ) shouldBe null
        }
    }

    @Test
    fun `kan ikke gjenåpne to ganger - den nye behandlingen lever`() = runTest {
        withTestApplicationContextAndPostgres { tac ->
            val (sak, _, avbruttBehandling) = opprettSøknadsbehandlingOgAvbryt(tac = tac)!!

            gjenåpneSøknadsbehandling(
                tac = tac,
                sakId = sak.id,
                avbruttBehandlingId = avbruttBehandling!!.id,
            )!!

            gjenåpneSøknadsbehandling(
                tac = tac,
                sakId = sak.id,
                avbruttBehandlingId = avbruttBehandling.id,
                forventet = ForventetRespons(409, contentType = "application/json; charset=UTF-8"),
            ) shouldBe null
        }
    }

    @Test
    fun `gjenåpning uten begrunnelse gir en hendelse uten begrunnelse`() = runTest {
        withTestApplicationContextAndPostgres { tac ->
            val (sak, søknad, avbruttBehandling) = opprettSøknadsbehandlingOgAvbryt(tac = tac)!!

            val (oppdatertSak, _) = gjenåpneSøknadsbehandling(
                tac = tac,
                sakId = sak.id,
                avbruttBehandlingId = avbruttBehandling!!.id,
                begrunnelse = null,
            )!!

            val hendelse = oppdatertSak.søknader.single { it.id == søknad.id }.avbrutt.last()
            (hendelse as Søknadshendelse.Gjenåpnet).begrunnelse shouldBe null
        }
    }
}
