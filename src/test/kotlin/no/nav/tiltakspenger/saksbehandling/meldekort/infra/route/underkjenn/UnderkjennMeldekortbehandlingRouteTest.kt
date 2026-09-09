package no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.underkjenn

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContext
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldekortbehandlingStatus
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.beslutter
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.saksbehandler
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgUnderkjennMeldekortbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.sendMeldekortbehandlingTilBeslutning
import org.junit.jupiter.api.Test

class UnderkjennMeldekortbehandlingRouteTest {
    @Test
    fun `saksbehandler kan underkjenne meldekortbehandling`() {
        withTestApplicationContext { tac ->
            this.iverksettSøknadsbehandlingOgUnderkjennMeldekortbehandling(
                tac = tac,
            )!!
        }
    }

    /**
     * Kjører mot postgres for å verifisere at beslutteren på en underkjent behandling lagres og leses tilbake.
     */
    @Test
    fun `underkjent meldekortbehandling beholder beslutter og går rett tilbake til samme beslutter`() {
        withTestApplicationContextAndPostgres { tac ->
            val (sak, _, _, meldekortbehandling, _) = this.iverksettSøknadsbehandlingOgUnderkjennMeldekortbehandling(
                tac = tac,
            )!!
            meldekortbehandling.beslutter shouldBe beslutter("beslutter").navIdent

            val (_, sendtPåNytt, _) = this.sendMeldekortbehandlingTilBeslutning(
                tac = tac,
                sakId = sak.id,
                meldekortId = meldekortbehandling.id,
                saksbehandler = saksbehandler("saksbehandler"),
            )!!

            sendtPåNytt.status shouldBe MeldekortbehandlingStatus.UNDER_BESLUTNING
            sendtPåNytt.beslutter shouldBe beslutter("beslutter").navIdent
        }
    }
}
