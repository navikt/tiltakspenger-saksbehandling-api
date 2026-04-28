package no.nav.tiltakspenger.saksbehandling.klage.infra.route.settPåVent

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.TikkendeKlokke
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.felles.Ventestatus
import no.nav.tiltakspenger.saksbehandling.felles.VentestatusHendelse
import no.nav.tiltakspenger.saksbehandling.fixedClockAt
import no.nav.tiltakspenger.saksbehandling.infra.route.shouldBeEqualToIgnoringLocalDateTime
import no.nav.tiltakspenger.saksbehandling.infra.route.shouldEqualJsonIgnoringTimestamps
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsresultat
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsstatus
import no.nav.tiltakspenger.saksbehandling.klage.infra.route.shouldBeFerdigstiltOpprettholdtKlagebehandlingDTO
import no.nav.tiltakspenger.saksbehandling.klage.infra.route.shouldBeKlagebehandlingDTO
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.ferdigstillOpprettholdtKlagebehandlingOgOpprettRammebehandlingForKlage
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgOpprettRammebehandlingForKlage
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgSettKlagebehandlingMedRammebehandlingPåVent
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.settRammebehandlingPåVent
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

class SettKlagebehandlingMedRammebehandlingPåVentRouteTest {
    @Test
    fun `kan sette klagebehandling med rammebehandling på vent`() {
        val clock = TikkendeKlokke(fixedClockAt(1.januar(2025)))
        withTestApplicationContextAndPostgres(clock = clock, runIsolated = true) { tac ->
            val (sak, rammebehandlingMedKlagebehandling, json) = iverksettSøknadsbehandlingOgSettKlagebehandlingMedRammebehandlingPåVent(
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
                ventestatus = listOf("""{"sattPåVentAv": "saksbehandlerKlagebehandling","tidspunkt": "TIMESTAMP","begrunnelse": "begrunnelse for å sette klage på vent","erSattPåVent": true,"frist": "2025-01-14"}"""),
            )
            rammebehandlingMedKlagebehandling.status shouldBe Rammebehandlingsstatus.KLAR_TIL_BEHANDLING
            rammebehandlingMedKlagebehandling.saksbehandler shouldBe null
            rammebehandlingMedKlagebehandling.ventestatus.shouldBeEqualToIgnoringLocalDateTime(
                Ventestatus(
                    listOf(
                        VentestatusHendelse(
                            tidspunkt = LocalDateTime.MIN,
                            endretAv = "saksbehandlerKlagebehandling",
                            begrunnelse = "begrunnelse for å sette klage på vent",
                            erSattPåVent = true,
                            status = "UNDER_BEHANDLING",
                            frist = LocalDate.parse("2025-01-14"),
                        ),
                    ),
                ),
            )
        }
    }

    @Test
    fun `kan sette rammebehandling tilknyttet klage på vent`() {
        val clock = TikkendeKlokke(fixedClockAt(1.januar(2025)))
        withTestApplicationContextAndPostgres(clock = clock, runIsolated = true) { tac ->
            val saksbehandler: Saksbehandler = ObjectMother.saksbehandler("saksbehandlerKlagebehandling")
            val (sak, rammebehandlingMedKlagebehandling, _) = iverksettSøknadsbehandlingOgOpprettRammebehandlingForKlage(
                tac = tac,
                saksbehandlerKlagebehandling = saksbehandler,
            )!!
            val (_, _, oppdatertRammebehandlingMedKlagebehandling, json) = `settRammebehandlingPåVent`(
                tac = tac,
                sakId = sak.id,
                rammebehandlingId = rammebehandlingMedKlagebehandling.id,
                saksbehandler = saksbehandler,
                frist = null,
            )!!
            val klagebehandling = oppdatertRammebehandlingMedKlagebehandling.klagebehandling!!
            // TODO: sjekk noe her, men trenger kanskje ikke sjekke hele saken
//            json.toString().shouldEqualJsonIgnoringTimestamps(
//                """
//                    "HELE SAKEN GOES HERE"
//                """.trimIndent(),
//            )
            klagebehandling.status shouldBe Klagebehandlingsstatus.KLAR_TIL_BEHANDLING
            klagebehandling.saksbehandler shouldBe null
            klagebehandling.ventestatus.shouldBeEqualToIgnoringLocalDateTime(
                Ventestatus(
                    listOf(
                        VentestatusHendelse(
                            tidspunkt = LocalDateTime.MIN,
                            endretAv = "saksbehandlerKlagebehandling",
                            begrunnelse = "Begrunnelse for å sette rammebehandling på vent",
                            erSattPåVent = true,
                            status = "UNDER_BEHANDLING",
                            frist = null,
                        ),
                    ),
                ),
            )
        }
    }

    @Test
    fun `en rammebehandling med klagebehandling ferdigstilt setter kun rammebehandlingen på vent`() {
        val clock = TikkendeKlokke(fixedClockAt(1.januar(2025)))
        withTestApplicationContextAndPostgres(clock = clock, runIsolated = true) { tac ->
            val saksbehandler = ObjectMother.saksbehandler("saksbehandlerKlagebehandling")
            val (sak, rammebehandling) = ferdigstillOpprettholdtKlagebehandlingOgOpprettRammebehandlingForKlage(
                tac = tac,
                type = "REVURDERING_OMGJØRING",
                saksbehandler = saksbehandler,
            )!!

            val (_, _, rammebehandlingPåVent, sakJson) = settRammebehandlingPåVent(
                tac = tac,
                sakId = sak.id,
                rammebehandlingId = rammebehandling.id,
                saksbehandler = saksbehandler,
            )!!

            val rammebehandlingPåVentJson = sakJson.get("behandlinger").last()
            val klagebehandlingJson = sakJson.get("klageBehandlinger").single()

            rammebehandlingPåVentJson.get("ventestatus").toString().shouldEqualJsonIgnoringTimestamps(
                """[
                 {
                        "sattPåVentAv": "saksbehandlerKlagebehandling",
                        "tidspunkt": "TIMESTAMP",
                        "begrunnelse": "Begrunnelse for å sette rammebehandling på vent",
                        "erSattPåVent": true,
                        "frist": null
                    }
                ]
                """.trimIndent(),
            )

            klagebehandlingJson.toString().shouldBeFerdigstiltOpprettholdtKlagebehandlingDTO(
                sakId = sak.id,
                saksnummer = sak.saksnummer,
                klagebehandlingId = rammebehandling.klagebehandling!!.id,
                fnr = "12345678911",
                resultat = rammebehandlingPåVent.klagebehandling!!.resultat as Klagebehandlingsresultat.Opprettholdt,
                behandlingDetKlagesPå = "${sak.rammevedtaksliste.first().behandlingId}",
                rammebehandlingId = listOf(rammebehandling.id.toString()),
                åpenRammebehandlingId = rammebehandling.id.toString(),
                vedtakDetKlagesPå = rammebehandling.klagebehandling!!.formkrav.vedtakDetKlagesPå!!.toString(),
            )
        }
    }
}
