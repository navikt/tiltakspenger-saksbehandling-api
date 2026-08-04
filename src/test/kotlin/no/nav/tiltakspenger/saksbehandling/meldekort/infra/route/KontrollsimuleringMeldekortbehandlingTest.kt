package no.nav.tiltakspenger.saksbehandling.meldekort.infra.route

import io.kotest.matchers.nulls.shouldNotBeNull
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContext
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.beslutter
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.saksbehandler
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettMeldekortbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgOppdaterMeldekortbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.oppdaterMeldekortbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.sendMeldekortbehandlingTilBeslutning
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.taMeldekortbehanding
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.underkjennMeldekortbehandling
import org.junit.jupiter.api.Test

/**
 * En meldekortbehandling kan bli påvirket av andre vedtak på saken mens den står åpen.
 * Derfor kjøres en kontrollsimulering både ved send til beslutter og ved iverksettelse, og avvik fra simuleringen saksbehandler så på skal blokkere.
 */
class KontrollsimuleringMeldekortbehandlingTest {

    @Test
    fun `kontrollen følger med gjennom underkjenning og ny oppdatering`() {
        withTestApplicationContextAndPostgres { tac ->
            val (sak, _, _, meldekortbehandling) = iverksettSøknadsbehandlingOgOppdaterMeldekortbehandling(
                tac = tac,
                saksbehandler = saksbehandler("saksbehandler"),
            )!!

            sendMeldekortbehandlingTilBeslutning(
                tac = tac,
                sakId = sak.id,
                meldekortId = meldekortbehandling.id,
                saksbehandler = saksbehandler("saksbehandler"),
            )!!

            taMeldekortbehanding(
                tac = tac,
                sakId = sak.id,
                meldekortId = meldekortbehandling.id,
                saksbehandlerEllerBeslutter = beslutter("beslutter"),
            )!!

            underkjennMeldekortbehandling(
                tac = tac,
                sakId = sak.id,
                meldekortId = meldekortbehandling.id,
                beslutter = beslutter("beslutter"),
            )!!

            // Kontrollen fra forrige runde skal fortsatt ligge på behandlingen etter at saksbehandler jobber videre.
            oppdaterMeldekortbehandling(
                tac = tac,
                sakId = sak.id,
                meldekortId = meldekortbehandling.id,
                saksbehandler = saksbehandler("saksbehandler"),
            )!!

            tac.sakContext.sakRepo.hentForSakId(sak.id)!!
                .hentMeldekortbehandling(meldekortbehandling.id)!!
                .utbetalingskontroll.shouldNotBeNull()
        }
    }

    @Test
    fun `kontrollsimuleringen blokkerer ikke når ingenting har endret seg`() {
        withTestApplicationContext { tac ->
            val (sak, _, _, meldekortbehandling) = iverksettSøknadsbehandlingOgOppdaterMeldekortbehandling(
                tac = tac,
                saksbehandler = saksbehandler("saksbehandler"),
            )!!

            sendMeldekortbehandlingTilBeslutning(
                tac = tac,
                sakId = sak.id,
                meldekortId = meldekortbehandling.id,
                saksbehandler = saksbehandler("saksbehandler"),
            )!!

            tac.sakContext.sakRepo.hentForSakId(sak.id)!!
                .hentMeldekortbehandling(meldekortbehandling.id)!!
                .utbetalingskontroll.shouldNotBeNull()

            taMeldekortbehanding(
                tac = tac,
                sakId = sak.id,
                meldekortId = meldekortbehandling.id,
                saksbehandlerEllerBeslutter = beslutter("beslutter"),
            )!!

            iverksettMeldekortbehandling(
                tac = tac,
                sakId = sak.id,
                meldekortId = meldekortbehandling.id,
                beslutter = beslutter("beslutter"),
            )!!
        }
    }
}
