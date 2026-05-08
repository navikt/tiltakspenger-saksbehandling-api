package no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.settPåVent

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContext
import no.nav.tiltakspenger.saksbehandling.felles.Ventestatus
import no.nav.tiltakspenger.saksbehandling.felles.VentestatusHendelse
import no.nav.tiltakspenger.saksbehandling.infra.route.shouldBeEqualToIgnoringLocalDateTime
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldekortbehandlingStatus
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOpprettMeldekortbehandlingOgSettPåVent
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingSendMeldekortbehandlingTilBeslutningTaBehandlingOgSettPåVent
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class SettMeldekortbehandlingPåVentRouteTest {

    @Test
    fun `saksbehandler kan sette meldekortbehandling på vent`() {
        withTestApplicationContext { tac ->
            val (_, _, _, meldekortbehandling, json) = iverksettSøknadsbehandlingOpprettMeldekortbehandlingOgSettPåVent(tac = tac)!!

            meldekortbehandling.status shouldBe MeldekortbehandlingStatus.KLAR_TIL_BEHANDLING
            meldekortbehandling.saksbehandler shouldBe null
            meldekortbehandling.ventestatus.shouldBeEqualToIgnoringLocalDateTime(
                Ventestatus(
                    listOf(
                        VentestatusHendelse(
                            tidspunkt = LocalDateTime.MIN,
                            endretAv = "Z12345",
                            begrunnelse = "Begrunnelse for å sette meldekortbehandling på vent",
                            erSattPåVent = true,
                            status = "UNDER_BEHANDLING",
                            frist = 1.januar(2026),
                        ),
                    ),
                ),
            )

            json.getString("status") shouldBe "KLAR_TIL_BEHANDLING"
            json.isNull("saksbehandler") shouldBe true
            json.getJSONArray("ventestatus").also { ventestatus ->
                ventestatus.length() shouldBe 1
                ventestatus.getJSONObject(0).also { hendelse ->
                    hendelse.getString("sattPåVentAv") shouldBe "Z12345"
                    hendelse.getString("begrunnelse") shouldBe "Begrunnelse for å sette meldekortbehandling på vent"
                    hendelse.getBoolean("erSattPåVent") shouldBe true
                    hendelse.getString("status") shouldBe "UNDER_BEHANDLING"
                    hendelse.getString("frist") shouldBe "2026-01-01"
                }
            }
        }
    }

    @Test
    fun `beslutter kan sette meldekortbehandling på vent`() {
        withTestApplicationContext { tac ->
            val (_, _, _, meldekortbehandling, json) = iverksettSøknadsbehandlingSendMeldekortbehandlingTilBeslutningTaBehandlingOgSettPåVent(tac = tac)!!

            meldekortbehandling.status shouldBe MeldekortbehandlingStatus.KLAR_TIL_BESLUTNING
            meldekortbehandling.beslutter shouldBe null
            meldekortbehandling.ventestatus.shouldBeEqualToIgnoringLocalDateTime(
                Ventestatus(
                    listOf(
                        VentestatusHendelse(
                            tidspunkt = LocalDateTime.MIN,
                            endretAv = "beslutter",
                            begrunnelse = "Begrunnelse for å sette meldekortbehandling på vent",
                            erSattPåVent = true,
                            status = "UNDER_BESLUTNING",
                            frist = 1.januar(2026),
                        ),
                    ),
                ),
            )

            json.getString("status") shouldBe "KLAR_TIL_BESLUTNING"
            json.isNull("beslutter") shouldBe true
            json.getJSONArray("ventestatus").also { ventestatus ->
                ventestatus.length() shouldBe 1
                ventestatus.getJSONObject(0).also { hendelse ->
                    hendelse.getString("sattPåVentAv") shouldBe "beslutter"
                    hendelse.getString("begrunnelse") shouldBe "Begrunnelse for å sette meldekortbehandling på vent"
                    hendelse.getBoolean("erSattPåVent") shouldBe true
                    hendelse.getString("status") shouldBe "UNDER_BESLUTNING"
                    hendelse.getString("frist") shouldBe "2026-01-01"
                }
            }
        }
    }
}
