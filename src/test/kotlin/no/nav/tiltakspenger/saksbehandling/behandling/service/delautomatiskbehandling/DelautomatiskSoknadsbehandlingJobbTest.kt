package no.nav.tiltakspenger.saksbehandling.behandling.service.delautomatiskbehandling

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.tiltakspenger.libs.common.TikkendeKlokke
import no.nav.tiltakspenger.libs.common.fixedClockAt
import no.nav.tiltakspenger.libs.dato.juni
import no.nav.tiltakspenger.libs.dato.mai
import no.nav.tiltakspenger.libs.periode.til
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Søknadsbehandling
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContext
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSakOgSøknad
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSøknadsbehandlingOgAvbryt
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSøknadsbehandlingUnderAutomatiskBehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSøknadsbehandlingUnderBehandling
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltakDeltakerstatus
import org.junit.jupiter.api.Test

/**
 * Tilstanden bygges gjennom prodstiene: søknaden kommer inn på søknadsruta, og jobben kjøres slik den kjøres i nais.
 * Tjenestene jobben kaller er de ekte, ikke mocks, slik at testene sier noe om utfallet og ikke bare om hvilke kall som ble gjort.
 */
class DelautomatiskSoknadsbehandlingJobbTest {

    @Test
    fun `opprettSøknadsbehandlingForSøknad - åpen søknad uten behandling - oppretter automatisk behandling`() {
        withTestApplicationContextAndPostgres { tac ->
            val (sak, søknad) = opprettSakOgSøknad(tac = tac)

            tac.delautomatiskSøknadsbehandlingJobb.opprettSøknadsbehandlingForSøknad(søknad.id)

            val rammebehandlinger = tac.sakContext.sakRepo.hentForSakId(sak.id)!!.rammebehandlinger
            rammebehandlinger.size shouldBe 1
            val behandling = rammebehandlinger.single()
            behandling.shouldBeInstanceOf<Søknadsbehandling>()
            behandling.søknad.id shouldBe søknad.id
            behandling.status shouldBe Rammebehandlingsstatus.UNDER_AUTOMATISK_BEHANDLING
        }
    }

    /**
     * En avbrutt søknad uten behandling finnes ikke i prod: søknaden avbrytes gjennom avbryt-ruta, som krever en behandling å avbryte.
     * Vi bygger derfor tilstanden slik den faktisk oppstår - behandling og søknad avbrytes sammen - og sjekker at jobben lar den ligge.
     */
    @Test
    fun `opprettSøknadsbehandlingForSøknad - avbrutt søknad - oppretter ikke behandling`() {
        withTestApplicationContextAndPostgres { tac ->
            val (sak, søknad) = opprettSøknadsbehandlingOgAvbryt(tac = tac)!!

            tac.delautomatiskSøknadsbehandlingJobb.opprettSøknadsbehandlingForSøknad(søknad.id)

            val rammebehandlinger = tac.sakContext.sakRepo.hentForSakId(sak.id)!!.rammebehandlinger
            rammebehandlinger.size shouldBe 1
            rammebehandlinger.single().status shouldBe Rammebehandlingsstatus.AVBRUTT
        }
    }

    @Test
    fun `opprettSøknadsbehandlingForSøknad - søknad med åpen behandling - oppretter ikke ny behandling`() {
        withTestApplicationContextAndPostgres { tac ->
            val (sak, søknad, behandling) = opprettSøknadsbehandlingUnderAutomatiskBehandling(tac = tac)

            tac.delautomatiskSøknadsbehandlingJobb.opprettSøknadsbehandlingForSøknad(søknad.id)

            val rammebehandlinger = tac.sakContext.sakRepo.hentForSakId(sak.id)!!.rammebehandlinger
            rammebehandlinger.size shouldBe 1
            rammebehandlinger.single().id shouldBe behandling.id
        }
    }

    @Test
    fun `automatiskBehandleSøknadsbehandling - behandling under automatisk behandling - behandles ferdig`() {
        withTestApplicationContextAndPostgres { tac ->
            val (_, _, behandling) = opprettSøknadsbehandlingUnderAutomatiskBehandling(tac = tac)

            tac.delautomatiskSøknadsbehandlingJobb.automatiskBehandleSøknadsbehandling(behandling.id)

            val behandlet = tac.behandlingContext.rammebehandlingRepo.hent(behandling.id)
            behandlet.status shouldBe Rammebehandlingsstatus.KLAR_TIL_BESLUTNING
            behandlet.saksbehandler shouldBe AUTOMATISK_SAKSBEHANDLER.navIdent
        }
    }

    @Test
    fun `automatiskBehandleSøknadsbehandling - behandling under manuell behandling - hoppes over`() {
        withTestApplicationContextAndPostgres { tac ->
            val (_, _, behandling) = opprettSøknadsbehandlingUnderBehandling(tac = tac)

            tac.delautomatiskSøknadsbehandlingJobb.automatiskBehandleSøknadsbehandling(behandling.id)

            val uendret = tac.behandlingContext.rammebehandlingRepo.hent(behandling.id)
            uendret.status shouldBe Rammebehandlingsstatus.UNDER_BEHANDLING
            uendret.saksbehandler shouldBe behandling.saksbehandler
        }
    }

    /**
     * Tiltaket har ikke startet, så første kjøring setter behandlingen på vent til startdatoen.
     * Andre kjøring skal la den ligge, siden `venterTil` ikke er passert.
     */
    @Test
    fun `automatiskBehandleSøknadsbehandling - venter til er ikke passert - hoppes over`() {
        val klokke = TikkendeKlokke(fixedClockAt(1.mai(2025)))
        withTestApplicationContextAndPostgres(clock = klokke) { tac ->
            val (_, _, behandling) = opprettSøknadsbehandlingUnderAutomatiskBehandling(
                tac = tac,
                tiltaksdeltakelse = tac.tiltaksdeltakelseSomIkkeHarStartet(),
            )

            tac.delautomatiskSøknadsbehandlingJobb.automatiskBehandleSøknadsbehandling(behandling.id)

            val påVent = tac.behandlingContext.rammebehandlingRepo.hent(behandling.id)
            påVent.status shouldBe Rammebehandlingsstatus.UNDER_AUTOMATISK_BEHANDLING
            påVent.ventestatus.erSattPåVent shouldBe true
            påVent.venterTil shouldBe 1.juni(2025).atTime(6, 0)

            tac.delautomatiskSøknadsbehandlingJobb.automatiskBehandleSøknadsbehandling(behandling.id)

            val fortsattPåVent = tac.behandlingContext.rammebehandlingRepo.hent(behandling.id)
            fortsattPåVent.status shouldBe Rammebehandlingsstatus.UNDER_AUTOMATISK_BEHANDLING
            fortsattPåVent.ventestatus.erSattPåVent shouldBe true
            fortsattPåVent.sistEndret shouldBe påVent.sistEndret
        }
    }

    @Test
    fun `automatiskBehandleSøknadsbehandling - venter til er passert - oppdaterer saksopplysninger og behandler`() {
        val klokke = TikkendeKlokke(fixedClockAt(1.mai(2025)))
        withTestApplicationContextAndPostgres(clock = klokke) { tac ->
            val (_, _, behandling) = opprettSøknadsbehandlingUnderAutomatiskBehandling(
                tac = tac,
                tiltaksdeltakelse = tac.tiltaksdeltakelseSomIkkeHarStartet(),
            )
            tac.delautomatiskSøknadsbehandlingJobb.automatiskBehandleSøknadsbehandling(behandling.id)
            val påVent = tac.behandlingContext.rammebehandlingRepo.hent(behandling.id)
            påVent.ventestatus.erSattPåVent shouldBe true

            klokke.spolTil(2.juni(2025))

            tac.delautomatiskSøknadsbehandlingJobb.automatiskBehandleSøknadsbehandling(behandling.id)

            val gjenopptatt = tac.behandlingContext.rammebehandlingRepo.hent(behandling.id)
            gjenopptatt.ventestatus.erSattPåVent shouldBe false
            gjenopptatt.status shouldNotBe Rammebehandlingsstatus.UNDER_AUTOMATISK_BEHANDLING
        }
    }
}

/**
 * Tiltaksdeltakelse som starter etter klokka i testen, slik at [DelautomatiskBehandlingService] setter behandlingen på vent.
 */
private fun TestApplicationContext.tiltaksdeltakelseSomIkkeHarStartet() =
    tiltaksdeltakelse(
        periode = 1.juni(2025) til 30.juni(2025),
        status = TiltakDeltakerstatus.VenterPåOppstart,
    )
