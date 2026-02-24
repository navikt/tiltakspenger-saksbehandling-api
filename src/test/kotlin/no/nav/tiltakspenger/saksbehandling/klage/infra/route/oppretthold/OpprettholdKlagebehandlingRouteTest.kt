package no.nav.tiltakspenger.saksbehandling.klage.infra.route.oppretthold

import io.kotest.assertions.json.shouldEqualJson
import io.kotest.matchers.equality.shouldBeEqualToIgnoringFields
import io.kotest.matchers.shouldBe
import io.ktor.http.HttpStatusCode
import no.nav.tiltakspenger.libs.common.TikkendeKlokke
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.fixedClockAt
import no.nav.tiltakspenger.saksbehandling.infra.route.shouldEqualJsonIgnoringTimestamps
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsstatus
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettForBehandlingId
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettKlagebehandlingForSakId
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgOpprettRammebehandlingForKlage
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgVurderKlagebehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.oppdaterSøknadsbehandlingInnvilgelse
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSakOgAvbrytKlagebehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSakOgIverksettKlagebehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSakOgKlagebehandlingTilAvvisning
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSakOgOppdaterKlagebehandlingTilAvvisningBrevtekst
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSakOgOpprettholdKlagebehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.sendSøknadsbehandlingTilBeslutningForBehandlingId
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.taBehandling
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class OpprettholdKlagebehandlingRouteTest {
    @Test
    fun `kan opprettholde klagebehandling`() {
        val clock = TikkendeKlokke(fixedClockAt(1.januar(2025)))
        withTestApplicationContextAndPostgres(clock = clock, runIsolated = true) { tac ->
            val (sak, klagebehandling, json) = opprettSakOgOpprettholdKlagebehandling(tac = tac)!!
            val rammevedtakDetKlagesPå = sak.rammevedtaksliste.first()
            json.toString().shouldEqualJsonIgnoringTimestamps(
                """
                   {
                     "id": "${klagebehandling.id}",
                     "sakId": "${sak.id}",
                     "saksnummer": "${sak.saksnummer}",
                     "fnr": "12345678911",
                     "opprettet": "2025-01-01T01:02:36.456789",
                     "sistEndret": "2025-01-01T01:02:40.456789",
                     "saksbehandler": "saksbehandlerKlagebehandling",
                     "journalpostId": "12345",
                     "journalpostOpprettet": "2025-01-01T01:02:35.456789",
                     "status": "OPPRETTHOLDT",
                     "resultat": "OPPRETTHOLDT",
                     "vedtakDetKlagesPå": "${rammevedtakDetKlagesPå.id}",
                     "erKlagerPartISaken": true,
                     "klagesDetPåKonkreteElementerIVedtaket": true,
                     "erKlagefristenOverholdt": true,
                     "erUnntakForKlagefrist": null,
                     "erKlagenSignert": true,
                     "innsendingsdato": "2026-02-16",
                     "innsendingskilde": "DIGITAL",
                     "brevtekst": [
                       {
                         "tittel": "Hva klagesaken gjelder",
                         "tekst": "Vi viser til klage av 2025-01-01 på vedtak av 2025-01-01 der <kort om resultatet i vedtaket>"
                       },
                       {
                         "tittel": "Klagers anførsler",
                         "tekst": "<saksbehandler fyller ut>"
                       },
                       {
                         "tittel": "Vurdering av klagen",
                         "tekst": "<saksbehandler fyller ut>"
                       }
                     ],
                     "avbrutt": null,
                     "kanIverksetteVedtak": false,
                     "kanIverksetteOpprettholdelse": false,
                     "iverksattTidspunkt": null,
                     "årsak": null,
                     "begrunnelse": null,
                     "rammebehandlingId": null,
                     "ventestatus": null,
                     "hjemler": ["ARBEIDSMARKEDSLOVEN_17"],
                     "iverksattOpprettholdelseTidspunkt": "2025-01-01T01:02:40.456789",
                     "journalføringstidspunktInnstillingsbrev": null,
                     "distribusjonstidspunktInnstillingsbrev": null,
                     "oversendtKlageinstansenTidspunkt": null,
                     "klageinstanshendelser": []
                   }
                """.trimIndent(),
            )
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
//                it.sistEndret shouldBe LocalDateTime.parse("2025-01-01T01:02:52.456789")
//                it.iverksattTidspunkt shouldBe LocalDateTime.parse("2025-01-01T01:02:52.456789")
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
