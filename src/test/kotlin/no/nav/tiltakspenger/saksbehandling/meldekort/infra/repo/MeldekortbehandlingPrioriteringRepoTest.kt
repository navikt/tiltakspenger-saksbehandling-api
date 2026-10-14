package no.nav.tiltakspenger.saksbehandling.meldekort.infra.repo

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Prioritering
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgOpprettMeldekortbehandling
import org.junit.jupiter.api.Test

/**
 * Nye meldekortbehandlinger opprettes alltid uten prioritering, så prodstien lagrer aldri en prioritering ved insert.
 * Repoet skal likevel ikke miste en prioritering, så insert-siden øves ved å kalle repoet direkte.
 * Oppdatering-siden er dekket av rutetestene i `meldekort/infra/route/oppdater/`.
 */
class MeldekortbehandlingPrioriteringRepoTest {

    @Test
    fun `lagre tar vare på prioriteringen`() {
        withTestApplicationContextAndPostgres { tac ->
            val (_, _, _, meldekortbehandling) = iverksettSøknadsbehandlingOgOpprettMeldekortbehandling(tac = tac)!!
            val prioritert = meldekortbehandling.copy(
                id = MeldekortId.random(),
                prioritering = Prioritering.create("Bruker står uten inntekt"),
            )
            val repo = tac.meldekortContext.meldekortbehandlingRepo

            repo.lagre(prioritert, null, null)

            repo.hent(prioritert.id)!!.prioritering shouldBe Prioritering.create("Bruker står uten inntekt")
        }
    }
}
