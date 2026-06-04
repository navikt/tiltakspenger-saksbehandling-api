package no.nav.tiltakspenger.saksbehandling.klage.infra.route.settPåVent

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
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.meldekortbehandlingMedKlageSattPåVentFraKlageRoute
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.meldekortbehandlingMedKlagebehandlingSattPåVentFraMeldekortRoute
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.meldekortbehandlingUnderBeslutningMedKlagebehandlingSattPåVentFraMeldekortRoute
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class SettKlagebehandlingMedMeldekortbehandlingPåVentRouteTest {
    @Test
    fun `kan sette klagebehandling med meldekortbehandling på vent`() {
        val clock = TikkendeKlokke(fixedClockAt(1.januar(2025)))
        withTestApplicationContextAndPostgres(clock = clock, runIsolated = true) { tac ->
            val saksbehandler = ObjectMother.saksbehandler("saksbehandlerKlagebehandling")
            val (_, oppdatertMeldekortbehandling, sakJson) = meldekortbehandlingMedKlageSattPåVentFraKlageRoute(
                tac = tac,
                saksbehandlerKlagebehandling = saksbehandler,
            )!!
            val oppdatertKlagebehandling = requireNotNull(oppdatertMeldekortbehandling.klagebehandling)

            oppdatertKlagebehandling.status shouldBe Klagebehandlingsstatus.KLAR_TIL_BEHANDLING
            oppdatertKlagebehandling.saksbehandler shouldBe null
            oppdatertKlagebehandling.ventestatus.erSattPåVent shouldBe true

            oppdatertMeldekortbehandling.status shouldBe MeldekortbehandlingStatus.KLAR_TIL_BEHANDLING
            oppdatertMeldekortbehandling.saksbehandler shouldBe null
            oppdatertMeldekortbehandling.ventestatus.erSattPåVent shouldBe true

            val klageJson = sakJson.get("klageBehandlinger").first()
            klageJson.get("status").asString() shouldBe "KLAR_TIL_BEHANDLING"
            klageJson.get("saksbehandler").isNull shouldBe true
            val klageVentestatusArray = klageJson.get("ventestatus")
            klageVentestatusArray.size() shouldBe 1
            klageVentestatusArray[0].also { hendelse ->
                hendelse.get("sattPåVentAv").asString() shouldBe "saksbehandlerKlagebehandling"
                hendelse.get("begrunnelse").asString() shouldBe "begrunnelse for å sette klage på vent"
                hendelse.get("erSattPåVent").asBoolean() shouldBe true
                hendelse.get("status").asString() shouldBe "UNDER_BEHANDLING"
                hendelse.get("frist").asString() shouldBe "2025-01-14"
            }

            val meldekortJson = sakJson.get("meldekortbehandlinger").get(oppdatertMeldekortbehandling.id.toString())
            meldekortJson.get("status").asString() shouldBe "KLAR_TIL_BEHANDLING"
            meldekortJson.get("saksbehandler").isNull shouldBe true
            val meldekortVentestatusArray = meldekortJson.get("ventestatus")
            meldekortVentestatusArray.size() shouldBe 1
            meldekortVentestatusArray[0].also { hendelse ->
                hendelse.get("sattPåVentAv").asString() shouldBe "saksbehandlerKlagebehandling"
                hendelse.get("begrunnelse").asString() shouldBe "begrunnelse for å sette klage på vent"
                hendelse.get("erSattPåVent").asBoolean() shouldBe true
                hendelse.get("status").asString() shouldBe "UNDER_BEHANDLING"
                hendelse.get("frist").asString() shouldBe "2025-01-14"
            }
        }
    }

    @Test
    fun `saksbehandler kan sette meldekortbehandling med klagebehandling på vent fra UNDER_BEHANDLING`() {
        val clock = TikkendeKlokke(fixedClockAt(1.januar(2025)))
        withTestApplicationContextAndPostgres(clock = clock, runIsolated = true) { tac ->
            val saksbehandler = ObjectMother.saksbehandler("saksbehandlerKlagebehandling")
            val (_, oppdatertMeldekortbehandling, sakJson) =
                meldekortbehandlingMedKlagebehandlingSattPåVentFraMeldekortRoute(
                    tac = tac,
                    saksbehandler = saksbehandler,
                )!!
            val oppdatertKlagebehandling = requireNotNull(oppdatertMeldekortbehandling.klagebehandling)

            oppdatertMeldekortbehandling.status shouldBe MeldekortbehandlingStatus.KLAR_TIL_BEHANDLING
            oppdatertMeldekortbehandling.saksbehandler shouldBe null
            oppdatertMeldekortbehandling.ventestatus.erSattPåVent shouldBe true
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
                    ),
                ),
            )

            oppdatertKlagebehandling.status shouldBe Klagebehandlingsstatus.KLAR_TIL_BEHANDLING
            oppdatertKlagebehandling.saksbehandler shouldBe null
            oppdatertKlagebehandling.ventestatus.erSattPåVent shouldBe true
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
                    ),
                ),
            )

            val meldekortJson = sakJson.get("meldekortbehandlinger").get(oppdatertMeldekortbehandling.id.toString())
            meldekortJson.get("status").asString() shouldBe "KLAR_TIL_BEHANDLING"
            meldekortJson.get("saksbehandler").isNull shouldBe true
            val meldekortVentestatusArray = meldekortJson.get("ventestatus")
            meldekortVentestatusArray.size() shouldBe 1
            meldekortVentestatusArray[0].also { hendelse ->
                hendelse.get("sattPåVentAv").asString() shouldBe saksbehandler.navIdent
                hendelse.get("begrunnelse").asString() shouldBe "begrunnelse for å sette klage på vent"
                hendelse.get("erSattPåVent").asBoolean() shouldBe true
                hendelse.get("status").asString() shouldBe "UNDER_BEHANDLING"
                hendelse.get("frist").asString() shouldBe "2025-01-14"
            }

            val klageJson = sakJson.get("klageBehandlinger").first()
            klageJson.get("status").asString() shouldBe "KLAR_TIL_BEHANDLING"
            klageJson.get("saksbehandler").isNull shouldBe true
            val klageVentestatusArray = klageJson.get("ventestatus")
            klageVentestatusArray.size() shouldBe 1
            klageVentestatusArray[0].also { hendelse ->
                hendelse.get("sattPåVentAv").asString() shouldBe saksbehandler.navIdent
                hendelse.get("begrunnelse").asString() shouldBe "begrunnelse for å sette klage på vent"
                hendelse.get("erSattPåVent").asBoolean() shouldBe true
                hendelse.get("status").asString() shouldBe "UNDER_BEHANDLING"
                hendelse.get("frist").asString() shouldBe "2025-01-14"
            }
        }
    }

    @Test
    fun `beslutter kan sette meldekortbehandling med klagebehandling på vent fra UNDER_BESLUTNING`() {
        val clock = TikkendeKlokke(fixedClockAt(1.januar(2025)))
        withTestApplicationContextAndPostgres(clock = clock, runIsolated = true) { tac ->
            val saksbehandler = ObjectMother.saksbehandler("saksbehandlerKlagebehandling")
            val beslutter = ObjectMother.beslutter("beslutter")
            val (_, oppdatertMeldekortbehandling, sakJson) =
                meldekortbehandlingUnderBeslutningMedKlagebehandlingSattPåVentFraMeldekortRoute(
                    tac = tac,
                    saksbehandler = saksbehandler,
                    beslutter = beslutter,
                )!!
            val oppdatertKlagebehandling = requireNotNull(oppdatertMeldekortbehandling.klagebehandling)

            oppdatertMeldekortbehandling.status shouldBe MeldekortbehandlingStatus.KLAR_TIL_BESLUTNING
            oppdatertMeldekortbehandling.beslutter shouldBe null
            oppdatertMeldekortbehandling.ventestatus.erSattPåVent shouldBe true
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
                    ),
                ),
            )

            oppdatertKlagebehandling.status shouldBe Klagebehandlingsstatus.KLAR_TIL_BEHANDLING
            oppdatertKlagebehandling.saksbehandler shouldBe "saksbehandlerKlagebehandling"
            oppdatertKlagebehandling.ventestatus.erSattPåVent shouldBe true
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
                    ),
                ),
            )

            val meldekortJson = sakJson.get("meldekortbehandlinger").get(oppdatertMeldekortbehandling.id.toString())
            meldekortJson.get("status").asString() shouldBe "KLAR_TIL_BESLUTNING"
            meldekortJson.get("beslutter").isNull shouldBe true
            val meldekortVentestatusArray = meldekortJson.get("ventestatus")
            meldekortVentestatusArray.size() shouldBe 1
            meldekortVentestatusArray[0].also { hendelse ->
                hendelse.get("sattPåVentAv").asString() shouldBe beslutter.navIdent
                hendelse.get("begrunnelse").asString() shouldBe "begrunnelse for å sette meldekort på vent"
                hendelse.get("erSattPåVent").asBoolean() shouldBe true
                hendelse.get("status").asString() shouldBe "UNDER_BESLUTNING"
                hendelse.get("frist").asString() shouldBe "2025-01-14"
            }

            val klageJson = sakJson.get("klageBehandlinger").first()
            klageJson.get("status").asString() shouldBe "KLAR_TIL_BEHANDLING"
            klageJson.get("saksbehandler").isNull shouldBe false
            val klageVentestatusArray = klageJson.get("ventestatus")
            klageVentestatusArray.size() shouldBe 1
            klageVentestatusArray[0].also { hendelse ->
                hendelse.get("sattPåVentAv").asString() shouldBe beslutter.navIdent
                hendelse.get("begrunnelse").asString() shouldBe "begrunnelse for å sette meldekort på vent"
                hendelse.get("erSattPåVent").asBoolean() shouldBe true
                hendelse.get("status").asString() shouldBe "UNDER_BEHANDLING"
                hendelse.get("frist").asString() shouldBe "2025-01-14"
            }
        }
    }
}
