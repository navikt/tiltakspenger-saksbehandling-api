package no.nav.tiltakspenger.saksbehandling.klage.infra.route.iverksett

import io.kotest.matchers.equality.shouldBeEqualToIgnoringFields
import io.kotest.matchers.shouldBe
import io.ktor.http.HttpStatusCode
import no.nav.tiltakspenger.libs.common.TikkendeKlokke
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.distribusjon.DistribusjonId
import no.nav.tiltakspenger.saksbehandling.fixedClockAt
import no.nav.tiltakspenger.saksbehandling.infra.route.shouldBeEqualToIgnoringLocalDateTime
import no.nav.tiltakspenger.saksbehandling.infra.route.shouldEqualJsonIgnoringTimestamps
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsstatus
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagevedtak
import no.nav.tiltakspenger.saksbehandling.klage.infra.repo.KlagevedtakPostgresRepo
import no.nav.tiltakspenger.saksbehandling.klage.infra.route.shouldBeKlagebehandlingDTO
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.hentSakForSaksnummer
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettForBehandlingId
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettKlagebehandlingForSakId
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgOpprettRammebehandlingForKlage
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgVurderKlagebehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.oppdaterSøknadsbehandlingInnvilgelse
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSakOgAvbrytKlagebehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSakOgIverksettKlagebehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSakOgKlagebehandlingTilAvvisning
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSakOgOppdaterKlagebehandlingTilAvvisningBrevtekst
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.sendSøknadsbehandlingTilBeslutningForBehandlingId
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.taBehandling
import org.junit.jupiter.api.Test
import java.time.LocalDate

class IverksettKlagebehandlingRouteTest {
    @Test
    fun `kan iverksette avvist klagebehandling`() {
        val clock = TikkendeKlokke(fixedClockAt(1.januar(2025)))
        // TODO jah: Fjern runIsolated når vi har fikset at databasetester kan kjøre parallelt (tiltaksdeltakelse og fnr må være garantert unik per test)
        withTestApplicationContextAndPostgres(clock = clock, runIsolated = true) { tac ->
            val (sak, klagevedtak, json) = opprettSakOgIverksettKlagebehandling(
                tac = tac,
            )!!
            val klagebehandling = klagevedtak.behandling
            val expectedKlagevedtak = Klagevedtak(
                id = klagevedtak.id,
                opprettet = klagevedtak.behandling.iverksattTidspunkt!!,
                behandling = klagebehandling,
                journalpostId = JournalpostId("1"),
                journalføringstidspunkt = klagevedtak.behandling.iverksattTidspunkt.plusSeconds(1),
                distribusjonId = DistribusjonId("1"),
                distribusjonstidspunkt = klagevedtak.behandling.iverksattTidspunkt.plusSeconds(2),
                vedtaksdato = LocalDate.parse("2025-01-01"),
                sendtTilDatadeling = null,
            )
            klagevedtak.shouldBeEqualToIgnoringLocalDateTime(expectedKlagevedtak)
            tac.sessionFactory.withSession {
                KlagevedtakPostgresRepo.hentForSakId(sak.id, it).single().shouldBeEqualToIgnoringLocalDateTime(expectedKlagevedtak)
            }
            json.toString().shouldBeKlagebehandlingDTO(
                sakId = sak.id,
                saksnummer = sak.saksnummer,
                klagebehandlingId = klagebehandling.id,
                fnr = "12345678911",
                status = "IVERKSATT",
                resultat = "AVVIST",
                iverksattTidspunkt = "TIMESTAMP",
                brevtekst = listOf("""{"tittel": "Avvisning av klage","tekst": "Din klage er dessverre avvist."}"""),
            )
            hentSakForSaksnummer(tac = tac, saksnummer = klagebehandling.saksnummer)!!.getJSONArray("alleKlagevedtak")
                .also {
                    it.length() shouldBe 1
                    val hentetKlagevedtakJson = it.getJSONObject(0)
                    hentetKlagevedtakJson.toString().shouldEqualJsonIgnoringTimestamps(
                        """
                    {
                      "klagebehandlingId": "${klagebehandling.id}",
                      "journalføringstidspunkt": "TIMESTAMP",
                      "opprettet": "TIMESTAMP",
                      "distribusjonstidspunkt": "TIMESTAMP",
                      "distribusjonId": "1",
                      "sakId": "${sak.id}",
                      "klagevedtakId": "${klagevedtak.id}",
                      "vedtaksdato": "2025-01-01",
                      "journalpostId": "1",
                      "resultat": "AVVIST"
                    }
                        """.trimIndent(),
                    )
                }
        }
    }

    @Test
    fun `kan ikke iverksette omgjøring fra klageroute`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            val (sak, _, _, klagebehandling) = iverksettSøknadsbehandlingOgVurderKlagebehandling(
                tac = tac,
            )!!
            iverksettKlagebehandlingForSakId(
                tac = tac,
                sakId = sak.id,
                klagebehandlingId = klagebehandling.id,
                forventetStatus = HttpStatusCode.BadRequest,
                forventetJsonBody = null,
            ) shouldBe null
        }
    }

    @Test
    fun `kan ikke iverksette avbrutt klagebehandling`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            val (sak, klagebehandling, _) = opprettSakOgAvbrytKlagebehandling(
                tac = tac,
            )!!
            iverksettKlagebehandlingForSakId(
                tac = tac,
                sakId = sak.id,
                klagebehandlingId = klagebehandling.id,
                forventetStatus = HttpStatusCode.BadRequest,
                forventetJsonBody = {
                    """
                     {
                        "melding": "Kan kun iverksette klagebehandling med status UNDER_BEHANDLING",
                        "kode": "må_ha_status_under_behandling"
                     }
                    """.trimIndent()
                },
            ) shouldBe null
        }
    }

    @Test
    fun `kan ikke iverksette allerede iverksatt avvist klagebehandling`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            val (sak, klagevedtak, _) = opprettSakOgIverksettKlagebehandling(
                tac = tac,
            )!!
            val klagebehandling = klagevedtak.behandling
            iverksettKlagebehandlingForSakId(
                tac = tac,
                sakId = sak.id,
                klagebehandlingId = klagebehandling.id,
                forventetStatus = HttpStatusCode.BadRequest,
                forventetJsonBody = {
                    """
                     {
                        "melding": "Kan kun iverksette klagebehandling med status UNDER_BEHANDLING",
                        "kode": "må_ha_status_under_behandling"
                     }
                    """.trimIndent()
                },
            ) shouldBe null
        }
    }

    @Test
    fun `kan ikke iverksette - feil saksbehandler`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            val (sak, klagebehandling, _) = opprettSakOgOppdaterKlagebehandlingTilAvvisningBrevtekst(
                tac = tac,
            )!!
            iverksettKlagebehandlingForSakId(
                tac = tac,
                sakId = sak.id,
                saksbehandler = ObjectMother.saksbehandler("annenSaksbehandler"),
                klagebehandlingId = klagebehandling.id,
                forventetStatus = HttpStatusCode.BadRequest,
                forventetJsonBody = {
                    """
                     {
                        "melding": "Du kan ikke utføre handlinger på en behandling som ikke er tildelt deg. Behandlingen er tildelt saksbehandlerKlagebehandling",
                        "kode": "behandling_eies_av_annen_saksbehandler"
                     }
                    """.trimIndent()
                },
            ) shouldBe null
        }
    }

    @Test
    fun `kan ikke iverksette avvist klagebehandling uten brevtekst`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            val (sak, klagebehandling, _) = opprettSakOgKlagebehandlingTilAvvisning(
                tac = tac,
            )!!
            iverksettKlagebehandlingForSakId(
                tac = tac,
                sakId = sak.id,
                klagebehandlingId = klagebehandling.id,
                forventetStatus = HttpStatusCode.BadRequest,
                forventetJsonBody = {
                    """
                     {
                        "melding": "Kan ikke iverksette klagebheandling uten brevtekst",
                        "kode": "mangler_brevtekst"
                     }
                    """.trimIndent()
                },
            ) shouldBe null
        }
    }

    @Test
    fun `kan iverksette klagebehandling til omgjøring`() {
        val clock = TikkendeKlokke(fixedClockAt(1.januar(2025)))
        withTestApplicationContextAndPostgres(clock = clock, runIsolated = true) { tac ->
            val (sak, rammebehandlingMedKlagebehandling, _) = iverksettSøknadsbehandlingOgOpprettRammebehandlingForKlage(
                tac = tac,
            )!!
            val klagebehandling = rammebehandlingMedKlagebehandling.klagebehandling!!
            val saksbehandler = ObjectMother.saksbehandler(klagebehandling.saksbehandler!!)
            oppdaterSøknadsbehandlingInnvilgelse(
                tac = tac,
                sakId = sak.id,
                behandlingId = rammebehandlingMedKlagebehandling.id,
                saksbehandler = saksbehandler,
            )
            sendSøknadsbehandlingTilBeslutningForBehandlingId(
                tac = tac,
                sakId = sak.id,
                behandlingId = rammebehandlingMedKlagebehandling.id,
                saksbehandler = saksbehandler,
            )
            val beslutter = ObjectMother.beslutter()
            taBehandling(
                tac = tac,
                sakId = sak.id,
                behandlingId = rammebehandlingMedKlagebehandling.id,
                saksbehandler = beslutter,
            )
            val (_, rammevedtak, json) = iverksettForBehandlingId(
                tac = tac,
                sakId = sak.id,
                behandlingId = rammebehandlingMedKlagebehandling.id,
                beslutter = beslutter,
            )!!
            rammevedtak.klagebehandling!!.also {
                it.status shouldBe Klagebehandlingsstatus.VEDTATT
                it.kanIverksetteVedtak shouldBe false
                it.erVedtatt shouldBe true
                it.erAvsluttet shouldBe true
                it.erUnderBehandling shouldBe false
                it.erÅpen shouldBe false
            }
            rammevedtak.klagebehandling.shouldBeEqualToIgnoringFields(
                klagebehandling,
                Klagebehandling::sistEndret,
                Klagebehandling::iverksattTidspunkt,
                Klagebehandling::status,
                Klagebehandling::kanIverksetteVedtak,
                Klagebehandling::erVedtatt,
                Klagebehandling::erAvsluttet,
                Klagebehandling::erUnderBehandling,
                Klagebehandling::erÅpen,
            )
            rammevedtak.klagebehandlingsresultat shouldBe klagebehandling.resultat
            json.getString("klagebehandlingId") shouldBe klagebehandling.id.toString()
        }
    }
}
