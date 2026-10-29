package no.nav.tiltakspenger.saksbehandling.klage.infra.route.forvaltningsmelding

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.ktor.test.common.ForventetRespons
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.infra.route.ikkeSladdetTekst
import no.nav.tiltakspenger.saksbehandling.infra.route.shouldEqualJsonIgnoringTimestamps
import no.nav.tiltakspenger.saksbehandling.klage.domene.brev.Brevtekster
import no.nav.tiltakspenger.saksbehandling.klage.domene.brev.TittelOgTekst
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.avbruttKlagebehandlng
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.forhåndsvisForvaltningsmelding
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSakOgKlagebehandlingTilOpprettholdelse
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSakOgSendForvaltningsmelding
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.sendForvaltningsmelding
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.settKlagebehandlingPåVent
import org.junit.jupiter.api.Test
import java.time.LocalDate

class ForvaltningsmeldingKlagebehandlingRouteTest {

    private val json = "application/json; charset=UTF-8"

    @Test
    fun `saksbehandler kan sende forvaltningsmelding`() {
        withTestApplicationContextAndPostgres { tac ->
            val (_, klagebehandling, json) = opprettSakOgSendForvaltningsmelding(tac = tac)!!

            klagebehandling.forvaltningsmelding.shouldNotBeNull().let {
                it.brevtekst shouldBe Brevtekster(
                    standardForvaltningsmeldingtekster.map { (tittel, tekst) -> TittelOgTekst(tittel = tittel, tekst = tekst) },
                )
                it.saksbehandler shouldBe "saksbehandlerKlagebehandling"
                it.sendtTidspunkt shouldBe klagebehandling.sistEndret
                it.journalpostId shouldBe null
            }
            json.get("forvaltningsmelding").toString().shouldEqualJsonIgnoringTimestamps(
                """
                {
                  "tekster": [
                    {"tittel": "Svartid", "tekst": ${ikkeSladdetTekst("Saken vil bli behandlet innen ca. 3 måneder.")}},
                    {"tittel": "Begrunnelse for saksbehandlingstiden vår", "tekst": ${ikkeSladdetTekst("Vi har mange saker til behandling.")}}
                  ],
                  "saksbehandler": "saksbehandlerKlagebehandling",
                  "sendtTidspunkt": "TIMESTAMP",
                  "journalpostId": null,
                  "journalføringstidspunkt": null,
                  "distribusjonstidspunkt": null
                }
                """.trimIndent(),
            )
        }
    }

    @Test
    fun `kan ikke sende forvaltningsmelding to ganger`() {
        withTestApplicationContextAndPostgres { tac ->
            val (sak, klagebehandling) = opprettSakOgSendForvaltningsmelding(tac = tac)!!

            sendForvaltningsmelding(
                tac = tac,
                sakId = sak.id,
                klagebehandlingId = klagebehandling.id,
                tekster = listOf("Svartid" to "Ny tekst"),
                forventet = ForventetRespons.json(
                    400,
                    """{"melding": "Det er allerede sendt en forvaltningsmelding for denne klagebehandlingen", "kode": "forvaltningsmelding_allerede_sendt"}""",
                    json,
                ),
            ) shouldBe null
        }
    }

    @Test
    fun `kan ikke sende forvaltningsmelding uten tekst`() {
        withTestApplicationContextAndPostgres { tac ->
            opprettSakOgSendForvaltningsmelding(
                tac = tac,
                tekster = emptyList(),
                forventet = ForventetRespons.json(
                    400,
                    """{"melding": "Forvaltningsmeldingen må ha minst én tekstblokk", "kode": "forvaltningsmelding_mangler_tekst"}""",
                    json,
                ),
            ) shouldBe null
        }
    }

    @Test
    fun `kan ikke sende forvaltningsmelding for en klagebehandling som er satt på vent`() {
        withTestApplicationContextAndPostgres { tac ->
            val (sak, _, klagebehandling) = opprettSakOgKlagebehandlingTilOpprettholdelse(tac = tac)!!
            settKlagebehandlingPåVent(
                tac = tac,
                sakId = sak.id,
                klagebehandlingId = klagebehandling.id,
                frist = LocalDate.now(tac.clock).plusWeeks(2),
            )!!

            sendForvaltningsmelding(
                tac = tac,
                sakId = sak.id,
                klagebehandlingId = klagebehandling.id,
                forventet = ForventetRespons.json(
                    400,
                    """{"melding": "Klagebehandlingen er satt på vent. Den må gjenopptas før den kan behandles videre.", "kode": "klagebehandling_er_satt_på_vent"}""",
                    json,
                ),
            ) shouldBe null
        }
    }

    @Test
    fun `kan ikke sende forvaltningsmelding for en avbrutt klagebehandling`() {
        withTestApplicationContextAndPostgres { tac ->
            val (sak, klagebehandling) = avbruttKlagebehandlng(tac = tac)!!

            sendForvaltningsmelding(
                tac = tac,
                sakId = sak.id,
                klagebehandlingId = klagebehandling.id,
                forventet = ForventetRespons.json(
                    400,
                    """{"melding": "Feil klagebehandlingsstatus. Forventet: [UNDER_BEHANDLING], faktisk: AVBRUTT", "kode": "feil_klagebehandlingsstatus"}""",
                    json,
                ),
            ) shouldBe null
        }
    }

    @Test
    fun `kan ikke sende forvaltningsmelding for en klagebehandling som eies av en annen saksbehandler`() {
        withTestApplicationContextAndPostgres { tac ->
            val (sak, _, klagebehandling) = opprettSakOgKlagebehandlingTilOpprettholdelse(tac = tac)!!

            sendForvaltningsmelding(
                tac = tac,
                sakId = sak.id,
                klagebehandlingId = klagebehandling.id,
                saksbehandler = ObjectMother.saksbehandler("annenSaksbehandler"),
                forventet = ForventetRespons.json(
                    400,
                    """{"melding": "Du kan ikke utføre handlinger på en behandling som ikke er tildelt deg. Behandlingen er tildelt saksbehandlerKlagebehandling", "kode": "behandling_eies_av_annen_saksbehandler"}""",
                    json,
                ),
            ) shouldBe null
        }
    }

    @Test
    fun `kan forhåndsvise forvaltningsmelding før og etter den er sendt`() {
        withTestApplicationContextAndPostgres { tac ->
            val (sak, _, klagebehandling) = opprettSakOgKlagebehandlingTilOpprettholdelse(tac = tac)!!

            forhåndsvisForvaltningsmelding(tac = tac, sakId = sak.id, klagebehandlingId = klagebehandling.id).shouldNotBeNull()
            sendForvaltningsmelding(tac = tac, sakId = sak.id, klagebehandlingId = klagebehandling.id)!!
            forhåndsvisForvaltningsmelding(
                tac = tac,
                sakId = sak.id,
                klagebehandlingId = klagebehandling.id,
                tekster = emptyList(),
            ).shouldNotBeNull()
        }
    }

    @Test
    fun `bruker uten fagrolle får 403 ved forhåndsvisning av forvaltningsmelding`() {
        withTestApplicationContextAndPostgres { tac ->
            val (sak, _, klagebehandling) = opprettSakOgKlagebehandlingTilOpprettholdelse(tac = tac)!!

            forhåndsvisForvaltningsmelding(
                tac = tac,
                sakId = sak.id,
                klagebehandlingId = klagebehandling.id,
                saksbehandler = ObjectMother.utvikler(),
                forventet = ForventetRespons(403, contentType = json),
            ) shouldBe null
        }
    }
}
