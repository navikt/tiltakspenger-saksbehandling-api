package no.nav.tiltakspenger.saksbehandling.klage.infra.route.oppdater

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.common.NonBlankString
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContext
import no.nav.tiltakspenger.saksbehandling.klage.domene.brev.Brevtekster
import no.nav.tiltakspenger.saksbehandling.klage.domene.brev.TittelOgTekst
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSakOgOppdaterKlagebehandlingBrevtekst
import org.junit.jupiter.api.Test

class OppdaterKlagebehandlingBrevtekstRouteTest {
    @Test
    fun `kan oppdatere klagebehandling - brevtekst`() {
        withTestApplicationContext { tac ->
            opprettSakOgOppdaterKlagebehandlingBrevtekst(
                tac = tac,
            )!!.second.also {
                it.brevtekst!! shouldBe Brevtekster(
                    listOf(
                        TittelOgTekst(
                            tittel = NonBlankString.create("Avvisning av klage"),
                            tekst = NonBlankString.create("Din klage er dessverre avvist."),
                        ),
                    ),
                )
            }
        }
    }
}
