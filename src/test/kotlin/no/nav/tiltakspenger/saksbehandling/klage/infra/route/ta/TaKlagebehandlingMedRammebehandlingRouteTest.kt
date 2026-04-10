package no.nav.tiltakspenger.saksbehandling.klage.infra.route.ta

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.common.TikkendeKlokke
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.fixedClockAt
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsstatus
import no.nav.tiltakspenger.saksbehandling.klage.infra.route.shouldBeKlagebehandlingDTO
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.ferdigstiltOppretholdKlagebehandlingMedRammebehandlingLagtTilbake
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgTaKlagebehandlingMedRammebehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.taBehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.taKlagebehandling
import org.junit.jupiter.api.Test

class TaKlagebehandlingMedRammebehandlingRouteTest {
    @Test
    fun `kan ta klagebehandling med rammebehandling`() {
        val clock = TikkendeKlokke(fixedClockAt(1.januar(2025)))
        withTestApplicationContextAndPostgres(clock = clock, runIsolated = true) { tac ->
            val (sak, rammebehandlingMedKlagebehandling, json) = iverksettSøknadsbehandlingOgTaKlagebehandlingMedRammebehandling(
                tac = tac,
            )!!
            val klagebehandling = rammebehandlingMedKlagebehandling.klagebehandling!!
            json.get("klageBehandlinger").first().toString().shouldBeKlagebehandlingDTO(
                sakId = sak.id,
                saksnummer = sak.saksnummer,
                klagebehandlingId = klagebehandling.id,
                fnr = "12345678911",
                saksbehandler = "saksbehandlerSomTarKlagebehandling",
                resultat = "OMGJØR",
                vedtakDetKlagesPå = "${sak.rammevedtaksliste.first().id}",
                behandlingDetKlagesPå = "${sak.rammevedtaksliste.first().behandlingId}",
                kanIverksetteVedtak = null,
                årsak = "PROSESSUELL_FEIL",
                begrunnelse = "Begrunnelse for omgjøring",
                rammebehandlingId = listOf(rammebehandlingMedKlagebehandling.id.toString()),
                åpenRammebehandlingId = rammebehandlingMedKlagebehandling.id.toString(),
            )
            rammebehandlingMedKlagebehandling.status shouldBe Rammebehandlingsstatus.UNDER_BEHANDLING
            rammebehandlingMedKlagebehandling.saksbehandler shouldBe "saksbehandlerSomTarKlagebehandling"
        }
    }

    @Test
    fun `kan ta rammebehandling som er tilknyttet ferdigstilt klagebehandling (fra klagebehandling)`() {
        val clock = TikkendeKlokke(fixedClockAt(1.januar(2025)))
        withTestApplicationContextAndPostgres(clock = clock, runIsolated = true) { tac ->
            val førsteSaksbehandler = ObjectMother.saksbehandler("førsteSaksbehandler")
            val (sak, rammebehandling) = ferdigstiltOppretholdKlagebehandlingMedRammebehandlingLagtTilbake(
                tac = tac,
                type = "REVURDERING_OMGJØRING",
                saksbehandler = førsteSaksbehandler,
            )!!
            val nySaksbehandler = ObjectMother.saksbehandler("saksbehandlerSomTarKlagebehandling")
            val (_, _, json) = taKlagebehandling(
                tac = tac,
                sakId = sak.id,
                klagebehandlingId = rammebehandling.klagebehandling!!.id,
                saksbehandlerSomTar = nySaksbehandler,
            )!!

            val rammebehandlingJson = json.get("behandlinger").last()
            val klagebehandlingJson = json.get("klageBehandlinger").single()

            rammebehandlingJson.get("saksbehandler").asString() shouldBe nySaksbehandler.navIdent
            rammebehandlingJson.get("status").asString() shouldBe "UNDER_BEHANDLING"
            klagebehandlingJson.get("saksbehandler").asString() shouldBe førsteSaksbehandler.navIdent
            klagebehandlingJson.get("status").asString() shouldBe "FERDIGSTILT"
        }
    }

    @Test
    fun `kan ta rammebehandling som er tilknyttet ferdigstilt klagebehandling (fra rammebehandling)`() {
        val clock = TikkendeKlokke(fixedClockAt(1.januar(2025)))
        withTestApplicationContextAndPostgres(clock = clock, runIsolated = true) { tac ->
            val førsteSaksbehandler = ObjectMother.saksbehandler("førsteSaksbehandler")
            val (sak, rammebehandling) = ferdigstiltOppretholdKlagebehandlingMedRammebehandlingLagtTilbake(
                tac = tac,
                type = "REVURDERING_OMGJØRING",
                saksbehandler = førsteSaksbehandler,
            )!!
            val nySaksbehandler = ObjectMother.saksbehandler("saksbehandlerSomTarRammebehandling")
            val (_, tattRammebehandling, rammebehandlingJson) = taBehandling(
                tac = tac,
                sakId = sak.id,
                behandlingId = rammebehandling.id,
                saksbehandler = nySaksbehandler,
            )

            rammebehandlingJson.getString("saksbehandler") shouldBe nySaksbehandler.navIdent
            rammebehandlingJson.getString("status") shouldBe "UNDER_BEHANDLING"
            (tattRammebehandling as Rammebehandling).klagebehandling!!.saksbehandler shouldBe førsteSaksbehandler.navIdent
            tattRammebehandling.klagebehandling!!.status shouldBe Klagebehandlingsstatus.FERDIGSTILT
        }
    }
}
