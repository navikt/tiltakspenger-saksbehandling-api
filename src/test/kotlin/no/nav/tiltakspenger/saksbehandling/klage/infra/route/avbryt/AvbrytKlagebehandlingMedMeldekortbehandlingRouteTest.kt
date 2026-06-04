package no.nav.tiltakspenger.saksbehandling.klage.infra.route.avbryt

import io.kotest.matchers.shouldBe
import io.ktor.http.HttpStatusCode
import no.nav.tiltakspenger.libs.common.TikkendeKlokke
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.fixedClockAt
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsstatus
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldekortbehandlingStatus
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.avbruttMeldekortbehandlingMedAvbruttKlagebehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.avbrytKlagebehandlingForSak
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettMeldekortbehandlingForKlage
import org.junit.jupiter.api.Test

class AvbrytKlagebehandlingMedMeldekortbehandlingRouteTest {

    @Test
    fun `kan avbryte klagebehandling med meldekortbehandling etter at meldekortbehandlingen er avbrutt`() {
        val clock = TikkendeKlokke(fixedClockAt(1.januar(2025)))
        withTestApplicationContextAndPostgres(clock = clock, runIsolated = true) { tac ->
            val saksbehandler = ObjectMother.saksbehandler("saksbehandlerKlagebehandling")

            val (_, avbruttMeldekortbehandling, avbruttKlagebehandling) =
                avbruttMeldekortbehandlingMedAvbruttKlagebehandling(tac = tac, saksbehandlerKlagebehandling = saksbehandler)!!

            avbruttMeldekortbehandling.status shouldBe MeldekortbehandlingStatus.AVBRUTT
            avbruttMeldekortbehandling.klagebehandling?.status shouldBe Klagebehandlingsstatus.AVBRUTT

            avbruttKlagebehandling.status shouldBe Klagebehandlingsstatus.AVBRUTT
            avbruttKlagebehandling.åpenBehandlingId shouldBe null
        }
    }

    @Test
    fun `kan ikke avbryte klagebehandling når meldekortbehandling fortsatt er åpen`() {
        val clock = TikkendeKlokke(fixedClockAt(1.januar(2025)))
        withTestApplicationContextAndPostgres(clock = clock, runIsolated = true) { tac ->
            val saksbehandler = ObjectMother.saksbehandler("saksbehandlerKlagebehandling")

            val (sak, klagebehandling, meldekortbehandling) =
                opprettMeldekortbehandlingForKlage(tac = tac, saksbehandlerKlagebehandling = saksbehandler)

            avbrytKlagebehandlingForSak(
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
