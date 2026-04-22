package no.nav.tiltakspenger.saksbehandling.klage.infra.route.overta

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.common.RammebehandlingId
import no.nav.tiltakspenger.libs.common.TikkendeKlokke
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.fixedClockAt
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsresultat
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsstatus
import no.nav.tiltakspenger.saksbehandling.klage.infra.route.shouldBeFerdigstiltOpprettholdtKlagebehandlingDTO
import no.nav.tiltakspenger.saksbehandling.klage.infra.route.shouldBeKlagebehandlingDTO
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.ferdigstillOpprettholdtKlagebehandlingOgOpprettBehandlingForKlage
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgOvertaKlagebehandlingMedRammebehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSakOgOmgjørFraKaKlagebehandlingMedNyRammebehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.overtaBehanding
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.overtaKlagebehandling
import org.junit.jupiter.api.Test

class OvertaKlagebehandlingMedRammebehandlingRouteTest {
    @Test
    fun `overtar klagebehandlingen med rammebehandling`() {
        val clock = TikkendeKlokke(fixedClockAt(1.januar(2025)))
        withTestApplicationContextAndPostgres(clock = clock, runIsolated = true) { tac ->
            val (sak, rammebehandlingMedKlagebehandling, json) = iverksettSøknadsbehandlingOgOvertaKlagebehandlingMedRammebehandling(
                tac = tac,
            )!!
            val klagebehandling = rammebehandlingMedKlagebehandling.klagebehandling!!
            json.get("klageBehandlinger").first().toString().shouldBeKlagebehandlingDTO(
                sakId = sak.id,
                saksnummer = sak.saksnummer,
                klagebehandlingId = klagebehandling.id,
                fnr = "12345678911",
                saksbehandler = "saksbehandlerSomOvertarKlagebehandling",
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
            rammebehandlingMedKlagebehandling.saksbehandler shouldBe "saksbehandlerSomOvertarKlagebehandling"
        }
    }

    @Test
    fun `overtar klagebehandlingen med status omgjøring_etter_klageinstans`() {
        val clock = TikkendeKlokke(fixedClockAt(1.januar(2025)))
        withTestApplicationContextAndPostgres(clock = clock, runIsolated = true) { tac ->
            val (sak, rammebehandlingMedKlagebehandling, _, _) = opprettSakOgOmgjørFraKaKlagebehandlingMedNyRammebehandling(
                tac = tac,
            )!!
            val klagebehandling = rammebehandlingMedKlagebehandling.klagebehandling!!

            val saksbehandlerSomOvertar = ObjectMother.saksbehandler("saksbehandlerSomOvertarKlagebehandling")
            clock.spol1timeFrem()
            val (_, oppdatertKlagebehandling, json) = overtaKlagebehandling(
                tac = tac,
                sakId = sak.id,
                klagebehandlingId = klagebehandling.id,
                saksbehandler = saksbehandlerSomOvertar,
                overtarFra = klagebehandling.saksbehandler!!,
            )!!

            oppdatertKlagebehandling.status shouldBe Klagebehandlingsstatus.OMGJØRING_ETTER_KLAGEINSTANS
            oppdatertKlagebehandling.saksbehandler shouldBe saksbehandlerSomOvertar.navIdent

            val resultat = oppdatertKlagebehandling.resultat!! as Klagebehandlingsresultat.Opprettholdt
            json.get("behandlinger").last().get("saksbehandler").stringValue() shouldBe saksbehandlerSomOvertar.navIdent
            json.get("klageBehandlinger").single().toString().shouldBeKlagebehandlingDTO(
                sakId = sak.id,
                saksnummer = sak.saksnummer,
                klagebehandlingId = klagebehandling.id,
                fnr = "12345678911",
                saksbehandler = saksbehandlerSomOvertar.navIdent,
                resultat = "OPPRETTHOLDT",
                vedtakDetKlagesPå = "${sak.rammevedtaksliste.first().id}",
                behandlingDetKlagesPå = "${sak.rammevedtaksliste.first().behandlingId}",
                kanIverksetteVedtak = null,
                rammebehandlingId = listOf(rammebehandlingMedKlagebehandling.id.toString()),
                åpenRammebehandlingId = rammebehandlingMedKlagebehandling.id.toString(),
                status = "OMGJØRING_ETTER_KLAGEINSTANS",
                brevtekst = listOf(
                    """{"tittel":"Hva klagesaken gjelder","tekst":"Vi viser til klage av 2025-01-01 på vedtak av 2025-01-01 der <kort om resultatet i vedtaket>"}""",
                    """{"tittel":"Klagers anførsler","tekst":"<saksbehandler fyller ut>"}""",
                    """{"tittel":"Vurdering av klagen","tekst":"<saksbehandler fyller ut>"}""",
                ),
                hjemler = listOf("ARBEIDSMARKEDSLOVEN_17"),
                iverksattOpprettholdelseTidspunkt = true,
                journalføringstidspunktInnstillingsbrev = true,
                distribusjonstidspunktInnstillingsbrev = true,
                oversendtKlageinstansenTidspunkt = true,
                journalpostIdInnstillingsbrev = klagebehandling.journalpostIdInnstillingsbrev!!.toString(),
                dokumentInfoIder = klagebehandling.dokumentInfoIder.map { it.toString() },
                klageinstanshendelser = listOf(
                    """
                     {
                      "klagehendelseId": "${resultat.klageinstanshendelser.single().klagehendelseId}",
                      "klagebehandlingId": "${klagebehandling.id}",
                      "opprettet": "TIMESTAMP",
                      "sistEndret": "TIMESTAMP",
                      "eksternKlagehendelseId": "${resultat.klageinstanshendelser.single().eksternKlagehendelseId}",
                      "avsluttetTidspunkt": "TIMESTAMP",
                      "journalpostreferanser": [],
                      "utfall": "STADFESTELSE",
                      "hendelsestype": "KLAGEBEHANDLING_AVSLUTTET"
                    }
                    """.trimIndent(),
                ),
            )
        }
    }

    @Test
    fun `kan overta rammebehandling som er tilknyttet en ferdigstilt klagebehandling (gjort fra klagebehandling)`() {
        val clock = TikkendeKlokke(fixedClockAt(1.januar(2025)))
        withTestApplicationContextAndPostgres(clock = clock, runIsolated = true) { tac ->
            val (sak, rammebehandling) = ferdigstillOpprettholdtKlagebehandlingOgOpprettBehandlingForKlage(
                tac = tac,
                type = "REVURDERING_OMGJØRING",
            )!!
            val nySaksbehandler = ObjectMother.saksbehandler("saksbehandlerSomOvertarKlagebehandling")
            clock.spol1timeFrem()
            val (_, _, sakJson) = overtaKlagebehandling(
                tac = tac,
                sakId = sak.id,
                klagebehandlingId = rammebehandling.klagebehandling!!.id,
                saksbehandler = nySaksbehandler,
                overtarFra = rammebehandling.saksbehandler!!,
            )!!

            val overtattRammebehandling = sakJson.get("behandlinger").last()
            val klagebehandling = sakJson.get("klageBehandlinger").single()

            overtattRammebehandling.get("saksbehandler").asString() shouldBe nySaksbehandler.navIdent
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
    fun `kan overta rammebehandling som er tilknyttet en ferdigstilt klagebehandling (gjort fra rammebehandling)`() {
        val clock = TikkendeKlokke(fixedClockAt(1.januar(2025)))
        withTestApplicationContextAndPostgres(clock = clock, runIsolated = true) { tac ->
            val (sak, rammebehandling) = ferdigstillOpprettholdtKlagebehandlingOgOpprettBehandlingForKlage(
                tac = tac,
                type = "REVURDERING_OMGJØRING",
            )!!
            val nySaksbehandler = ObjectMother.saksbehandler("saksbehandlerSomOvertarKlagebehandling")
            clock.spol1timeFrem()
            val (_, overtattRammebehandling, rammebehandlingJson) = overtaBehanding(
                tac = tac,
                sakId = sak.id,
                behandlingId = rammebehandling.id as RammebehandlingId,
                overtarFra = rammebehandling.saksbehandler!!,
                saksbehandler = nySaksbehandler,
            )

            rammebehandlingJson.get("saksbehandler") shouldBe nySaksbehandler.navIdent
            overtattRammebehandling.klagebehandling!!.saksbehandler shouldBe rammebehandling.saksbehandler
            overtattRammebehandling.klagebehandling!!.status shouldBe Klagebehandlingsstatus.FERDIGSTILT
        }
    }
}
