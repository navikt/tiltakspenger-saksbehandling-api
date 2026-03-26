package no.nav.tiltakspenger.saksbehandling.klage.infra.route.ferdigstill

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
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.ferdigstillKlagebehandlingForSakId
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgOpprettRammebehandlingForKlage
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgVurderKlagebehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettRammebehandlingForKlage
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSakOgFerdigstillOppretholdtKlagebehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSakOgMottaOppretholdtKlagebehandlingFraKa
import org.junit.jupiter.api.Test
import java.util.UUID

class FerdigstillKlagebehandlingRouteTest {

    @Test
    fun `kan ferdigstille en klagebehandling (opprettholdelse) som ikke har behov for videre behandling`() {
        val clock = TikkendeKlokke(fixedClockAt(1.januar(2025)))
        withTestApplicationContextAndPostgres(clock = clock, runIsolated = true) { tac ->
            val (sak, klagebehandling, json) = opprettSakOgFerdigstillOppretholdtKlagebehandling(tac = tac)!!
            val resultat = klagebehandling.resultat as Klagebehandlingsresultat.Opprettholdt
            json.toString().shouldBeKlagebehandlingDTO(
                sakId = sak.id,
                klagebehandlingId = klagebehandling.id,
                fnr = "12345678911",
                saksbehandler = "saksbehandlerKlagebehandling",
                resultat = "OPPRETTHOLDT",
                vedtakDetKlagesPå = sak.rammevedtaksliste.first().id.toString(),
                behandlingDetKlagesPå = sak.rammevedtaksliste.first().behandlingId.toString(),
                status = "FERDIGSTILT",
                kanIverksetteVedtak = null,
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
                journalpostIdInnstillingsbrev = "2",
                dokumentInfoIder = listOf("1"),
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
    fun `kan ferdigstille en klagebehandling (opprettholdelse) som har behov for videre behandling`() {
        val clock = TikkendeKlokke(fixedClockAt(1.januar(2025)))
        withTestApplicationContextAndPostgres(clock = clock, runIsolated = true) { tac ->
            val (sak, klagebehandling, json) = opprettSakOgFerdigstillOppretholdtKlagebehandling(
                tac = tac,
                hendelseGenerering = { _, klagebehandling ->
                    GenerererKlageinstanshendelse.avsluttetJson(
                        eventId = UUID.randomUUID().toString(),
                        kildeReferanse = klagebehandling.id.toString(),
                        kabalReferanse = UUID.randomUUID().toString(),
                        avsluttetTidspunkt = nå(tac.clock).toString(),
                        utfall = Klageinstanshendelse.KlagebehandlingAvsluttet.KlagehendelseKlagebehandlingAvsluttetUtfall.UGUNST,
                    )
                },
            )!!
            val resultat = klagebehandling.resultat as Klagebehandlingsresultat.Opprettholdt
            json.toString().shouldBeKlagebehandlingDTO(
                sakId = sak.id,
                klagebehandlingId = klagebehandling.id,
                fnr = "12345678911",
                saksbehandler = "saksbehandlerKlagebehandling",
                resultat = "OPPRETTHOLDT",
                vedtakDetKlagesPå = sak.rammevedtaksliste.first().id.toString(),
                behandlingDetKlagesPå = klagebehandling.formkrav.behandlingDetKlagesPå?.toString(),
                status = "FERDIGSTILT",
                kanIverksetteVedtak = null,
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
                journalpostIdInnstillingsbrev = "2",
                dokumentInfoIder = listOf("1"),
                klageinstanshendelser = listOf(
                    """
                     {
                      "klagehendelseId": "${resultat.klageinstanshendelser.single().klagehendelseId}",
                      "klagebehandlingId": "${klagebehandling.id}",
                      "opprettet": "TIMESTAMP",
                      "sistEndret": "TIMESTAMP",
                      "eksternKlagehendelseId": "${resultat.klageinstanshendelser.single().eksternKlagehendelseId}",
                      "avsluttetTidspunkt": "TIMESTAMP",
                      "journalpostreferanser": [
                        "123",
                        "456"
                       ],
                      "utfall": "UGUNST",
                      "hendelsestype": "KLAGEBEHANDLING_AVSLUTTET"
                    }
                    """.trimIndent(),
                ),
            )
        }
    }

    @Test
    fun `kan ferdigstille en klagebehandling (omgjøring) uten å opprette ny rammebehandling`() {
        val clock = TikkendeKlokke(fixedClockAt(1.januar(2025)))
        withTestApplicationContextAndPostgres(clock = clock, runIsolated = true) { tac ->
            val (sak, _, rammevedtakSøknadsbehandling, klagebehandling, _) = iverksettSøknadsbehandlingOgVurderKlagebehandling(
                tac = tac,
            )!!

            val (_, _, ferdigstiltklageJson) = ferdigstillKlagebehandlingForSakId(
                tac = tac,
                sakId = sak.id,
                klagebehandlingId = klagebehandling.id,
            )!!

            ferdigstiltklageJson.toString().shouldBeKlagebehandlingDTO(
                sakId = sak.id,
                klagebehandlingId = klagebehandling.id,
                fnr = "12345678911",
                saksbehandler = "saksbehandlerKlagebehandling",
                resultat = "OMGJØR",
                årsak = "PROSESSUELL_FEIL",
                begrunnelse = "Begrunnelse for omgjøring",
                vedtakDetKlagesPå = rammevedtakSøknadsbehandling.id.toString(),
                behandlingDetKlagesPå = klagebehandling.formkrav.behandlingDetKlagesPå?.toString(),
                status = "FERDIGSTILT",
                rammebehandlingId = null,
                ferdigstiltTidspunkt = true,
                iverksattTidspunkt = null,
            )
        }
    }

    @Test
    fun `kan ikke ferdigstille en klagebehandling (opprettholdelse) som har en aktiv rammebehandling`() {
        val clock = TikkendeKlokke(fixedClockAt(1.januar(2025)))
        withTestApplicationContextAndPostgres(clock = clock, runIsolated = true) { tac ->
            val (sak, oversendtKlagebehandling, _) = opprettSakOgMottaOppretholdtKlagebehandlingFraKa(
                tac = tac,
            )!!

            val (_, opprettetRammebehandling) = opprettRammebehandlingForKlage(
                tac = tac,
                sakId = sak.id,
                klagebehandlingId = oversendtKlagebehandling.id,
                søknadId = null,
                vedtakIdSomOmgjøres = oversendtKlagebehandling.formkrav.vedtakDetKlagesPå!!.toString(),
                type = "REVURDERING_OMGJØRING",
            )!!

            ferdigstillKlagebehandlingForSakId(
                tac = tac,
                sakId = sak.id,
                klagebehandlingId = opprettetRammebehandling.klagebehandling!!.id,
                forventetStatus = HttpStatusCode.BadRequest,
                forventetJsonBody = {
                    //language=json
                    """
                        {
                          "kode": "klagebehandling_er_knyttet_til_rammebehandling",
                          "melding": "Klagebehandlingen er knyttet til en rammebehandling og kan derfor ikke ferdigstilles. Rammebehandlingen må enten avbrytes, eller vedtas"
                        }
                    """.trimIndent()
                },
            )
        }
    }

    @Test
    fun `kan ikke ferdigstille en klagebehandling (omgjøring) som har en aktiv rammebehandling`() {
        val clock = TikkendeKlokke(fixedClockAt(1.januar(2025)))
        withTestApplicationContextAndPostgres(clock = clock, runIsolated = true) { tac ->
            val (sak, rammebehandlingMedKlagebehandling, json) = iverksettSøknadsbehandlingOgOpprettRammebehandlingForKlage(
                tac = tac,
            )!!

            ferdigstillKlagebehandlingForSakId(
                tac = tac,
                sakId = sak.id,
                klagebehandlingId = rammebehandlingMedKlagebehandling.klagebehandling!!.id,
                forventetStatus = HttpStatusCode.BadRequest,
                forventetJsonBody = {
                    //language=json
                    """
                        {
                          "kode": "klagebehandling_er_knyttet_til_rammebehandling",
                          "melding": "Klagebehandlingen er knyttet til en rammebehandling og kan derfor ikke ferdigstilles. Rammebehandlingen må enten avbrytes, eller vedtas"
                        }
                    """.trimIndent()
                },
            )
        }
    }

    @Test
    fun `kan ferdigstille med begrunnelse for ferdigstilling`() {
        val clock = TikkendeKlokke(fixedClockAt(1.januar(2025)))
        withTestApplicationContextAndPostgres(clock = clock, runIsolated = true) { tac ->
            val (sak, _, rammevedtakSøknadsbehandling, klagebehandling, _) = iverksettSøknadsbehandlingOgVurderKlagebehandling(
                tac = tac,
            )!!

            val (_, _, ferdigstiltklageJson) = ferdigstillKlagebehandlingForSakId(
                tac = tac,
                sakId = sak.id,
                klagebehandlingId = klagebehandling.id,
                begrunnelse = "Dette er en veltenkt begrunnelse for ferdigstilling",
            )!!

            ferdigstiltklageJson.toString().shouldBeKlagebehandlingDTO(
                sakId = sak.id,
                klagebehandlingId = klagebehandling.id,
                fnr = "12345678911",
                saksbehandler = "saksbehandlerKlagebehandling",
                resultat = "OMGJØR",
                årsak = "PROSESSUELL_FEIL",
                begrunnelse = "Begrunnelse for omgjøring",
                vedtakDetKlagesPå = rammevedtakSøknadsbehandling.id.toString(),
                behandlingDetKlagesPå = klagebehandling.formkrav.behandlingDetKlagesPå?.toString(),
                status = "FERDIGSTILT",
                rammebehandlingId = null,
                ferdigstiltTidspunkt = true,
                iverksattTidspunkt = null,
                begrunnelseFerdigstilling = "Dette er en veltenkt begrunnelse for ferdigstilling",
            )
        }
    }
}
