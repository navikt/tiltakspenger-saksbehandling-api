package no.nav.tiltakspenger.saksbehandling.klage.infra.route.ferdigstill

import io.kotest.matchers.shouldBe
import io.ktor.http.HttpStatusCode
import no.nav.tiltakspenger.libs.common.TikkendeKlokke
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.fixedClockAt
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsresultat
import no.nav.tiltakspenger.saksbehandling.klage.domene.hendelse.Klageinstanshendelse
import no.nav.tiltakspenger.saksbehandling.klage.infra.kafka.GenerererKlageinstanshendelse
import no.nav.tiltakspenger.saksbehandling.klage.infra.route.shouldBeKlagebehandlingDTO
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSakOgFerdigstillKlagebehandling
import org.junit.jupiter.api.Test
import java.util.UUID

class FerdigstillKlagebehandlingRouteTest {

    @Test
    fun `kan ferdigstille en klagebehandling`() {
        val clock = TikkendeKlokke(fixedClockAt(1.januar(2025)))
        withTestApplicationContextAndPostgres(clock = clock, runIsolated = true) { tac ->
            val (sak, klagebehandling, json) = opprettSakOgFerdigstillKlagebehandling(tac = tac)!!
            val resultat = klagebehandling.resultat as Klagebehandlingsresultat.Opprettholdt
            json.toString().shouldBeKlagebehandlingDTO(
                sakId = sak.id,
                klagebehandlingId = klagebehandling.id,
                fnr = "12345678911",
                saksbehandler = "saksbehandlerKlagebehandling",
                resultat = "OPPRETTHOLDT",
                vedtakDetKlagesPå = sak.rammevedtaksliste.first().id.toString(),
                status = "FERDIGSTILT",
                kanIverksetteVedtak = false,
                rammebehandlingId = null,
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
                ferdigstiltTidspunkt = true,
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
    fun `Avsluttet hendelse + visse utfall skal ikke ferdigstilles (ugunst i denne testen)`() {
        val clock = TikkendeKlokke(fixedClockAt(1.januar(2025)))
        withTestApplicationContextAndPostgres(clock = clock, runIsolated = true) { tac ->
            opprettSakOgFerdigstillKlagebehandling(
                tac = tac,
                forventetStatus = HttpStatusCode.BadRequest,
                forventetJsonBody = {
                    """{
                           "kode": "utfall_fra_klageinstans_skal_føre_til_ny_rammebehandling",
                           "melding": "Klagebehandlingen har et utfall fra klageinstansen som skal føre til ny rammebehandling, og kan derfor ikke ferdigstilles"
                     }
                    """.trimIndent()
                },
                hendelseGenerering = { _, klagebehandling ->
                    GenerererKlageinstanshendelse.avsluttetJson(
                        eventId = UUID.randomUUID().toString(),
                        kildeReferanse = klagebehandling.id.toString(),
                        kabalReferanse = UUID.randomUUID().toString(),
                        avsluttetTidspunkt = nå(tac.clock).toString(),
                        utfall = Klageinstanshendelse.KlagebehandlingAvsluttet.KlagehendelseKlagebehandlingAvsluttetUtfall.UGUNST,
                    )
                },
            ) shouldBe null
        }
    }
}
