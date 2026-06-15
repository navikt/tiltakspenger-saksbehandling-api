package no.nav.tiltakspenger.saksbehandling.klage.infra.route.gjenoppta

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.common.TikkendeKlokke
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.felles.Ventestatus
import no.nav.tiltakspenger.saksbehandling.felles.VentestatusHendelse
import no.nav.tiltakspenger.saksbehandling.fixedClockAt
import no.nav.tiltakspenger.saksbehandling.infra.route.shouldBeEqualToIgnoringLocalDateTime
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsstatus
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldekortbehandlingStatus
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.gjenopptattMeldekortbehandlingMedKlagebehandlingFraKlageRoute
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.gjenopptattMeldekortbehandlingMedKlagebehandlingFraKlarTilBehanlingFraMeldekortRoute
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.gjenopptattMeldekortbehandlingMedKlagebehandlingFraKlarTilBeslutningFraMeldekortRoute
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.gjenopptattMeldekortbehandlngMedKlagebehandlingFraMeldekortbehandling
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class GjenopptaKlagebehandlingMedMeldekortbehandlingRouteTest {

    @Test
    fun `kan gjenoppta klagebehandling med meldekortbehandling`() {
        val clock = TikkendeKlokke(fixedClockAt(1.januar(2025)))
        withTestApplicationContextAndPostgres(clock = clock, runIsolated = true) { tac ->
            val saksbehandler = ObjectMother.saksbehandler("saksbehandlerKlagebehandling")
            val (_, oppdatertMeldekortbehandling, json) = gjenopptattMeldekortbehandlingMedKlagebehandlingFraKlageRoute(
                tac = tac,
                saksbehandlerKlagebehandling = saksbehandler,
            )!!
            val oppdatertKlagebehandling = requireNotNull(oppdatertMeldekortbehandling.klagebehandling)

            oppdatertKlagebehandling.status shouldBe Klagebehandlingsstatus.UNDER_BEHANDLING
            oppdatertKlagebehandling.saksbehandler shouldBe saksbehandler.navIdent
            oppdatertKlagebehandling.ventestatus.erSattPåVent shouldBe false
            oppdatertKlagebehandling.ventestatus.shouldBeEqualToIgnoringLocalDateTime(
                Ventestatus(
                    listOf(
                        VentestatusHendelse(
                            tidspunkt = LocalDateTime.MIN,
                            endretAv = saksbehandler.navIdent,
                            begrunnelse = "begrunnelse for å sette klage på vent",
                            erSattPåVent = true,
                            status = "UNDER_BEHANDLING",
                            frist = 14.januar(2025),
                        ),
                        VentestatusHendelse(
                            tidspunkt = LocalDateTime.MIN.plusSeconds(1),
                            endretAv = saksbehandler.navIdent,
                            begrunnelse = "",
                            erSattPåVent = false,
                            status = "KLAR_TIL_BEHANDLING",
                            frist = null,
                        ),
                    ),
                ),
            )

            oppdatertMeldekortbehandling.status shouldBe MeldekortbehandlingStatus.UNDER_BEHANDLING
            oppdatertMeldekortbehandling.saksbehandler shouldBe saksbehandler.navIdent
            oppdatertMeldekortbehandling.ventestatus.erSattPåVent shouldBe false
            oppdatertMeldekortbehandling.ventestatus.shouldBeEqualToIgnoringLocalDateTime(
                Ventestatus(
                    listOf(
                        VentestatusHendelse(
                            tidspunkt = LocalDateTime.MIN,
                            endretAv = saksbehandler.navIdent,
                            begrunnelse = "begrunnelse for å sette klage på vent",
                            erSattPåVent = true,
                            status = "UNDER_BEHANDLING",
                            frist = 14.januar(2025),
                        ),
                        VentestatusHendelse(
                            tidspunkt = LocalDateTime.MIN.plusSeconds(1),
                            endretAv = saksbehandler.navIdent,
                            begrunnelse = "",
                            erSattPåVent = false,
                            status = "KLAR_TIL_BEHANDLING",
                            frist = null,
                        ),
                    ),
                ),
            )

            // Sjekk klagebehandling JSON (SakDTO)
            val klageJson = json.get("klageBehandlinger").first()
            klageJson.get("status").asString() shouldBe "UNDER_BEHANDLING"
            klageJson.get("saksbehandler").asString() shouldBe saksbehandler.navIdent
            val klageVentestatusArray = klageJson.get("ventestatus")
            klageVentestatusArray.size() shouldBe 2
            klageVentestatusArray[0].also { hendelse ->
                hendelse.get("sattPåVentAv").asString() shouldBe saksbehandler.navIdent
                hendelse.get("begrunnelse").asString() shouldBe ""
                hendelse.get("erSattPåVent").asBoolean() shouldBe false
                hendelse.get("status").asString() shouldBe "KLAR_TIL_BEHANDLING"
                hendelse.get("frist").isNull shouldBe true
            }

            // Sjekk meldekortbehandling JSON (SakDTO)
            val meldekortJson = json.get("meldekortbehandlinger").get(oppdatertMeldekortbehandling.id.toString())
            meldekortJson.get("status").asString() shouldBe "UNDER_BEHANDLING"
            meldekortJson.get("saksbehandler").asString() shouldBe saksbehandler.navIdent
            val meldekortVentestatusArray = meldekortJson.get("ventestatus")
            meldekortVentestatusArray.size() shouldBe 2
            meldekortVentestatusArray[0].also { hendelse ->
                hendelse.get("sattPåVentAv").asString() shouldBe saksbehandler.navIdent
                hendelse.get("begrunnelse").asString() shouldBe ""
                hendelse.get("erSattPåVent").asBoolean() shouldBe false
                hendelse.get("status").asString() shouldBe "KLAR_TIL_BEHANDLING"
                hendelse.get("frist").isNull shouldBe true
            }
        }
    }

    @Test
    fun `kan gjenoppta meldekortbehandling tilknyttet klage`() {
        val clock = TikkendeKlokke(fixedClockAt(1.januar(2025)))
        withTestApplicationContextAndPostgres(clock = clock, runIsolated = true) { tac ->
            val saksbehandler = ObjectMother.saksbehandler("saksbehandlerKlagebehandling")
            val (_, oppdatertMeldekortbehandling) = gjenopptattMeldekortbehandlngMedKlagebehandlingFraMeldekortbehandling(
                tac = tac,
                saksbehandlerKlagebehandling = saksbehandler,
            )!!
            val oppdatertKlagebehandling = requireNotNull(oppdatertMeldekortbehandling.klagebehandling)

            oppdatertKlagebehandling.status shouldBe Klagebehandlingsstatus.UNDER_BEHANDLING
            oppdatertKlagebehandling.saksbehandler shouldBe saksbehandler.navIdent
            oppdatertKlagebehandling.ventestatus.erSattPåVent shouldBe false
            oppdatertKlagebehandling.ventestatus.shouldBeEqualToIgnoringLocalDateTime(
                Ventestatus(
                    listOf(
                        VentestatusHendelse(
                            tidspunkt = LocalDateTime.MIN,
                            endretAv = saksbehandler.navIdent,
                            begrunnelse = "begrunnelse for å sette klage på vent",
                            erSattPåVent = true,
                            status = "UNDER_BEHANDLING",
                            frist = 14.januar(2025),
                        ),
                        VentestatusHendelse(
                            tidspunkt = LocalDateTime.MIN.plusSeconds(1),
                            endretAv = saksbehandler.navIdent,
                            begrunnelse = "",
                            erSattPåVent = false,
                            status = "KLAR_TIL_BEHANDLING",
                            frist = null,
                        ),
                    ),
                ),
            )

            oppdatertMeldekortbehandling.status shouldBe MeldekortbehandlingStatus.UNDER_BEHANDLING
            oppdatertMeldekortbehandling.saksbehandler shouldBe saksbehandler.navIdent
            oppdatertMeldekortbehandling.ventestatus.erSattPåVent shouldBe false
            oppdatertMeldekortbehandling.ventestatus.shouldBeEqualToIgnoringLocalDateTime(
                Ventestatus(
                    listOf(
                        VentestatusHendelse(
                            tidspunkt = LocalDateTime.MIN,
                            endretAv = saksbehandler.navIdent,
                            begrunnelse = "begrunnelse for å sette klage på vent",
                            erSattPåVent = true,
                            status = "UNDER_BEHANDLING",
                            frist = 14.januar(2025),
                        ),
                        VentestatusHendelse(
                            tidspunkt = LocalDateTime.MIN.plusSeconds(1),
                            endretAv = saksbehandler.navIdent,
                            begrunnelse = "",
                            erSattPåVent = false,
                            status = "KLAR_TIL_BEHANDLING",
                            frist = null,
                        ),
                    ),
                ),
            )
        }
    }

    @Test
    fun `saksbehandler kan gjenoppta meldekortbehandling med klagebehandling fra KLAR_TIL_BEHANDLING`() {
        val clock = TikkendeKlokke(fixedClockAt(1.januar(2025)))
        withTestApplicationContextAndPostgres(clock = clock, runIsolated = true) { tac ->
            val saksbehandler = ObjectMother.saksbehandler("saksbehandlerKlagebehandling")
            val (_, oppdatertMeldekortbehandling) =
                gjenopptattMeldekortbehandlingMedKlagebehandlingFraKlarTilBehanlingFraMeldekortRoute(
                    tac = tac,
                    saksbehandler = saksbehandler,
                )!!
            val oppdatertKlagebehandling = requireNotNull(oppdatertMeldekortbehandling.klagebehandling)

            // Sjekk meldekortbehandling domene
            oppdatertMeldekortbehandling.status shouldBe MeldekortbehandlingStatus.UNDER_BEHANDLING
            oppdatertMeldekortbehandling.saksbehandler shouldBe saksbehandler.navIdent
            oppdatertMeldekortbehandling.ventestatus.erSattPåVent shouldBe false
            oppdatertMeldekortbehandling.ventestatus.shouldBeEqualToIgnoringLocalDateTime(
                Ventestatus(
                    listOf(
                        VentestatusHendelse(
                            tidspunkt = LocalDateTime.MIN,
                            endretAv = saksbehandler.navIdent,
                            begrunnelse = "begrunnelse for å sette klage på vent",
                            erSattPåVent = true,
                            status = "UNDER_BEHANDLING",
                            frist = 14.januar(2025),
                        ),
                        VentestatusHendelse(
                            tidspunkt = LocalDateTime.MIN.plusSeconds(1),
                            endretAv = saksbehandler.navIdent,
                            begrunnelse = "",
                            erSattPåVent = false,
                            status = "KLAR_TIL_BEHANDLING",
                            frist = null,
                        ),
                    ),
                ),
            )

            // Sjekk klagebehandling domene - skal også gjenopptas
            oppdatertKlagebehandling.status shouldBe Klagebehandlingsstatus.UNDER_BEHANDLING
            oppdatertKlagebehandling.saksbehandler shouldBe saksbehandler.navIdent
            oppdatertKlagebehandling.ventestatus.erSattPåVent shouldBe false
            oppdatertKlagebehandling.ventestatus.shouldBeEqualToIgnoringLocalDateTime(
                Ventestatus(
                    listOf(
                        VentestatusHendelse(
                            tidspunkt = LocalDateTime.MIN,
                            endretAv = saksbehandler.navIdent,
                            begrunnelse = "begrunnelse for å sette klage på vent",
                            erSattPåVent = true,
                            status = "UNDER_BEHANDLING",
                            frist = 14.januar(2025),
                        ),
                        VentestatusHendelse(
                            tidspunkt = LocalDateTime.MIN.plusSeconds(1),
                            endretAv = saksbehandler.navIdent,
                            begrunnelse = "",
                            erSattPåVent = false,
                            status = "KLAR_TIL_BEHANDLING",
                            frist = null,
                        ),
                    ),
                ),
            )
        }
    }

    @Test
    fun `beslutter kan gjenoppta meldekortbehandling med klagebehandling fra KLAR_TIL_BESLUTNING`() {
        val clock = TikkendeKlokke(fixedClockAt(1.januar(2025)))
        withTestApplicationContextAndPostgres(clock = clock, runIsolated = true) { tac ->
            val saksbehandler = ObjectMother.saksbehandler("saksbehandlerKlagebehandling")
            val beslutter = ObjectMother.beslutter("beslutter")
            val (_, oppdatertMeldekortbehandling) =
                gjenopptattMeldekortbehandlingMedKlagebehandlingFraKlarTilBeslutningFraMeldekortRoute(
                    tac = tac,
                    saksbehandler = saksbehandler,
                    beslutter = beslutter,
                )!!
            val oppdatertKlagebehandling = requireNotNull(oppdatertMeldekortbehandling.klagebehandling)

            oppdatertMeldekortbehandling.status shouldBe MeldekortbehandlingStatus.UNDER_BESLUTNING
            oppdatertMeldekortbehandling.beslutter shouldBe beslutter.navIdent
            oppdatertMeldekortbehandling.ventestatus.erSattPåVent shouldBe false
            oppdatertMeldekortbehandling.ventestatus.shouldBeEqualToIgnoringLocalDateTime(
                Ventestatus(
                    listOf(
                        VentestatusHendelse(
                            tidspunkt = LocalDateTime.MIN,
                            endretAv = beslutter.navIdent,
                            begrunnelse = "begrunnelse for å sette meldekort på vent",
                            erSattPåVent = true,
                            status = "UNDER_BESLUTNING",
                            frist = 14.januar(2025),
                        ),
                        VentestatusHendelse(
                            tidspunkt = LocalDateTime.MIN.plusSeconds(1),
                            endretAv = beslutter.navIdent,
                            begrunnelse = "",
                            erSattPåVent = false,
                            status = "KLAR_TIL_BESLUTNING",
                            frist = null,
                        ),
                    ),
                ),
            )

            oppdatertKlagebehandling.status shouldBe Klagebehandlingsstatus.UNDER_BEHANDLING
            oppdatertKlagebehandling.saksbehandler shouldBe saksbehandler.navIdent
            oppdatertKlagebehandling.ventestatus.erSattPåVent shouldBe false
            oppdatertKlagebehandling.ventestatus.shouldBeEqualToIgnoringLocalDateTime(
                Ventestatus(
                    listOf(
                        VentestatusHendelse(
                            tidspunkt = LocalDateTime.MIN,
                            endretAv = beslutter.navIdent,
                            begrunnelse = "begrunnelse for å sette meldekort på vent",
                            erSattPåVent = true,
                            status = "UNDER_BEHANDLING",
                            frist = 14.januar(2025),
                        ),
                        VentestatusHendelse(
                            tidspunkt = LocalDateTime.MIN.plusSeconds(1),
                            endretAv = beslutter.navIdent,
                            begrunnelse = "",
                            erSattPåVent = false,
                            status = "KLAR_TIL_BEHANDLING",
                            frist = null,
                        ),
                    ),
                ),
            )
        }
    }
}
