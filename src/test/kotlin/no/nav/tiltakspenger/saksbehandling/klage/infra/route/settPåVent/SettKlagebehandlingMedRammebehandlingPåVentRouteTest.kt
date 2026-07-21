package no.nav.tiltakspenger.saksbehandling.klage.infra.route.settPåVent

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.TikkendeKlokke
import no.nav.tiltakspenger.libs.dato.januar
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
import no.nav.tiltakspenger.saksbehandling.klage.infra.route.shouldBeFerdigstiltOpprettholdtKlagebehandlingDTO
import no.nav.tiltakspenger.saksbehandling.klage.infra.route.shouldBeKlagebehandlingDTO
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgSettKlagebehandlingMedRammebehandlingPåVent
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgSettRammebehandlingMedKlagebehandlingPåVentFraUnderBeslutning
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettetSøknadsbehandlingForKlage
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.rammebehandlingMedFerdigstiltOpprettholdtKlage
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.settRammebehandlingPåVent
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

class SettKlagebehandlingMedRammebehandlingPåVentRouteTest {
    @Test
    fun `kan sette klagebehandling med rammebehandling på vent`() {
        val clock = TikkendeKlokke(fixedClockAt(1.januar(2025)))
        withTestApplicationContextAndPostgres(clock = clock, runIsolated = true) { tac ->
            val (sak, rammebehandlingMedKlagebehandling, json) = iverksettSøknadsbehandlingOgSettKlagebehandlingMedRammebehandlingPåVent(
                tac = tac,
            )!!
            val klagebehandling = rammebehandlingMedKlagebehandling.klagebehandling!!
            json.get("klageBehandlinger").first().toString().shouldBeKlagebehandlingDTO(
                sakId = sak.id,
                saksnummer = sak.saksnummer,
                klagebehandlingId = klagebehandling.id,
                fnr = "12345678911",
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
                ventestatus = listOf("""{"sattPåVentAv": "saksbehandlerKlagebehandling","status": "UNDER_BEHANDLING","tidspunkt": "TIMESTAMP","begrunnelse": "begrunnelse for å sette klage på vent","erSattPåVent": true,"frist": "2025-01-14"}"""),
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
        withTestApplicationContextAndPostgres(clock = clock, runIsolated = true) { tac ->
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
        withTestApplicationContextAndPostgres(clock = clock, runIsolated = true) { tac ->
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
                fnr = "12345678911",
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
        withTestApplicationContextAndPostgres(clock = clock, runIsolated = true) { tac ->
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
}
