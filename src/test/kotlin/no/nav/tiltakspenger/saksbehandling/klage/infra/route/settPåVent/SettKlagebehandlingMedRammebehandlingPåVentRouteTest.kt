package no.nav.tiltakspenger.saksbehandling.klage.infra.route.settPåVent

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.TikkendeKlokke
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.ktor.test.common.ForventetRespons
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.felles.Ventestatus
import no.nav.tiltakspenger.saksbehandling.felles.VentestatusHendelse
import no.nav.tiltakspenger.saksbehandling.fixedClockAt
import no.nav.tiltakspenger.saksbehandling.infra.route.klagebehandlingJson
import no.nav.tiltakspenger.saksbehandling.infra.route.rammebehandlingJson
import no.nav.tiltakspenger.saksbehandling.infra.route.shouldBeEqualToIgnoringLocalDateTime
import no.nav.tiltakspenger.saksbehandling.infra.route.shouldHaSisteVentestatus
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsresultat
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsstatus
import no.nav.tiltakspenger.saksbehandling.klage.domene.hentKlagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.infra.route.shouldBeFerdigstiltOpprettholdtKlagebehandlingDTO
import no.nav.tiltakspenger.saksbehandling.klage.infra.route.shouldBeKlagebehandlingDTO
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgSettKlagebehandlingMedRammebehandlingPåVent
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgSettRammebehandlingMedKlagebehandlingPåVentFraUnderBeslutning
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettetSøknadsbehandlingForKlage
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.rammebehandlingMedFerdigstiltOpprettholdtKlage
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.settKlagebehandlingPåVent
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.settRammebehandlingPåVent
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

class SettKlagebehandlingMedRammebehandlingPåVentRouteTest {
    @Test
    fun `kan sette klagebehandling med rammebehandling på vent`() {
        val clock = TikkendeKlokke(fixedClockAt(1.januar(2025)))
        withTestApplicationContextAndPostgres(clock = clock) { tac ->
            val (sak, rammebehandlingMedKlagebehandling, json) = iverksettSøknadsbehandlingOgSettKlagebehandlingMedRammebehandlingPåVent(
                tac = tac,
            )!!
            val klagebehandling = rammebehandlingMedKlagebehandling.klagebehandling!!
            json.get("klagebehandlinger").first().toString().shouldBeKlagebehandlingDTO(
                sakId = sak.id,
                saksnummer = sak.saksnummer,
                klagebehandlingId = klagebehandling.id,
                fnr = sak.fnr.verdi,
                saksbehandler = null,
                status = "KLAR_TIL_BEHANDLING",
                resultat = "OMGJØR",
                vedtakDetKlagesPå = "${sak.rammevedtaksliste.first().id}",
                behandlingDetKlagesPå = "${sak.rammevedtaksliste.first().behandlingId}",
                årsak = "PROSESSUELL_FEIL",
                begrunnelse = "Begrunnelse for omgjøring",
                behandlingId = listOf(rammebehandlingMedKlagebehandling.id.toString()),
                åpenBehandlingId = rammebehandlingMedKlagebehandling.id.toString(),
                //language=json
                ventestatus = listOf("""{"sattPåVentAv": "saksbehandlerKlagebehandling","status": "UNDER_BEHANDLING","tidspunkt": "TIMESTAMP","begrunnelse": {"verdi": "begrunnelse for å sette klage på vent", "erSladdet": false},"erSattPåVent": true,"frist": "2025-01-14"}"""),
            )
            rammebehandlingMedKlagebehandling.status shouldBe Rammebehandlingsstatus.KLAR_TIL_BEHANDLING
            rammebehandlingMedKlagebehandling.saksbehandler shouldBe null
            rammebehandlingMedKlagebehandling.ventestatus.shouldBeEqualToIgnoringLocalDateTime(
                Ventestatus(
                    listOf(
                        VentestatusHendelse(
                            tidspunkt = LocalDateTime.MIN,
                            endretAv = "saksbehandlerKlagebehandling",
                            begrunnelse = "begrunnelse for å sette klage på vent",
                            erSattPåVent = true,
                            status = "UNDER_BEHANDLING",
                            frist = LocalDate.parse("2025-01-14"),
                        ),
                    ),
                ),
            )
        }
    }

    @Test
    fun `kan sette rammebehandling tilknyttet klage på vent`() {
        val clock = TikkendeKlokke(fixedClockAt(1.januar(2025)))
        withTestApplicationContextAndPostgres(clock = clock) { tac ->
            val saksbehandler: Saksbehandler = ObjectMother.saksbehandler("saksbehandlerKlagebehandling")
            val (sak, rammebehandlingMedKlagebehandling, _) = opprettetSøknadsbehandlingForKlage(
                tac = tac,
                saksbehandlerKlagebehandling = saksbehandler,
            )!!
            val (_, _, oppdatertRammebehandlingMedKlagebehandling, sakJson) = settRammebehandlingPåVent(
                tac = tac,
                sakId = sak.id,
                rammebehandlingId = rammebehandlingMedKlagebehandling.id,
                saksbehandler = saksbehandler,
                frist = null,
            )!!
            val klagebehandling = oppdatertRammebehandlingMedKlagebehandling.klagebehandling!!
            sakJson.rammebehandlingJson(oppdatertRammebehandlingMedKlagebehandling.id).also { behandlingJson ->
                behandlingJson.get("status").asString() shouldBe "KLAR_TIL_BEHANDLING"
                behandlingJson.get("saksbehandler").isNull shouldBe true
                behandlingJson.shouldHaSisteVentestatus(
                    sattPåVentAv = "saksbehandlerKlagebehandling",
                    begrunnelse = "Begrunnelse for å sette rammebehandling på vent",
                    status = "UNDER_BEHANDLING",
                    frist = null,
                    forventetAntallHendelser = 1,
                )
            }
            sakJson.klagebehandlingJson(klagebehandling.id).also { klageJson ->
                klageJson.get("status").asString() shouldBe "KLAR_TIL_BEHANDLING"
                klageJson.get("saksbehandler").isNull shouldBe true
                klageJson.shouldHaSisteVentestatus(
                    sattPåVentAv = "saksbehandlerKlagebehandling",
                    begrunnelse = "Begrunnelse for å sette rammebehandling på vent",
                    status = "UNDER_BEHANDLING",
                    frist = null,
                    forventetAntallHendelser = 1,
                )
            }
            klagebehandling.status shouldBe Klagebehandlingsstatus.KLAR_TIL_BEHANDLING
            klagebehandling.saksbehandler shouldBe null
            klagebehandling.ventestatus.shouldBeEqualToIgnoringLocalDateTime(
                Ventestatus(
                    listOf(
                        VentestatusHendelse(
                            tidspunkt = LocalDateTime.MIN,
                            endretAv = "saksbehandlerKlagebehandling",
                            begrunnelse = "Begrunnelse for å sette rammebehandling på vent",
                            erSattPåVent = true,
                            status = "UNDER_BEHANDLING",
                            frist = null,
                        ),
                    ),
                ),
            )
        }
    }

    @Test
    fun `en rammebehandling med klagebehandling ferdigstilt setter kun rammebehandlingen på vent`() {
        val clock = TikkendeKlokke(fixedClockAt(1.januar(2025)))
        withTestApplicationContextAndPostgres(clock = clock) { tac ->
            val saksbehandler = ObjectMother.saksbehandler("saksbehandlerKlagebehandling")
            val (sak, rammebehandling) = rammebehandlingMedFerdigstiltOpprettholdtKlage(
                tac = tac,
                type = "REVURDERING_OMGJØRING",
                saksbehandler = saksbehandler,
            )!!

            val (_, _, rammebehandlingPåVent, sakJson) = settRammebehandlingPåVent(
                tac = tac,
                sakId = sak.id,
                rammebehandlingId = rammebehandling.id,
                saksbehandler = saksbehandler,
            )!!

            val klagebehandlingJson = sakJson.klagebehandlingJson(rammebehandling.klagebehandling!!.id)

            sakJson.rammebehandlingJson(rammebehandling.id).shouldHaSisteVentestatus(
                sattPåVentAv = "saksbehandlerKlagebehandling",
                begrunnelse = "Begrunnelse for å sette rammebehandling på vent",
                status = "UNDER_BEHANDLING",
                frist = null,
                forventetAntallHendelser = 1,
            )

            klagebehandlingJson.toString().shouldBeFerdigstiltOpprettholdtKlagebehandlingDTO(
                sakId = sak.id,
                saksnummer = sak.saksnummer,
                klagebehandlingId = rammebehandling.klagebehandling!!.id,
                fnr = sak.fnr.verdi,
                resultat = rammebehandlingPåVent.klagebehandling!!.resultat as Klagebehandlingsresultat.Opprettholdt,
                behandlingDetKlagesPå = "${sak.rammevedtaksliste.first().behandlingId}",
                behandlingId = listOf(rammebehandling.id.toString()),
                åpenBehandlingId = rammebehandling.id.toString(),
                vedtakDetKlagesPå = rammebehandling.klagebehandling!!.formkrav.vedtakDetKlagesPå!!.toString(),
            )
        }
    }

    @Test
    fun `beslutter kan sette rammebehandling med klagebehandling på vent fra UNDER_BESLUTNING`() {
        val clock = TikkendeKlokke(fixedClockAt(1.januar(2025)))
        withTestApplicationContextAndPostgres(clock = clock) { tac ->
            val saksbehandler = ObjectMother.saksbehandler("saksbehandlerKlagebehandling")
            val beslutter = ObjectMother.beslutter("beslutter")
            val (_, oppdatertRammebehandling, sakJson) =
                iverksettSøknadsbehandlingOgSettRammebehandlingMedKlagebehandlingPåVentFraUnderBeslutning(
                    tac = tac,
                    saksbehandler = saksbehandler,
                    beslutter = beslutter,
                )!!
            val oppdatertKlagebehandling = requireNotNull(oppdatertRammebehandling.klagebehandling)

            oppdatertRammebehandling.status shouldBe Rammebehandlingsstatus.KLAR_TIL_BESLUTNING
            oppdatertRammebehandling.saksbehandler shouldBe saksbehandler.navIdent
            oppdatertRammebehandling.beslutter shouldBe null
            oppdatertRammebehandling.ventestatus.erSattPåVent shouldBe true
            oppdatertRammebehandling.ventestatus.shouldBeEqualToIgnoringLocalDateTime(
                Ventestatus(
                    listOf(
                        VentestatusHendelse(
                            tidspunkt = LocalDateTime.MIN,
                            endretAv = beslutter.navIdent,
                            begrunnelse = "begrunnelse for å sette rammebehandling på vent",
                            erSattPåVent = true,
                            status = "UNDER_BESLUTNING",
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
                            endretAv = beslutter.navIdent,
                            begrunnelse = "begrunnelse for å sette rammebehandling på vent",
                            erSattPåVent = true,
                            status = "UNDER_BEHANDLING",
                            frist = 14.januar(2025),
                        ),
                    ),
                ),
            )

            sakJson.rammebehandlingJson(oppdatertRammebehandling.id).also { behandlingJson ->
                behandlingJson.get("status").asString() shouldBe "KLAR_TIL_BESLUTNING"
                behandlingJson.get("saksbehandler").asString() shouldBe saksbehandler.navIdent
                behandlingJson.get("beslutter").isNull shouldBe true
                behandlingJson.shouldHaSisteVentestatus(
                    sattPåVentAv = beslutter.navIdent,
                    begrunnelse = "begrunnelse for å sette rammebehandling på vent",
                    status = "UNDER_BESLUTNING",
                    frist = 14.januar(2025),
                    forventetAntallHendelser = 1,
                )
            }

            sakJson.klagebehandlingJson(oppdatertKlagebehandling.id).also { klageJson ->
                klageJson.get("status").asString() shouldBe "KLAR_TIL_BEHANDLING"
                klageJson.get("saksbehandler").isNull shouldBe true
                klageJson.shouldHaSisteVentestatus(
                    sattPåVentAv = beslutter.navIdent,
                    begrunnelse = "begrunnelse for å sette rammebehandling på vent",
                    status = "UNDER_BEHANDLING",
                    frist = 14.januar(2025),
                    forventetAntallHendelser = 1,
                )
            }
        }
    }

    @Test
    fun `kan oppdatere begrunnelse og frist på rammebehandling med klagebehandling som er klar til behandling og satt på vent`() {
        val clock = TikkendeKlokke(fixedClockAt(1.januar(2025)))
        withTestApplicationContextAndPostgres(clock = clock) { tac ->
            val saksbehandler = ObjectMother.saksbehandler("saksbehandlerKlagebehandling")
            val annenSaksbehandler = ObjectMother.saksbehandler("annenSaksbehandler")
            val (sak, rammebehandling, _) = opprettetSøknadsbehandlingForKlage(
                tac = tac,
                saksbehandlerKlagebehandling = saksbehandler,
            )!!
            settRammebehandlingPåVent(
                tac = tac,
                sakId = sak.id,
                rammebehandlingId = rammebehandling.id,
                saksbehandler = saksbehandler,
            )!!

            val (_, _, oppdatertRammebehandling, sakJson) = settRammebehandlingPåVent(
                tac = tac,
                sakId = sak.id,
                rammebehandlingId = rammebehandling.id,
                saksbehandler = annenSaksbehandler,
                begrunnelse = "Oppdatert begrunnelse",
                frist = 21.januar(2025),
                oppdaterSattPåVent = true,
            )!!
            val oppdatertKlagebehandling = requireNotNull(oppdatertRammebehandling.klagebehandling)

            oppdatertRammebehandling.status shouldBe Rammebehandlingsstatus.KLAR_TIL_BEHANDLING
            oppdatertRammebehandling.saksbehandler shouldBe null
            oppdatertKlagebehandling.status shouldBe Klagebehandlingsstatus.KLAR_TIL_BEHANDLING
            oppdatertKlagebehandling.saksbehandler shouldBe null

            sakJson.rammebehandlingJson(oppdatertRammebehandling.id).shouldHaSisteVentestatus(
                sattPåVentAv = annenSaksbehandler.navIdent,
                begrunnelse = "Oppdatert begrunnelse",
                status = "KLAR_TIL_BEHANDLING",
                frist = 21.januar(2025),
                forventetAntallHendelser = 2,
            )
            sakJson.klagebehandlingJson(oppdatertKlagebehandling.id).shouldHaSisteVentestatus(
                sattPåVentAv = annenSaksbehandler.navIdent,
                begrunnelse = "Oppdatert begrunnelse",
                status = "KLAR_TIL_BEHANDLING",
                frist = 21.januar(2025),
                forventetAntallHendelser = 2,
            )
        }
    }

    @Test
    fun `kan oppdatere begrunnelse og frist på rammebehandling med klagebehandling som er klar til beslutning og satt på vent`() {
        val clock = TikkendeKlokke(fixedClockAt(1.januar(2025)))
        withTestApplicationContextAndPostgres(clock = clock) { tac ->
            val saksbehandler = ObjectMother.saksbehandler("saksbehandlerKlagebehandling")
            val annenBeslutter = ObjectMother.beslutter("annenBeslutter")
            val (sak, rammebehandling, _) =
                iverksettSøknadsbehandlingOgSettRammebehandlingMedKlagebehandlingPåVentFraUnderBeslutning(
                    tac = tac,
                    saksbehandler = saksbehandler,
                )!!

            val (_, _, oppdatertRammebehandling, sakJson) = settRammebehandlingPåVent(
                tac = tac,
                sakId = sak.id,
                rammebehandlingId = rammebehandling.id,
                saksbehandler = annenBeslutter,
                begrunnelse = "Oppdatert begrunnelse fra beslutter",
                frist = 21.januar(2025),
                oppdaterSattPåVent = true,
            )!!
            val oppdatertKlagebehandling = requireNotNull(oppdatertRammebehandling.klagebehandling)

            oppdatertRammebehandling.status shouldBe Rammebehandlingsstatus.KLAR_TIL_BESLUTNING
            oppdatertRammebehandling.saksbehandler shouldBe saksbehandler.navIdent
            oppdatertRammebehandling.beslutter shouldBe null
            oppdatertKlagebehandling.status shouldBe Klagebehandlingsstatus.KLAR_TIL_BEHANDLING
            oppdatertKlagebehandling.saksbehandler shouldBe null

            sakJson.rammebehandlingJson(oppdatertRammebehandling.id).shouldHaSisteVentestatus(
                sattPåVentAv = annenBeslutter.navIdent,
                begrunnelse = "Oppdatert begrunnelse fra beslutter",
                status = "KLAR_TIL_BESLUTNING",
                frist = 21.januar(2025),
                forventetAntallHendelser = 2,
            )
            sakJson.klagebehandlingJson(oppdatertKlagebehandling.id).shouldHaSisteVentestatus(
                sattPåVentAv = annenBeslutter.navIdent,
                begrunnelse = "Oppdatert begrunnelse fra beslutter",
                status = "KLAR_TIL_BEHANDLING",
                frist = 21.januar(2025),
                forventetAntallHendelser = 2,
            )
        }
    }

    @Test
    fun `kan oppdatere begrunnelse og frist på klagebehandling med rammebehandling fra klagebehandlingen`() {
        val clock = TikkendeKlokke(fixedClockAt(1.januar(2025)))
        withTestApplicationContextAndPostgres(clock = clock) { tac ->
            val annenSaksbehandler = ObjectMother.saksbehandler("annenSaksbehandler")
            val (sak, rammebehandling, _) = iverksettSøknadsbehandlingOgSettKlagebehandlingMedRammebehandlingPåVent(
                tac = tac,
            )!!
            val klagebehandlingId = rammebehandling.klagebehandling!!.id

            val (oppdatertSak, oppdatertKlagebehandling, sakJson) = settKlagebehandlingPåVent(
                tac = tac,
                sakId = sak.id,
                klagebehandlingId = klagebehandlingId,
                saksbehandler = annenSaksbehandler,
                begrunnelse = "Oppdatert begrunnelse",
                frist = 21.januar(2025),
                oppdaterSattPåVent = true,
            )!!
            val oppdatertRammebehandling = oppdatertSak.hentRammebehandling(rammebehandling.id)!!

            oppdatertRammebehandling.status shouldBe Rammebehandlingsstatus.KLAR_TIL_BEHANDLING
            oppdatertRammebehandling.saksbehandler shouldBe null
            oppdatertKlagebehandling.status shouldBe Klagebehandlingsstatus.KLAR_TIL_BEHANDLING
            oppdatertKlagebehandling.saksbehandler shouldBe null

            sakJson.rammebehandlingJson(rammebehandling.id).shouldHaSisteVentestatus(
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
    fun `kan ikke oppdatere klagebehandling når tilknyttet rammebehandling ikke er satt på vent`() {
        val clock = TikkendeKlokke(fixedClockAt(1.januar(2025)))
        withTestApplicationContextAndPostgres(clock = clock) { tac ->
            val saksbehandler = ObjectMother.saksbehandler("saksbehandlerKlagebehandling")
            val (sak, rammebehandling, _) = opprettetSøknadsbehandlingForKlage(
                tac = tac,
                saksbehandlerKlagebehandling = saksbehandler,
            )!!

            settKlagebehandlingPåVent(
                tac = tac,
                sakId = sak.id,
                klagebehandlingId = rammebehandling.klagebehandling!!.id,
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
            lagretSak.hentRammebehandling(rammebehandling.id)!!.ventestatus.ventestatusHendelser shouldBe emptyList()
            lagretSak.hentKlagebehandling(rammebehandling.klagebehandling!!.id).ventestatus.ventestatusHendelser shouldBe emptyList()
        }
    }

    @Test
    fun `saksbehandler uten beslutterrolle kan ikke oppdatere klagebehandling når tilknyttet rammebehandling er klar til beslutning`() {
        val clock = TikkendeKlokke(fixedClockAt(1.januar(2025)))
        withTestApplicationContextAndPostgres(clock = clock) { tac ->
            val saksbehandler = ObjectMother.saksbehandler("saksbehandlerKlagebehandling")
            val (sak, rammebehandling, _) =
                iverksettSøknadsbehandlingOgSettRammebehandlingMedKlagebehandlingPåVentFraUnderBeslutning(
                    tac = tac,
                    saksbehandler = saksbehandler,
                )!!

            settKlagebehandlingPåVent(
                tac = tac,
                sakId = sak.id,
                klagebehandlingId = rammebehandling.klagebehandling!!.id,
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
            lagretSak.hentRammebehandling(rammebehandling.id)!!.ventestatus.ventestatusHendelser.size shouldBe 1
            lagretSak.hentKlagebehandling(rammebehandling.klagebehandling!!.id).ventestatus.ventestatusHendelser.size shouldBe 1
        }
    }
}
