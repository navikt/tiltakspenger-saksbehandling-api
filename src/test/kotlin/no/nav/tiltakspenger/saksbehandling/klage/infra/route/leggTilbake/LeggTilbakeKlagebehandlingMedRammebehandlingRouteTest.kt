package no.nav.tiltakspenger.saksbehandling.klage.infra.route.leggTilbake

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.common.TikkendeKlokke
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.fixedClockAt
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsresultat
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsstatus
import no.nav.tiltakspenger.saksbehandling.klage.infra.route.shouldBeFerdigstiltOpprettholdtKlagebehandlingDTO
import no.nav.tiltakspenger.saksbehandling.klage.infra.route.shouldBeKlagebehandlingDTO
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.ferdigstillOpprettholdtKlagebehandlingOgOpprettRammebehandlingForKlage
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgLeggKlagebehandlingMedRammebehandlingTilbake
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgOpprettRammebehandlingForKlage
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.leggKlagebehandlingTilbake
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.leggTilbakeRammebehandling
import org.junit.jupiter.api.Test

class LeggTilbakeKlagebehandlingMedRammebehandlingRouteTest {
    @Test
    fun `kan legge klagebehandling med rammebehandling tilbake`() {
        val clock = TikkendeKlokke(fixedClockAt(1.januar(2025)))
        withTestApplicationContextAndPostgres(clock = clock, runIsolated = true) { tac ->
            val (sak, rammebehandlingMedKlagebehandling, json) = iverksettSøknadsbehandlingOgLeggKlagebehandlingMedRammebehandlingTilbake(
                tac = tac,
            )!!
            val klagebehandling = rammebehandlingMedKlagebehandling.klagebehandling!!
            json.get("klageBehandlinger").first().toString().shouldBeKlagebehandlingDTO(
                sakId = sak.id,
                saksnummer = sak.saksnummer,
                klagebehandlingId = klagebehandling.id,
                fnr = "12345678911",
                saksbehandler = null,
                status = "KLAR_TIL_BEHANDLING",
                resultat = "OMGJØR",
                vedtakDetKlagesPå = "${sak.rammevedtaksliste.first().id}",
                behandlingDetKlagesPå = "${sak.rammevedtaksliste.first().behandlingId}",
                årsak = "PROSESSUELL_FEIL",
                begrunnelse = "Begrunnelse for omgjøring",
                rammebehandlingId = listOf(rammebehandlingMedKlagebehandling.id.toString()),
                åpenRammebehandlingId = rammebehandlingMedKlagebehandling.id.toString(),
            )
        }
    }

    @Test
    fun `kan legge rammebehandling med klagebehandling tilbake`() {
        val clock = TikkendeKlokke(fixedClockAt(1.januar(2025)))
        withTestApplicationContextAndPostgres(clock = clock, runIsolated = true) { tac ->
            val saksbehandler = ObjectMother.saksbehandler("saksbehandlerKlagebehandling")
            val (sak, rammebehandlingMedKlagebehandling, _) = iverksettSøknadsbehandlingOgOpprettRammebehandlingForKlage(
                tac = tac,
                saksbehandlerKlagebehandling = saksbehandler,
            )!!
            val (_, oppdatertRammebehandling, json) = this.leggTilbakeRammebehandling(
                tac = tac,
                sakId = sak.id,
                behandlingId = rammebehandlingMedKlagebehandling.id,
                saksbehandler = saksbehandler,
            )
            val klagebehandling = oppdatertRammebehandling.klagebehandling!!
            json.getString("klagebehandlingId")
                .shouldBe(rammebehandlingMedKlagebehandling.klagebehandling!!.id.toString())
            json.getString("status").shouldBe("KLAR_TIL_BEHANDLING")
            json.getString("saksbehandler").shouldBe("null")
            klagebehandling.status shouldBe Klagebehandlingsstatus.KLAR_TIL_BEHANDLING
            klagebehandling.saksbehandler shouldBe null
        }
    }

    @Test
    fun `kan legge rammebehandling med tilknyttet ferdigstilt klage tilbake (fra klagebehandling)`() {
        val clock = TikkendeKlokke(fixedClockAt(1.januar(2025)))
        withTestApplicationContextAndPostgres(clock = clock, runIsolated = true) { tac ->
            val (sak, rammebehandling) = ferdigstillOpprettholdtKlagebehandlingOgOpprettRammebehandlingForKlage(
                tac = tac,
                type = "REVURDERING_OMGJØRING",
            )!!

            val (_, _, sakJson) = leggKlagebehandlingTilbake(
                tac = tac,
                sakId = sak.id,
                klagebehandlingId = rammebehandling.klagebehandling!!.id,
            )!!

            val overtattRammebehandling = sakJson.get("behandlinger").last()
            val klagebehandling = sakJson.get("klageBehandlinger").single()

            overtattRammebehandling.get("saksbehandler").isNull shouldBe true
            overtattRammebehandling.get("status").asString() shouldBe "KLAR_TIL_BEHANDLING"
            klagebehandling.toString().shouldBeFerdigstiltOpprettholdtKlagebehandlingDTO(
                sakId = sak.id,
                fnr = sak.fnr.verdi,
                klagebehandlingId = rammebehandling.klagebehandling!!.id,
                resultat = rammebehandling.klagebehandling!!.resultat as Klagebehandlingsresultat.Opprettholdt,
                behandlingDetKlagesPå = "${sak.rammevedtaksliste.first().behandlingId}",
                rammebehandlingId = listOf(rammebehandling.id.toString()),
                åpenRammebehandlingId = rammebehandling.id.toString(),
                vedtakDetKlagesPå = rammebehandling.klagebehandling!!.formkrav.vedtakDetKlagesPå!!.toString(),
            )
        }
    }

    @Test
    fun `kan legge rammebehandling med tilknyttet ferdigstilt klage tilbake (fra rammebehandling)`() {
        val clock = TikkendeKlokke(fixedClockAt(1.januar(2025)))
        withTestApplicationContextAndPostgres(clock = clock, runIsolated = true) { tac ->
            val saksbehandler = ObjectMother.saksbehandler("saksbehandlerLeggeTilbake")
            val (sak, rammebehandling) = ferdigstillOpprettholdtKlagebehandlingOgOpprettRammebehandlingForKlage(
                tac = tac,
                type = "REVURDERING_OMGJØRING",
                saksbehandler = saksbehandler,
            )!!

            val (_, rammebehandlingLagtTilbake, rammebehandlingJson) = leggTilbakeRammebehandling(
                tac = tac,
                sakId = sak.id,
                behandlingId = rammebehandling.id,
                saksbehandler = saksbehandler,
            )

            rammebehandlingJson.get("saksbehandler") shouldBe null
            rammebehandlingJson.getString("status") shouldBe "KLAR_TIL_BEHANDLING"

            rammebehandlingLagtTilbake.klagebehandling!!.saksbehandler shouldBe rammebehandling.saksbehandler
            rammebehandlingLagtTilbake.klagebehandling!!.status shouldBe Klagebehandlingsstatus.FERDIGSTILT
        }
    }
}
