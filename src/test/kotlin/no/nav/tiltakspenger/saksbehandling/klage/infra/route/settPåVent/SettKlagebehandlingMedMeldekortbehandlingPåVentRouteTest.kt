package no.nav.tiltakspenger.saksbehandling.klage.infra.route.settPåVent

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.common.TikkendeKlokke
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.ktor.test.common.ForventetRespons
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.felles.Ventestatus
import no.nav.tiltakspenger.saksbehandling.felles.VentestatusHendelse
import no.nav.tiltakspenger.saksbehandling.fixedClockAt
import no.nav.tiltakspenger.saksbehandling.infra.route.klagebehandlingJson
import no.nav.tiltakspenger.saksbehandling.infra.route.shouldBeEqualToIgnoringLocalDateTime
import no.nav.tiltakspenger.saksbehandling.infra.route.shouldHaSisteVentestatus
import no.nav.tiltakspenger.saksbehandling.infra.route.sladdbarVerdi
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsstatus
import no.nav.tiltakspenger.saksbehandling.klage.domene.hentKlagebehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldekortbehandlingStatus
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.meldekortbehandlingMedKlageSattPåVentFraKlageRoute
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.meldekortbehandlingMedKlagebehandlingSattPåVentFraMeldekortRoute
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.meldekortbehandlingUnderBeslutningMedKlagebehandlingSattPåVentFraMeldekortRoute
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettMeldekortbehandlingForKlage
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.settKlagebehandlingPåVent
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.settMeldekortbehandlingPåVent
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class SettKlagebehandlingMedMeldekortbehandlingPåVentRouteTest {
    @Test
    fun `kan sette klagebehandling med meldekortbehandling på vent fra klagebehandling`() {
        val clock = TikkendeKlokke(fixedClockAt(1.januar(2025)))
        withTestApplicationContextAndPostgres(clock = clock) { tac ->
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

            val klageJson = sakJson.get("klagebehandlinger").first()
            klageJson.get("status").asString() shouldBe "KLAR_TIL_BEHANDLING"
            klageJson.get("saksbehandler").isNull shouldBe true
            val klageVentestatusArray = klageJson.get("ventestatus")
            klageVentestatusArray.size() shouldBe 1
            klageVentestatusArray[0].also { hendelse ->
                hendelse.get("sattPåVentAv").asString() shouldBe saksbehandler.navIdent
                hendelse.sladdbarVerdi("begrunnelse").asString() shouldBe "begrunnelse for å sette klage på vent"
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
                hendelse.get("sattPåVentAv").asString() shouldBe saksbehandler.navIdent
                hendelse.sladdbarVerdi("begrunnelse").asString() shouldBe "begrunnelse for å sette klage på vent"
                hendelse.get("erSattPåVent").asBoolean() shouldBe true
                hendelse.get("status").asString() shouldBe "UNDER_BEHANDLING"
                hendelse.get("frist").asString() shouldBe "2025-01-14"
            }
        }
    }

    @Test
    fun `kan sette klagebehandling med meldekortbehandling på vent fra meldekortbehandling`() {
        val clock = TikkendeKlokke(fixedClockAt(1.januar(2025)))
        withTestApplicationContextAndPostgres(clock = clock) { tac ->
            val saksbehandler = ObjectMother.saksbehandler("saksbehandlerKlagebehandling")
            val (_, oppdatertMeldekortbehandling, sakJson) = meldekortbehandlingMedKlagebehandlingSattPåVentFraMeldekortRoute(
                tac = tac,
                saksbehandler = saksbehandler,
            )!!
            val oppdatertKlagebehandling = requireNotNull(oppdatertMeldekortbehandling.klagebehandling)

            oppdatertKlagebehandling.status shouldBe Klagebehandlingsstatus.KLAR_TIL_BEHANDLING
            oppdatertKlagebehandling.saksbehandler shouldBe null
            oppdatertKlagebehandling.ventestatus.erSattPåVent shouldBe true

            oppdatertMeldekortbehandling.status shouldBe MeldekortbehandlingStatus.KLAR_TIL_BEHANDLING
            oppdatertMeldekortbehandling.saksbehandler shouldBe null
            oppdatertMeldekortbehandling.ventestatus.erSattPåVent shouldBe true

            val klageJson = sakJson.get("klagebehandlinger").first()
            klageJson.get("status").asString() shouldBe "KLAR_TIL_BEHANDLING"
            klageJson.get("saksbehandler").isNull shouldBe true
            val klageVentestatusArray = klageJson.get("ventestatus")
            klageVentestatusArray.size() shouldBe 1
            klageVentestatusArray[0].also { hendelse ->
                hendelse.get("sattPåVentAv").asString() shouldBe saksbehandler.navIdent
                hendelse.sladdbarVerdi("begrunnelse").asString() shouldBe "begrunnelse for å sette klage på vent"
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
                hendelse.get("sattPåVentAv").asString() shouldBe saksbehandler.navIdent
                hendelse.sladdbarVerdi("begrunnelse").asString() shouldBe "begrunnelse for å sette klage på vent"
                hendelse.get("erSattPåVent").asBoolean() shouldBe true
                hendelse.get("status").asString() shouldBe "UNDER_BEHANDLING"
                hendelse.get("frist").asString() shouldBe "2025-01-14"
            }
        }
    }

    @Test
    fun `saksbehandler kan sette meldekortbehandling med klagebehandling på vent fra UNDER_BEHANDLING`() {
        val clock = TikkendeKlokke(fixedClockAt(1.januar(2025)))
        withTestApplicationContextAndPostgres(clock = clock) { tac ->
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
                hendelse.sladdbarVerdi("begrunnelse").asString() shouldBe "begrunnelse for å sette klage på vent"
                hendelse.get("erSattPåVent").asBoolean() shouldBe true
                hendelse.get("status").asString() shouldBe "UNDER_BEHANDLING"
                hendelse.get("frist").asString() shouldBe "2025-01-14"
            }

            val klageJson = sakJson.get("klagebehandlinger").first()
            klageJson.get("status").asString() shouldBe "KLAR_TIL_BEHANDLING"
            klageJson.get("saksbehandler").isNull shouldBe true
            val klageVentestatusArray = klageJson.get("ventestatus")
            klageVentestatusArray.size() shouldBe 1
            klageVentestatusArray[0].also { hendelse ->
                hendelse.get("sattPåVentAv").asString() shouldBe saksbehandler.navIdent
                hendelse.sladdbarVerdi("begrunnelse").asString() shouldBe "begrunnelse for å sette klage på vent"
                hendelse.get("erSattPåVent").asBoolean() shouldBe true
                hendelse.get("status").asString() shouldBe "UNDER_BEHANDLING"
                hendelse.get("frist").asString() shouldBe "2025-01-14"
            }
        }
    }

    @Test
    fun `beslutter kan sette meldekortbehandling med klagebehandling på vent fra UNDER_BESLUTNING`() {
        val clock = TikkendeKlokke(fixedClockAt(1.januar(2025)))
        withTestApplicationContextAndPostgres(clock = clock) { tac ->
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
                hendelse.sladdbarVerdi("begrunnelse").asString() shouldBe "begrunnelse for å sette meldekort på vent"
                hendelse.get("erSattPåVent").asBoolean() shouldBe true
                hendelse.get("status").asString() shouldBe "UNDER_BESLUTNING"
                hendelse.get("frist").asString() shouldBe "2025-01-14"
            }

            val klageJson = sakJson.get("klagebehandlinger").first()
            klageJson.get("status").asString() shouldBe "KLAR_TIL_BEHANDLING"
            klageJson.get("saksbehandler").isNull shouldBe false
            val klageVentestatusArray = klageJson.get("ventestatus")
            klageVentestatusArray.size() shouldBe 1
            klageVentestatusArray[0].also { hendelse ->
                hendelse.get("sattPåVentAv").asString() shouldBe beslutter.navIdent
                hendelse.sladdbarVerdi("begrunnelse").asString() shouldBe "begrunnelse for å sette meldekort på vent"
                hendelse.get("erSattPåVent").asBoolean() shouldBe true
                hendelse.get("status").asString() shouldBe "UNDER_BEHANDLING"
                hendelse.get("frist").asString() shouldBe "2025-01-14"
            }
        }
    }

    @Test
    fun `saksbehandler kan oppdatere meldekortbehandling med klagebehandling som er klar til behandling og satt på vent`() {
        val clock = TikkendeKlokke(fixedClockAt(1.januar(2025)))
        withTestApplicationContextAndPostgres(clock = clock) { tac ->
            val saksbehandler = ObjectMother.saksbehandler("saksbehandlerKlagebehandling")
            val annenSaksbehandler = ObjectMother.saksbehandler("annenSaksbehandler")
            val (sak, meldekortPåVent) = meldekortbehandlingMedKlagebehandlingSattPåVentFraMeldekortRoute(
                tac = tac,
                saksbehandler = saksbehandler,
            )!!

            val (_, oppdatertMeldekortbehandling) = settMeldekortbehandlingPåVent(
                tac = tac,
                sakId = sak.id,
                meldekortId = meldekortPåVent.id,
                saksbehandlerEllerBeslutter = annenSaksbehandler,
                begrunnelse = "Oppdatert begrunnelse",
                frist = 21.januar(2025),
                oppdaterPåVent = true,
            )!!
            val oppdatertKlagebehandling = requireNotNull(oppdatertMeldekortbehandling.klagebehandling)

            oppdatertMeldekortbehandling.status shouldBe MeldekortbehandlingStatus.KLAR_TIL_BEHANDLING
            oppdatertMeldekortbehandling.saksbehandler shouldBe null
            oppdatertMeldekortbehandling.ventestatus.ventestatusHendelser.size shouldBe 2
            oppdatertMeldekortbehandling.ventestatus.ventestatusHendelser.last().let { hendelse ->
                hendelse.endretAv shouldBe annenSaksbehandler.navIdent
                hendelse.begrunnelse shouldBe "Oppdatert begrunnelse"
                hendelse.frist shouldBe 21.januar(2025)
                hendelse.status shouldBe "KLAR_TIL_BEHANDLING"
            }

            oppdatertKlagebehandling.status shouldBe Klagebehandlingsstatus.KLAR_TIL_BEHANDLING
            oppdatertKlagebehandling.saksbehandler shouldBe null
            oppdatertKlagebehandling.ventestatus.ventestatusHendelser.size shouldBe 2
            oppdatertKlagebehandling.ventestatus.ventestatusHendelser.last().let { hendelse ->
                hendelse.endretAv shouldBe annenSaksbehandler.navIdent
                hendelse.begrunnelse shouldBe "Oppdatert begrunnelse"
                hendelse.frist shouldBe 21.januar(2025)
                hendelse.erSattPåVent shouldBe true
                hendelse.status shouldBe "KLAR_TIL_BEHANDLING"
            }
        }
    }

    @Test
    fun `beslutter kan oppdatere meldekortbehandling med klagebehandling som er klar til beslutning og satt på vent`() {
        val clock = TikkendeKlokke(fixedClockAt(1.januar(2025)))
        withTestApplicationContextAndPostgres(clock = clock) { tac ->
            val saksbehandler = ObjectMother.saksbehandler("saksbehandlerKlagebehandling")
            val beslutter = ObjectMother.beslutter("beslutter")
            val annenBeslutter = ObjectMother.beslutter("annenBeslutter")
            val (sak, meldekortPåVent) = meldekortbehandlingUnderBeslutningMedKlagebehandlingSattPåVentFraMeldekortRoute(
                tac = tac,
                saksbehandler = saksbehandler,
                beslutter = beslutter,
            )!!

            val (_, oppdatertMeldekortbehandling) = settMeldekortbehandlingPåVent(
                tac = tac,
                sakId = sak.id,
                meldekortId = meldekortPåVent.id,
                saksbehandlerEllerBeslutter = annenBeslutter,
                begrunnelse = "Oppdatert begrunnelse fra beslutter",
                frist = 21.januar(2025),
                oppdaterPåVent = true,
            )!!
            val oppdatertKlagebehandling = requireNotNull(oppdatertMeldekortbehandling.klagebehandling)

            oppdatertMeldekortbehandling.status shouldBe MeldekortbehandlingStatus.KLAR_TIL_BESLUTNING
            oppdatertMeldekortbehandling.saksbehandler shouldBe saksbehandler.navIdent
            oppdatertMeldekortbehandling.beslutter shouldBe null
            oppdatertMeldekortbehandling.ventestatus.ventestatusHendelser.size shouldBe 2
            oppdatertMeldekortbehandling.ventestatus.ventestatusHendelser.last().let { hendelse ->
                hendelse.endretAv shouldBe annenBeslutter.navIdent
                hendelse.begrunnelse shouldBe "Oppdatert begrunnelse fra beslutter"
                hendelse.frist shouldBe 21.januar(2025)
                hendelse.status shouldBe "KLAR_TIL_BESLUTNING"
            }

            oppdatertKlagebehandling.status shouldBe Klagebehandlingsstatus.KLAR_TIL_BEHANDLING
            oppdatertKlagebehandling.saksbehandler shouldBe saksbehandler.navIdent
            oppdatertKlagebehandling.ventestatus.ventestatusHendelser.size shouldBe 2
            oppdatertKlagebehandling.ventestatus.ventestatusHendelser.last().let { hendelse ->
                hendelse.endretAv shouldBe annenBeslutter.navIdent
                hendelse.begrunnelse shouldBe "Oppdatert begrunnelse fra beslutter"
                hendelse.frist shouldBe 21.januar(2025)
                hendelse.erSattPåVent shouldBe true
                hendelse.status shouldBe "KLAR_TIL_BEHANDLING"
            }
        }
    }

    @Test
    fun `kan oppdatere begrunnelse og frist på klagebehandling med meldekortbehandling fra klagebehandlingen`() {
        val clock = TikkendeKlokke(fixedClockAt(1.januar(2025)))
        withTestApplicationContextAndPostgres(clock = clock) { tac ->
            val annenSaksbehandler = ObjectMother.saksbehandler("annenSaksbehandler")
            val (sak, meldekortPåVent, _) = meldekortbehandlingMedKlageSattPåVentFraKlageRoute(
                tac = tac,
            )!!
            val klagebehandlingId = meldekortPåVent.klagebehandling!!.id

            val (oppdatertSak, oppdatertKlagebehandling, sakJson) = settKlagebehandlingPåVent(
                tac = tac,
                sakId = sak.id,
                klagebehandlingId = klagebehandlingId,
                saksbehandler = annenSaksbehandler,
                begrunnelse = "Oppdatert begrunnelse",
                frist = 21.januar(2025),
                oppdaterSattPåVent = true,
            )!!
            val oppdatertMeldekortbehandling = oppdatertSak.hentMeldekortbehandling(meldekortPåVent.id)!!

            oppdatertMeldekortbehandling.status shouldBe MeldekortbehandlingStatus.KLAR_TIL_BEHANDLING
            oppdatertMeldekortbehandling.saksbehandler shouldBe null
            oppdatertKlagebehandling.status shouldBe Klagebehandlingsstatus.KLAR_TIL_BEHANDLING
            oppdatertKlagebehandling.saksbehandler shouldBe null

            sakJson.get("meldekortbehandlinger").get(meldekortPåVent.id.toString()).shouldHaSisteVentestatus(
                sattPåVentAv = annenSaksbehandler.navIdent,
                begrunnelse = "Oppdatert begrunnelse",
                status = "KLAR_TIL_BEHANDLING",
                frist = 21.januar(2025),
                forventetAntallHendelser = 2,
            )
            sakJson.klagebehandlingJson(klagebehandlingId).shouldHaSisteVentestatus(
                sattPåVentAv = annenSaksbehandler.navIdent,
                begrunnelse = "Oppdatert begrunnelse",
                status = "KLAR_TIL_BEHANDLING",
                frist = 21.januar(2025),
                forventetAntallHendelser = 2,
            )
        }
    }

    @Test
    fun `kan ikke oppdatere klagebehandling når tilknyttet meldekortbehandling ikke er satt på vent`() {
        val clock = TikkendeKlokke(fixedClockAt(1.januar(2025)))
        withTestApplicationContextAndPostgres(clock = clock) { tac ->
            val saksbehandler = ObjectMother.saksbehandler("saksbehandlerKlagebehandling")
            val (sak, klagebehandling, meldekortbehandling) = opprettMeldekortbehandlingForKlage(
                tac = tac,
                saksbehandlerKlagebehandling = saksbehandler,
            )

            settKlagebehandlingPåVent(
                tac = tac,
                sakId = sak.id,
                klagebehandlingId = klagebehandling.id,
                saksbehandler = saksbehandler,
                oppdaterSattPåVent = true,
                forventet = ForventetRespons.json(
                    400,
                    """
                    {
                      "melding": "Behandlingen må være satt på vent for å kunne oppdateres.",
                      "kode": "må_være_satt_på_vent"
                    }
                    """.trimIndent(),
                    "application/json; charset=UTF-8",
                ),
            ) shouldBe null

            val lagretSak = tac.sakContext.sakRepo.hentForSakId(sak.id)!!
            lagretSak.hentMeldekortbehandling(meldekortbehandling.id)!!.ventestatus.ventestatusHendelser shouldBe emptyList()
            lagretSak.hentKlagebehandling(klagebehandling.id).ventestatus.ventestatusHendelser shouldBe emptyList()
        }
    }

    @Test
    fun `saksbehandler uten beslutterrolle kan ikke oppdatere klagebehandling når tilknyttet meldekortbehandling er klar til beslutning`() {
        val clock = TikkendeKlokke(fixedClockAt(1.januar(2025)))
        withTestApplicationContextAndPostgres(clock = clock) { tac ->
            val saksbehandler = ObjectMother.saksbehandler("saksbehandlerKlagebehandling")
            val (sak, meldekortPåVent, _) = meldekortbehandlingUnderBeslutningMedKlagebehandlingSattPåVentFraMeldekortRoute(
                tac = tac,
                saksbehandler = saksbehandler,
            )!!
            val klagebehandlingId = meldekortPåVent.klagebehandling!!.id

            settKlagebehandlingPåVent(
                tac = tac,
                sakId = sak.id,
                klagebehandlingId = klagebehandlingId,
                saksbehandler = saksbehandler,
                begrunnelse = "Oppdatert begrunnelse",
                frist = 21.januar(2025),
                oppdaterSattPåVent = true,
                forventet = ForventetRespons.json(
                    403,
                    """
                    {
                      "melding": "Du må være beslutter for å sette denne behandlingen på vent.",
                      "kode": "maa_vaere_beslutter"
                    }
                    """.trimIndent(),
                    "application/json; charset=UTF-8",
                ),
            ) shouldBe null

            val lagretSak = tac.sakContext.sakRepo.hentForSakId(sak.id)!!
            lagretSak.hentMeldekortbehandling(meldekortPåVent.id)!!.ventestatus.ventestatusHendelser.size shouldBe 1
            lagretSak.hentKlagebehandling(klagebehandlingId).ventestatus.ventestatusHendelser.size shouldBe 1
        }
    }
}
