package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.gjenopprett

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.libs.ktor.test.common.ForventetRespons
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.gjenopprettSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSøknadsbehandlingOgAvbryt
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSøknadsbehandlingUnderBehandling
import no.nav.tiltakspenger.saksbehandling.søknad.domene.Søknadshendelse
import org.junit.jupiter.api.Test

/**
 * Gjenoppretting av en søknad som ble avsluttet uten vedtak.
 * Saksbehandler peker på den avbrutte søknadsbehandlingen, men det er søknaden som tas opp igjen - den avbrutte behandlingen står urørt og erstattes av en ny.
 */
class GjenopprettSøknadsbehandlingRouteTest {

    @Test
    fun `gjenoppretter søknaden og oppretter en ny søknadsbehandling`() = runTest {
        withTestApplicationContextAndPostgres { tac ->
            val (sak, søknad, avbruttBehandling) = opprettSøknadsbehandlingOgAvbryt(tac = tac)!!
            søknad.erAvbrutt shouldBe true
            avbruttBehandling!!.status shouldBe Rammebehandlingsstatus.AVBRUTT

            val (oppdatertSak, nyBehandling) = gjenopprettSøknadsbehandling(
                tac = tac,
                sakId = sak.id,
                avbruttBehandlingId = avbruttBehandling.id,
            )!!

            val gjenopprettetSøknad = oppdatertSak.søknader.single()
            gjenopprettetSøknad.id shouldBe søknad.id
            gjenopprettetSøknad.avbrutt.toList().map { it::class } shouldContainExactly listOf(
                Søknadshendelse.Avbrutt::class,
                Søknadshendelse.Gjenopprettet::class,
            )
            (gjenopprettetSøknad.avbrutt.last() as Søknadshendelse.Gjenopprettet).begrunnelse!!.value shouldBe
                "søknaden ble avbrutt ved en feil"

            nyBehandling.status shouldBe Rammebehandlingsstatus.UNDER_BEHANDLING
            nyBehandling.søknad.id shouldBe søknad.id
            nyBehandling.søknad.erAvbrutt shouldBe false

            // Den avbrutte behandlingen står urørt.
            oppdatertSak.hentRammebehandling(avbruttBehandling.id)!!.status shouldBe Rammebehandlingsstatus.AVBRUTT
        }
    }

    @Test
    fun `søknaden er plukkbar for automatisk behandling igjen etter gjenoppretting`() = runTest {
        withTestApplicationContextAndPostgres { tac ->
            val (sak, søknad, avbruttBehandling) = opprettSøknadsbehandlingOgAvbryt(tac = tac)!!
            tac.søknadContext.søknadRepo.hentUbehandletSøknad(søknad.id) shouldBe null

            gjenopprettSøknadsbehandling(
                tac = tac,
                sakId = sak.id,
                avbruttBehandlingId = avbruttBehandling!!.id,
            )!!

            // Søknaden har fått en ny behandling, så den er fortsatt ikke ubehandlet - men avbruddet er borte.
            tac.sakContext.sakRepo.hentForSakId(sak.id)!!.søknader.single().erAvbrutt shouldBe false
        }
    }

    @Test
    fun `kan ikke gjenopprette en behandling som ikke er avbrutt`() = runTest {
        withTestApplicationContextAndPostgres { tac ->
            val (sak, _, søknadsbehandling) = opprettSøknadsbehandlingUnderBehandling(tac = tac)

            gjenopprettSøknadsbehandling(
                tac = tac,
                sakId = sak.id,
                avbruttBehandlingId = søknadsbehandling.id,
                forventet = ForventetRespons(400, contentType = "application/json; charset=UTF-8"),
            ) shouldBe null
        }
    }

    @Test
    fun `må være saksbehandler for å gjenopprette`() = runTest {
        withTestApplicationContextAndPostgres { tac ->
            val (sak, _, avbruttBehandling) = opprettSøknadsbehandlingOgAvbryt(tac = tac)!!

            gjenopprettSøknadsbehandling(
                tac = tac,
                sakId = sak.id,
                avbruttBehandlingId = avbruttBehandling!!.id,
                saksbehandler = ObjectMother.beslutter(),
                forventet = ForventetRespons(403, contentType = "application/json; charset=UTF-8"),
            ) shouldBe null
        }
    }

    @Test
    fun `kan ikke gjenopprette to ganger - den nye behandlingen lever`() = runTest {
        withTestApplicationContextAndPostgres { tac ->
            val (sak, _, avbruttBehandling) = opprettSøknadsbehandlingOgAvbryt(tac = tac)!!

            gjenopprettSøknadsbehandling(
                tac = tac,
                sakId = sak.id,
                avbruttBehandlingId = avbruttBehandling!!.id,
            )!!

            gjenopprettSøknadsbehandling(
                tac = tac,
                sakId = sak.id,
                avbruttBehandlingId = avbruttBehandling.id,
                forventet = ForventetRespons(409, contentType = "application/json; charset=UTF-8"),
            ) shouldBe null
        }
    }

    @Test
    fun `gjenoppretting uten begrunnelse gir en hendelse uten begrunnelse`() = runTest {
        withTestApplicationContextAndPostgres { tac ->
            val (sak, søknad, avbruttBehandling) = opprettSøknadsbehandlingOgAvbryt(tac = tac)!!

            val (oppdatertSak, _) = gjenopprettSøknadsbehandling(
                tac = tac,
                sakId = sak.id,
                avbruttBehandlingId = avbruttBehandling!!.id,
                begrunnelse = null,
            )!!

            val hendelse = oppdatertSak.søknader.single { it.id == søknad.id }.avbrutt.last()
            (hendelse as Søknadshendelse.Gjenopprettet).begrunnelse shouldBe null
        }
    }
}
