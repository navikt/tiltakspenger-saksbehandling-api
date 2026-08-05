package no.nav.tiltakspenger.saksbehandling.klage.infra.route.forhåndsvis

import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.avbruttKlagebehandlng
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.forhåndsvisKlagebehandlingsbrev
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettMeldekortvedtakOgForhåndsvisKlagebehandlingTilAvvisningsbrev
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.leggKlagebehandlingTilbake
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSakOgForhåndsvisKlagebehandlingTilAvvisningsbrev
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSakOgKlagebehandlingTilAvvisning
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSakOgOppdaterKlagebehandlingTilOpprettholdelseBrevtekst
import org.junit.jupiter.api.Test

class ForhåndsvisBrevKlagebehandlingRouteTest {
    @Test
    fun `kan forhåndsvise klagebehandling til avvisning `() {
        withTestApplicationContextAndPostgres { tac ->
            opprettSakOgForhåndsvisKlagebehandlingTilAvvisningsbrev(
                tac = tac,
            )!!
        }
    }

    @Test
    fun `kan forhåndsvise klagebehandling til opprettholdelse`() {
        withTestApplicationContextAndPostgres { tac ->
            val (sak, _, klagebehandling) = opprettSakOgOppdaterKlagebehandlingTilOpprettholdelseBrevtekst(
                tac = tac,
            )!!
            forhåndsvisKlagebehandlingsbrev(
                tac = tac,
                sakId = sak.id,
                klagebehandlingId = klagebehandling.id,
            )
        }
    }

    @Test
    fun `kan forhåndsvise klagebehandling som er lagt tilbake og ikke har saksbehandler`() {
        withTestApplicationContextAndPostgres { tac ->
            val saksbehandler = ObjectMother.saksbehandler("saksbehandlerKlagebehandling")
            val (sak, klagebehandling, _) = opprettSakOgKlagebehandlingTilAvvisning(
                tac = tac,
                saksbehandler = saksbehandler,
            )!!
            leggKlagebehandlingTilbake(
                tac = tac,
                sakId = sak.id,
                klagebehandlingId = klagebehandling.id,
                saksbehandler = saksbehandler,
            )!!
            forhåndsvisKlagebehandlingsbrev(
                tac = tac,
                sakId = sak.id,
                klagebehandlingId = klagebehandling.id,
                saksbehandler = ObjectMother.saksbehandler("annenSaksbehandler"),
            )!!
        }
    }

    @Test
    fun `kan forhåndsvise opprettholdelse som er lagt tilbake og ikke har saksbehandler`() {
        withTestApplicationContextAndPostgres { tac ->
            val saksbehandler = ObjectMother.saksbehandler("saksbehandlerKlagebehandling")
            val (sak, _, klagebehandling) = opprettSakOgOppdaterKlagebehandlingTilOpprettholdelseBrevtekst(
                tac = tac,
                saksbehandler = saksbehandler,
            )!!
            leggKlagebehandlingTilbake(
                tac = tac,
                sakId = sak.id,
                klagebehandlingId = klagebehandling.id,
                saksbehandler = saksbehandler,
            )!!
            forhåndsvisKlagebehandlingsbrev(
                tac = tac,
                sakId = sak.id,
                klagebehandlingId = klagebehandling.id,
                saksbehandler = ObjectMother.saksbehandler("annenSaksbehandler"),
            )!!
        }
    }

    @Test
    fun `kan forhåndsvise avbrutt klagebehandling`() {
        withTestApplicationContextAndPostgres { tac ->
            val (sak, klagebehandling, _) = avbruttKlagebehandlng(
                tac = tac,
            )!!
            forhåndsvisKlagebehandlingsbrev(
                tac = tac,
                sakId = sak.id,
                klagebehandlingId = klagebehandling.id,
                saksbehandler = ObjectMother.saksbehandler("annenSaksbehandler"),
            )!!
        }
    }

    @Test
    fun `kan forhåndsvise klagebehandling til avvisning der vedtak er utbetalingsvedtak`() {
        withTestApplicationContextAndPostgres { tac ->
            iverksettMeldekortvedtakOgForhåndsvisKlagebehandlingTilAvvisningsbrev(
                tac = tac,
            )!!
        }
    }
}
