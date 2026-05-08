package no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.gjenoppta

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContext
import no.nav.tiltakspenger.saksbehandling.felles.Ventestatus
import no.nav.tiltakspenger.saksbehandling.felles.VentestatusHendelse
import no.nav.tiltakspenger.saksbehandling.infra.route.shouldBeEqualToIgnoringLocalDateTime
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldekortbehandlingStatus
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOpprettMeldekortbehandlingSettPåVentOgGjenoppta
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingSendMeldekortbehandlingTilBeslutningTaBehandlingSettPåVentOgGjenoppta
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class GjenopptaMeldekortbehandlingRouteTest {

    @Test
    fun `saksbehandler kan gjenoppta meldekortbehandling`() {
        withTestApplicationContext { tac ->
            val (_, _, _, meldekortbehandling, json) = iverksettSøknadsbehandlingOpprettMeldekortbehandlingSettPåVentOgGjenoppta(tac = tac)!!

            meldekortbehandling.status shouldBe MeldekortbehandlingStatus.UNDER_BEHANDLING
            meldekortbehandling.saksbehandler shouldBe "Z12345"
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
                        VentestatusHendelse(
                            tidspunkt = LocalDateTime.MIN.plusNanos(1),
                            endretAv = "Z12345",
                            begrunnelse = "",
                            erSattPåVent = false,
                            status = "KLAR_TIL_BEHANDLING",
                            frist = null,
                        ),
                    ),
                ),
            )

            json.getString("status") shouldBe "UNDER_BEHANDLING"
            json.getString("saksbehandler") shouldBe "Z12345"
            json.getJSONArray("ventestatus").also { ventestatus ->
                ventestatus.length() shouldBe 2
                ventestatus.getJSONObject(1).also { hendelse ->
                    hendelse.getString("sattPåVentAv") shouldBe "Z12345"
                    hendelse.getString("begrunnelse") shouldBe ""
                    hendelse.getBoolean("erSattPåVent") shouldBe false
                    hendelse.getString("status") shouldBe "KLAR_TIL_BEHANDLING"
                    hendelse.isNull("frist") shouldBe true
                }
            }
        }
    }

    @Test
    fun `beslutter kan gjenoppta meldekortbehandling`() {
        withTestApplicationContext { tac ->
            val (_, _, _, meldekortbehandling, json) = iverksettSøknadsbehandlingSendMeldekortbehandlingTilBeslutningTaBehandlingSettPåVentOgGjenoppta(tac = tac)!!

            meldekortbehandling.status shouldBe MeldekortbehandlingStatus.UNDER_BESLUTNING
            meldekortbehandling.beslutter shouldBe "beslutter"
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
                        VentestatusHendelse(
                            tidspunkt = LocalDateTime.MIN.plusNanos(1),
                            endretAv = "beslutter",
                            begrunnelse = "",
                            erSattPåVent = false,
                            status = "KLAR_TIL_BESLUTNING",
                            frist = null,
                        ),
                    ),
                ),
            )

            json.getString("status") shouldBe "UNDER_BESLUTNING"
            json.getString("beslutter") shouldBe "beslutter"
            json.getJSONArray("ventestatus").also { ventestatus ->
                ventestatus.length() shouldBe 2
                ventestatus.getJSONObject(1).also { hendelse ->
                    hendelse.getString("sattPåVentAv") shouldBe "beslutter"
                    hendelse.getString("begrunnelse") shouldBe ""
                    hendelse.getBoolean("erSattPåVent") shouldBe false
                    hendelse.getString("status") shouldBe "KLAR_TIL_BESLUTNING"
                    hendelse.isNull("frist") shouldBe true
                }
            }
        }
    }
}
