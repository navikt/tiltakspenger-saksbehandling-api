package no.nav.tiltakspenger.saksbehandling.klage.infra.route.ferdigstill

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.ktor.http.HttpStatusCode
import no.nav.tiltakspenger.libs.common.TikkendeKlokke
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.fixedClockAt
import no.nav.tiltakspenger.saksbehandling.infra.route.shouldBeEqualToIgnoringLocalDateTime
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsresultat
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsstatus
import no.nav.tiltakspenger.saksbehandling.klage.domene.hendelse.Klageinstanshendelse
import no.nav.tiltakspenger.saksbehandling.klage.infra.kafka.GenerererKlageinstanshendelse
import no.nav.tiltakspenger.saksbehandling.klage.infra.route.shouldBeKlagebehandlingDTO
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSakOgFerdigstillKlagebehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSakOgFerdigstillKlagebehandlingMedNyRammebehandling
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
    fun `Avsluttet hendelse + visse utfall skal ikke ferdigstilles uten en opprettet rammebehandling`() {
        val clock = TikkendeKlokke(fixedClockAt(1.januar(2025)))
        withTestApplicationContextAndPostgres(clock = clock, runIsolated = true) { tac ->
            opprettSakOgFerdigstillKlagebehandling(
                tac = tac,
                forventetStatus = HttpStatusCode.InternalServerError,
                forventetJsonBody = { """{"kode": "server_feil","melding": "Noe gikk galt på serversiden"}""" },
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

    @Test
    fun `Ferdigstiller avsluttet hendelse (medhold) med ny revurdering-omgjøring`() {
        val clock = TikkendeKlokke(fixedClockAt(1.januar(2025)))
        withTestApplicationContextAndPostgres(clock = clock, runIsolated = true) { tac ->
            val (sak, rammebehandling, klagebehandling, rammebehandlingJson) = opprettSakOgFerdigstillKlagebehandlingMedNyRammebehandling(
                tac = tac,
            )!!

            rammebehandlingJson.toString().shouldBeEqualToIgnoringLocalDateTime(
                """{"avbrutt":null,"attesteringer":[],"saksnummer":"${sak.saksnummer}","saksbehandler":"saksbehandlerKlagebehandling","utbetalingskontroll":null,"iverksattTidspunkt":null,"vedtaksperiode":null,"fritekstTilVedtaksbrev":null,"resultat":"OMGJØRING_IKKE_VALGT","type":"REVURDERING","beslutter":null,"begrunnelseVilkårsvurdering":null,"klagebehandlingId":"${klagebehandling.id}","utbetaling":null,"ventestatus":null,"omgjørVedtak":"${klagebehandling.formkrav.vedtakDetKlagesPå!!}","saksopplysninger":{"oppslagstidspunkt":"2025-01-01T01:02:52.456789","tiltaksdeltagelse":[{"typeKode":"GRUPPE_AMO","gjennomforingsprosent":null,"eksternDeltagelseId":"61328250-7d5d-4961-b70e-5cb727a34371","gjennomføringId":"358f6fe9-ebbe-4f7d-820f-2c0f04055c23","antallDagerPerUke":5,"deltakelseStatus":"Deltar","typeNavn":"Arbeidsmarkedsoppfølging gruppe","deltagelseFraOgMed":"2023-01-01","deltagelseTilOgMed":"2023-03-31","kilde":"Komet","internDeltakelseId":"tiltaksdeltaker_01KEYFWFRPZ9F0H446TF8HQFP0","deltakelseProsent":100}],"fødselsdato":"2001-01-01","ytelser":[],"tiltakspengevedtakFraArena":[],"periode":{"fraOgMed":"2023-01-01","tilOgMed":"2023-03-31"}},"rammevedtakId":null,"sistEndret":"2025-01-01T01:02:51.456789","sakId":"${sak.id}","id":"${rammebehandling.id}","status":"UNDER_BEHANDLING"}""".trimIndent(),
            )

            klagebehandling.rammebehandlingId shouldBe rammebehandling.id
            klagebehandling.status shouldBe Klagebehandlingsstatus.FERDIGSTILT
            klagebehandling.resultat.shouldBeInstanceOf<Klagebehandlingsresultat.Opprettholdt>()
            klagebehandling.resultat.ferdigstiltTidspunkt.shouldNotBeNull()
        }
    }
}
