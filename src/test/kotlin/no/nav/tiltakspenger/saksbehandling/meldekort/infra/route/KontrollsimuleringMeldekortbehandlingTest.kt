package no.nav.tiltakspenger.saksbehandling.meldekort.infra.route

import io.kotest.matchers.nulls.shouldNotBeNull
import no.nav.tiltakspenger.libs.ktor.test.common.ForventetRespons
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContext
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.infra.route.harKode
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.beslutter
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.saksbehandler
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettMeldekortbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgOppdaterMeldekortbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.oppdaterMeldekortbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettOgIverksettMeldekortbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.sendMeldekortbehandlingTilBeslutning
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.taMeldekortbehanding
import org.junit.jupiter.api.Test

/**
 * Flere meldekortbehandlinger kan være åpne samtidig, og de påvirker hverandre.
 * Derfor kjøres en kontrollsimulering både ved send til beslutter og ved iverksettelse, og avvik fra simuleringen saksbehandler så på skal blokkere.
 */
class KontrollsimuleringMeldekortbehandlingTest {

    @Test
    fun `kan ikke sende til beslutter når en annen behandling på samme meldeperiode er iverksatt i mellomtiden`() {
        withTestApplicationContextAndPostgres { tac ->
            val (sak, _, _, meldekortbehandling) = iverksettSøknadsbehandlingOgOppdaterMeldekortbehandling(
                tac = tac,
                saksbehandler = saksbehandler("saksbehandler"),
            )!!

            opprettOgIverksettMeldekortbehandling(
                tac = tac,
                sakId = sak.id,
                kjedeId = sak.meldeperiodeKjeder.sisteMeldeperiodePerKjede.first().kjedeId,
            )!!

            sendMeldekortbehandlingTilBeslutning(
                tac = tac,
                sakId = sak.id,
                meldekortId = meldekortbehandling.id,
                saksbehandler = saksbehandler("saksbehandler"),
                forventet = ForventetRespons(409, contentType = "application/json; charset=UTF-8"),
            ) {
                it harKode "simulering_endret"
            }

            // Kontrollen lagres selv om behandlingen blir stående under behandling, slik at saksbehandler ser hva som avviker.
            tac.sakContext.sakRepo.hentForSakId(sak.id)!!
                .hentMeldekortbehandling(meldekortbehandling.id)!!
                .utbetalingskontroll.shouldNotBeNull()

            // Saksbehandler kan fortsatt jobbe videre på behandlingen, og kontrollen følger med gjennom oppdateringen.
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
    fun `kan ikke iverksette når en annen behandling på samme meldeperiode er iverksatt i mellomtiden`() {
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

            taMeldekortbehanding(
                tac = tac,
                sakId = sak.id,
                meldekortId = meldekortbehandling.id,
                saksbehandlerEllerBeslutter = beslutter("beslutter"),
            )!!

            opprettOgIverksettMeldekortbehandling(
                tac = tac,
                sakId = sak.id,
                kjedeId = sak.meldeperiodeKjeder.sisteMeldeperiodePerKjede.first().kjedeId,
                saksbehandler = saksbehandler("saksbehandler2"),
                beslutter = beslutter("beslutter2"),
            )!!

            iverksettMeldekortbehandling(
                tac = tac,
                sakId = sak.id,
                meldekortId = meldekortbehandling.id,
                beslutter = beslutter("beslutter"),
                forventet = ForventetRespons(409, contentType = "application/json; charset=UTF-8"),
            ) {
                it harKode "simulering_endret"
            }
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
