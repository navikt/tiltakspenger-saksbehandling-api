package no.nav.tiltakspenger.saksbehandling.klage.infra.route.forhåndsvis

import io.ktor.http.HttpStatusCode
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.forhåndsvisKlagebehandlingsbrev
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSakOgForhåndsvisKlagebehandlingTilAvvisningsbrev
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSakOgOppdaterKlagebehandlingTilOpprettholdelseBrevtekst
import org.junit.jupiter.api.Test

class ForhåndsvisBrevKlagebehandlingRouteTest {
    @Test
    fun `kan forhåndsvise klagebehandling til avvisning `() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            opprettSakOgForhåndsvisKlagebehandlingTilAvvisningsbrev(
                tac = tac,
            )!!
        }
    }

    @Test
    fun `kan ikke forhåndsvise klagebehandling til opprettholdelse fordi brevet er ikke implementert `() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            val (sak, _, klagebehandling) = opprettSakOgOppdaterKlagebehandlingTilOpprettholdelseBrevtekst(
                tac = tac,
            )!!
            forhåndsvisKlagebehandlingsbrev(
                tac = tac,
                sakId = sak.id,
                klagebehandlingId = klagebehandling.id,
                forventetStatus = HttpStatusCode.InternalServerError,
            )
        }
    }
}
