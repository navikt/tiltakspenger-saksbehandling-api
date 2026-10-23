package no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.settPåVent

import arrow.core.left
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.ktor.test.common.ForventetRespons
import no.nav.tiltakspenger.saksbehandling.behandling.service.sak.SakService
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContext
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.felles.Ventestatus
import no.nav.tiltakspenger.saksbehandling.felles.VentestatusHendelse
import no.nav.tiltakspenger.saksbehandling.infra.route.shouldBeEqualToIgnoringLocalDateTime
import no.nav.tiltakspenger.saksbehandling.infra.route.shouldHaSisteVentestatus
import no.nav.tiltakspenger.saksbehandling.infra.route.sladdbarVerdi
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldekortbehandlingStatus
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.settPåVent.KanIkkeSetteMeldekortbehandlingPåVent
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.settPåVent.SettMeldekortbehandlingPåVentKommando
import no.nav.tiltakspenger.saksbehandling.meldekort.service.SettMeldekortbehandlingPåVentService
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.angreMeldekortbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgBeslutterTarBehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgOpprettMeldekortbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOpprettMeldekortbehandlingOgSettPåVent
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingSendMeldekortbehandlingTilBeslutningTaBehandlingOgSettPåVent
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.settMeldekortbehandlingPåVent
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class SettMeldekortbehandlingPåVentRouteTest {

    @Test
    fun `beslutter kan ikke overskrive angring ved å sette meldekortbehandling på vent`() {
        withTestApplicationContextAndPostgres { tac ->
            val saksbehandler = ObjectMother.saksbehandler("saksbehandler")
            val beslutter = ObjectMother.beslutter("beslutter")
            val (sak, _, _, meldekortbehandling) = iverksettSøknadsbehandlingOgBeslutterTarBehandling(
                tac = tac,
                saksbehandler = saksbehandler,
                beslutter = beslutter,
            )!!
            val sakUnderBeslutning = tac.sakContext.sakService.hentForSakId(sak.id)

            val (_, angretBehandling) = angreMeldekortbehandling(
                tac = tac,
                sakId = sak.id,
                meldekortId = meldekortbehandling.id,
                saksbehandler = saksbehandler,
            )!!
            angretBehandling!!.status shouldBe MeldekortbehandlingStatus.UNDER_BEHANDLING

            val sakService = mockk<SakService>()
            every { sakService.hentForSakId(sak.id) } returns sakUnderBeslutning
            val service = SettMeldekortbehandlingPåVentService(
                meldekortbehandlingRepo = tac.meldekortContext.meldekortbehandlingRepo,
                sakService = sakService,
                clock = tac.clock,
                sessionFactory = tac.sessionFactory,
                statistikkService = tac.statistikkContext.statistikkService,
            )

            service.settPåVent(
                SettMeldekortbehandlingPåVentKommando(
                    sakId = sak.id,
                    meldekortId = meldekortbehandling.id,
                    oppdaterPåVent = false,
                    begrunnelse = "Beslutter setter på vent",
                    frist = null,
                    saksbehandler = beslutter,
                    correlationId = CorrelationId.generate(),
                ),
            ) shouldBe KanIkkeSetteMeldekortbehandlingPåVent.BehandlingenErIkkeLengerUnderBeslutning.left()

            tac.meldekortContext.meldekortbehandlingRepo.hent(meldekortbehandling.id) shouldBe angretBehandling
        }
    }

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

            val meldekortJson = json.get("meldekortbehandlinger").get(meldekortbehandling.id.toString())
            meldekortJson.get("status").asString() shouldBe "KLAR_TIL_BEHANDLING"
            meldekortJson.get("saksbehandler").isNull shouldBe true
            val ventestatusArray = meldekortJson.get("ventestatus")
            ventestatusArray.size() shouldBe 1
            ventestatusArray[0].also { hendelse ->
                hendelse.get("sattPåVentAv").asString() shouldBe "Z12345"
                hendelse.sladdbarVerdi("begrunnelse").asString() shouldBe "Begrunnelse for å sette meldekortbehandling på vent"
                hendelse.get("erSattPåVent").asBoolean() shouldBe true
                hendelse.get("status").asString() shouldBe "UNDER_BEHANDLING"
                hendelse.get("frist").asString() shouldBe "2026-01-01"
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

            val meldekortJson = json.get("meldekortbehandlinger").get(meldekortbehandling.id.toString())
            meldekortJson.get("status").asString() shouldBe "KLAR_TIL_BESLUTNING"
            meldekortJson.get("beslutter").isNull shouldBe true
            val ventestatusArray = meldekortJson.get("ventestatus")
            ventestatusArray.size() shouldBe 1
            ventestatusArray[0].also { hendelse ->
                hendelse.get("sattPåVentAv").asString() shouldBe "beslutter"
                hendelse.sladdbarVerdi("begrunnelse").asString() shouldBe "Begrunnelse for å sette meldekortbehandling på vent"
                hendelse.get("erSattPåVent").asBoolean() shouldBe true
                hendelse.get("status").asString() shouldBe "UNDER_BESLUTNING"
                hendelse.get("frist").asString() shouldBe "2026-01-01"
            }
        }
    }

    @Test
    fun `kan oppdatere begrunnelse og frist på en meldekortbehandling som er klar til behandling og satt på vent`() {
        withTestApplicationContext { tac ->
            val (sak, _, _, meldekortPåVent) = iverksettSøknadsbehandlingOpprettMeldekortbehandlingOgSettPåVent(tac = tac)!!
            val annenSaksbehandler = ObjectMother.saksbehandler123()

            val (_, oppdatertMeldekortbehandling, json) = settMeldekortbehandlingPåVent(
                tac = tac,
                sakId = sak.id,
                meldekortId = meldekortPåVent.id,
                saksbehandlerEllerBeslutter = annenSaksbehandler,
                begrunnelse = "Oppdatert begrunnelse",
                frist = 15.januar(2026),
                oppdaterPåVent = true,
            )!!

            oppdatertMeldekortbehandling.status shouldBe MeldekortbehandlingStatus.KLAR_TIL_BEHANDLING
            oppdatertMeldekortbehandling.saksbehandler shouldBe null
            oppdatertMeldekortbehandling.ventestatus.erSattPåVent shouldBe true
            oppdatertMeldekortbehandling.ventestatus.ventestatusHendelser.size shouldBe 2
            oppdatertMeldekortbehandling.ventestatus.ventestatusHendelser.last().let { hendelse ->
                hendelse.endretAv shouldBe annenSaksbehandler.navIdent
                hendelse.begrunnelse shouldBe "Oppdatert begrunnelse"
                hendelse.frist shouldBe 15.januar(2026)
                hendelse.erSattPåVent shouldBe true
                hendelse.status shouldBe "KLAR_TIL_BEHANDLING"
            }

            val meldekortJson = json.get("meldekortbehandlinger").get(oppdatertMeldekortbehandling.id.toString())
            meldekortJson.get("status").asString() shouldBe "KLAR_TIL_BEHANDLING"
            meldekortJson.get("saksbehandler").isNull shouldBe true
            meldekortJson.shouldHaSisteVentestatus(
                sattPåVentAv = annenSaksbehandler.navIdent,
                begrunnelse = "Oppdatert begrunnelse",
                status = "KLAR_TIL_BEHANDLING",
                frist = 15.januar(2026),
                forventetAntallHendelser = 2,
            )
        }
    }

    @Test
    fun `kan oppdatere begrunnelse og frist på en meldekortbehandling som er klar til beslutning og satt på vent`() {
        withTestApplicationContext { tac ->
            val annenBeslutter = ObjectMother.beslutter("annenBeslutter")
            val (sak, _, _, meldekortPåVent) = iverksettSøknadsbehandlingSendMeldekortbehandlingTilBeslutningTaBehandlingOgSettPåVent(tac = tac)!!

            settMeldekortbehandlingPåVent(
                tac = tac,
                sakId = sak.id,
                meldekortId = meldekortPåVent.id,
                saksbehandlerEllerBeslutter = annenBeslutter,
                begrunnelse = "Oppdatert begrunnelse fra beslutter",
                frist = 15.januar(2026),
                oppdaterPåVent = true,
            )!!

            tac.meldekortContext.meldekortbehandlingRepo.hent(meldekortPåVent.id)!!.also {
                it.status shouldBe MeldekortbehandlingStatus.KLAR_TIL_BESLUTNING
                it.saksbehandler shouldBe "saksbehandler"
                it.beslutter shouldBe null
                it.ventestatus.ventestatusHendelser.size shouldBe 2
                it.ventestatus.ventestatusHendelser.last().let { hendelse ->
                    hendelse.endretAv shouldBe annenBeslutter.navIdent
                    hendelse.begrunnelse shouldBe "Oppdatert begrunnelse fra beslutter"
                    hendelse.frist shouldBe 15.januar(2026)
                    hendelse.erSattPåVent shouldBe true
                    hendelse.status shouldBe "KLAR_TIL_BESLUTNING"
                }
            }
        }
    }

    @Test
    fun `kan ikke oppdatere en meldekortbehandling som ikke er satt på vent`() {
        withTestApplicationContext { tac ->
            val (sak, _, _, meldekortbehandling) = iverksettSøknadsbehandlingOgOpprettMeldekortbehandling(tac = tac)!!

            settMeldekortbehandlingPåVent(
                tac = tac,
                sakId = sak.id,
                meldekortId = meldekortbehandling.id,
                saksbehandlerEllerBeslutter = ObjectMother.saksbehandler(),
                oppdaterPåVent = true,
                forventet = ForventetRespons.json(
                    400,
                    """
                    {
                      "melding": "Meldekortbehandlingen må være satt på vent for å kunne oppdateres.",
                      "kode": "må_være_satt_på_vent"
                    }
                    """.trimIndent(),
                    "application/json; charset=UTF-8",
                ),
            ) shouldBe null

            tac.meldekortContext.meldekortbehandlingRepo.hent(meldekortbehandling.id)!!.also {
                it.status shouldBe MeldekortbehandlingStatus.UNDER_BEHANDLING
                it.ventestatus.erSattPåVent shouldBe false
            }
        }
    }

    @Test
    fun `kan ikke sette en meldekortbehandling på vent på nytt uten å oppdatere`() {
        withTestApplicationContext { tac ->
            val (sak, _, _, meldekortPåVent) = iverksettSøknadsbehandlingOpprettMeldekortbehandlingOgSettPåVent(tac = tac)!!

            settMeldekortbehandlingPåVent(
                tac = tac,
                sakId = sak.id,
                meldekortId = meldekortPåVent.id,
                saksbehandlerEllerBeslutter = ObjectMother.saksbehandler(),
                oppdaterPåVent = false,
                forventet = ForventetRespons.json(
                    400,
                    """
                    {
                      "melding": "Meldekortbehandlingen er allerede satt på vent.",
                      "kode": "behandlingen_er_allerede_paa_vent"
                    }
                    """.trimIndent(),
                    "application/json; charset=UTF-8",
                ),
            ) shouldBe null

            tac.meldekortContext.meldekortbehandlingRepo.hent(meldekortPåVent.id)!!.ventestatus.ventestatusHendelser.size shouldBe 1
        }
    }

    @Test
    fun `en bruker uten saksbehandlerrolle kan ikke oppdatere en meldekortbehandling som er klar til behandling og satt på vent`() {
        withTestApplicationContext { tac ->
            val (sak, _, _, meldekortPåVent) = iverksettSøknadsbehandlingOpprettMeldekortbehandlingOgSettPåVent(tac = tac)!!

            settMeldekortbehandlingPåVent(
                tac = tac,
                sakId = sak.id,
                meldekortId = meldekortPåVent.id,
                saksbehandlerEllerBeslutter = ObjectMother.beslutter(),
                oppdaterPåVent = true,
                forventet = ForventetRespons.json(
                    403,
                    """
                    {
                      "melding": "Du må være saksbehandler for å sette denne meldekortbehandlingen på vent.",
                      "kode": "maa_vaere_saksbehandler"
                    }
                    """.trimIndent(),
                    "application/json; charset=UTF-8",
                ),
            ) shouldBe null

            tac.meldekortContext.meldekortbehandlingRepo.hent(meldekortPåVent.id)!!.ventestatus.ventestatusHendelser.size shouldBe 1
        }
    }

    @Test
    fun `en bruker uten beslutterrolle kan ikke oppdatere en meldekortbehandling som er klar til beslutning og satt på vent`() {
        withTestApplicationContext { tac ->
            val (sak, _, _, meldekortPåVent) = iverksettSøknadsbehandlingSendMeldekortbehandlingTilBeslutningTaBehandlingOgSettPåVent(tac = tac)!!

            settMeldekortbehandlingPåVent(
                tac = tac,
                sakId = sak.id,
                meldekortId = meldekortPåVent.id,
                saksbehandlerEllerBeslutter = ObjectMother.saksbehandler(),
                oppdaterPåVent = true,
                forventet = ForventetRespons.json(
                    403,
                    """
                    {
                      "melding": "Du må være beslutter for å sette denne meldekortbehandlingen på vent.",
                      "kode": "maa_vaere_beslutter"
                    }
                    """.trimIndent(),
                    "application/json; charset=UTF-8",
                ),
            ) shouldBe null

            tac.meldekortContext.meldekortbehandlingRepo.hent(meldekortPåVent.id)!!.ventestatus.ventestatusHendelser.size shouldBe 1
        }
    }
}
