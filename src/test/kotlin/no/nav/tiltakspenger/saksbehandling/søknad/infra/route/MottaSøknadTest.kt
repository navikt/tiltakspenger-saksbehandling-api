package no.nav.tiltakspenger.saksbehandling.søknad.infra.route

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContext
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.hentEllerOpprettSakForSystembruker
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.mottaSøknad
import org.junit.jupiter.api.Test

internal class MottaSøknadTest {
    @Test
    fun `kan motta søknad`() {
        withTestApplicationContext { tac ->
            val fnr = Fnr.random()
            val saksnummer = hentEllerOpprettSakForSystembruker(tac, fnr)
            mottaSøknad(
                tac = tac,
                fnr = fnr,
                saksnummer = saksnummer,
            )
        }
    }
}
