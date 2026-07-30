package no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.leggTilbake

import io.kotest.matchers.nulls.shouldNotBeNull
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContext
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOpprettMeldekortbehandlingOgLeggTilbake
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingSendMeldekortbehandlingTilBeslutningTaBehandlingOgLeggTilbake
import org.junit.jupiter.api.Test

class LeggTilbakeMeldekortbehandlingRouteTest {

    @Test
    fun `saksbehandler kan legge tilbake meldekortbehandling`() {
        withTestApplicationContext { tac ->
            iverksettSøknadsbehandlingOpprettMeldekortbehandlingOgLeggTilbake(
                tac = tac,
            ).shouldNotBeNull()
        }
    }

    /**
     * Kjøres mot postgres fordi den er grunnsettet for [no.nav.tiltakspenger.saksbehandling.meldekort.infra.repo.MeldekortbehandlingPostgresRepo.leggTilbakeBehandlingBeslutter].
     * Ingen annen test treffer den spørringen.
     */
    @Test
    fun `beslutter kan legge tilbake meldekortbehandling`() {
        withTestApplicationContextAndPostgres { tac ->
            iverksettSøknadsbehandlingSendMeldekortbehandlingTilBeslutningTaBehandlingOgLeggTilbake(
                tac = tac,
            ).shouldNotBeNull()
        }
    }
}
