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
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgGjenopptaKlagebehandlingMedMeldekortbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgGjenopptaMeldekortbehandlingForKlage
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class GjenopptaKlagebehandlingMedMeldekortbehandlingRouteTest {

    @Test
    fun `kan gjenoppta klagebehandling med meldekortbehandling`() {
        val clock = TikkendeKlokke(fixedClockAt(1.januar(2025)))
        withTestApplicationContextAndPostgres(clock = clock, runIsolated = true) { tac ->
            val saksbehandler = ObjectMother.saksbehandler("saksbehandlerKlagebehandling")
            val (_, oppdatertMeldekortbehandling, _) = iverksettSøknadsbehandlingOgGjenopptaKlagebehandlingMedMeldekortbehandling(
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
    fun `kan gjenoppta meldekortbehandling tilknyttet klage`() {
        val clock = TikkendeKlokke(fixedClockAt(1.januar(2025)))
        withTestApplicationContextAndPostgres(clock = clock, runIsolated = true) { tac ->
            val saksbehandler = ObjectMother.saksbehandler("saksbehandlerKlagebehandling")
            val (_, oppdatertMeldekortbehandling, json) = iverksettSøknadsbehandlingOgGjenopptaMeldekortbehandlingForKlage(
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

            json.getString("status") shouldBe "UNDER_BEHANDLING"
            json.getString("saksbehandler") shouldBe saksbehandler.navIdent
        }
    }
}
