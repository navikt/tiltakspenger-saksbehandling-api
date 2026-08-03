package no.nav.tiltakspenger.saksbehandling.klage.infra.repo

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSakOgIverksettKlagebehandling
import org.junit.jupiter.api.Test

/**
 * Et klagevedtak lagres først uten journalpost og distribusjon, og oppdateres av jobbene etterpå.
 *
 * Rutetestene kjører jobbene som en del av byggerne, så tilstanden mellom iverksettelse og journalføring blir aldri observert der.
 * Den er likevel den som ligger i databasen i vinduet mellom de to, og en feil i skrivingen av nullene ville ikke vist seg før jobben kom og skrev over.
 */
class KlagevedtakPostgresRepoTest {

    @Test
    fun `et nytt klagevedtak lagres uten journalpost og distribusjon`() {
        withTestApplicationContextAndPostgres { tac ->
            val (sak, klagevedtak) = opprettSakOgIverksettKlagebehandling(tac = tac, utførJobber = false)!!

            klagevedtak.journalpostId shouldBe null
            klagevedtak.distribusjonId shouldBe null

            // Lest tilbake fra databasen, ikke bare fra objektet ruta returnerte.
            val lagret = tac.sakContext.sakRepo.hentForSakId(sak.id)!!
                .klagevedtaksliste.single { it.id == klagevedtak.id }
            lagret.journalpostId shouldBe null
            lagret.distribusjonId shouldBe null
            lagret.journalføringstidspunkt shouldBe null
            lagret.distribusjonstidspunkt shouldBe null
        }
    }
}
