package no.nav.tiltakspenger.saksbehandling.klage.infra.route.avbryt

import io.kotest.matchers.shouldBe
import io.ktor.http.HttpStatusCode
import no.nav.tiltakspenger.libs.common.TikkendeKlokke
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.fixedClockAt
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsstatus
import no.nav.tiltakspenger.saksbehandling.klage.domene.hentKlagebehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldekortbehandlingStatus
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.avbrytKlagebehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgAvbrytKlagebehandlingMedMeldekortbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgOpprettMeldekortbehandlingForKlage
import org.junit.jupiter.api.Test

class AvbrytKlagebehandlingMedMeldekortbehandlingRouteTest {

    @Test
    fun `kan avbryte klagebehandling med meldekortbehandling etter at meldekortbehandlingen er avbrutt`() {
        val clock = TikkendeKlokke(fixedClockAt(1.januar(2025)))
        withTestApplicationContextAndPostgres(clock = clock, runIsolated = true) { tac ->
            val saksbehandler = ObjectMother.saksbehandler("saksbehandlerKlagebehandling")

            val (oppdatertSak, avbruttMeldekortbehandling, avbruttKlagebehandling) =
                iverksettSøknadsbehandlingOgAvbrytKlagebehandlingMedMeldekortbehandling(
                    tac = tac,
                    saksbehandlerKlagebehandling = saksbehandler,
                )!!

            // Klagebehandling fra returverdien er avbrutt
            avbruttKlagebehandling.status shouldBe Klagebehandlingsstatus.AVBRUTT
            // Etter avbryt er åpenBehandlingId nullstilt (fjernBehandlingId setter den til null)
            avbruttKlagebehandling.åpenBehandlingId shouldBe null

            // Meldekortbehandlingen er avbrutt
            avbruttMeldekortbehandling.status shouldBe MeldekortbehandlingStatus.AVBRUTT

            // Embedded klagebehandling i meldekortbehandling er AVBRUTT (lest fra DB etter avbryt klagebehandling)
            avbruttMeldekortbehandling.klagebehandling?.status shouldBe Klagebehandlingsstatus.AVBRUTT

            // Klagebehandling på saken er avbrutt
            val sakKlagebehandling = oppdatertSak.hentKlagebehandling(avbruttKlagebehandling.id)
            sakKlagebehandling.status shouldBe Klagebehandlingsstatus.AVBRUTT
            sakKlagebehandling.åpenBehandlingId shouldBe null
        }
    }

    @Test
    fun `kan ikke avbryte klagebehandling når meldekortbehandling fortsatt er åpen`() {
        val clock = TikkendeKlokke(fixedClockAt(1.januar(2025)))
        withTestApplicationContextAndPostgres(clock = clock, runIsolated = true) { tac ->
            val saksbehandler = ObjectMother.saksbehandler("saksbehandlerKlagebehandling")

            val (sak, klagebehandling, meldekortbehandling) =
                iverksettSøknadsbehandlingOgOpprettMeldekortbehandlingForKlage(
                    tac = tac,
                    saksbehandlerKlagebehandling = saksbehandler,
                )

            // Forsøk å avbryte klagebehandlingen uten å avbryte meldekortbehandlingen først
            avbrytKlagebehandling(
                tac = tac,
                sakId = sak.id,
                klagebehandlingId = klagebehandling.id,
                saksbehandler = saksbehandler,
                forventetStatus = HttpStatusCode.BadRequest,
                forventetJsonBody = {
                    """
                    {
                        "melding": "Klagebehandlingen kan ikke avbrytes fordi den er knyttet til en behandling som ikke er avbrutt: [${meldekortbehandling.id}]",
                        "kode": "knyttet_til_ikke_avbrutt_behandling"
                    }
                    """.trimIndent()
                },
            ) shouldBe null
        }
    }
}
