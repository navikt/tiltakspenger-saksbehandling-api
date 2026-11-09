package no.nav.tiltakspenger.saksbehandling.behandling.infra.repo

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Revurdering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.Omgjøringsresultat.OmgjøringInnvilgelse
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgStartRevurderingOmgjøring
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.oppdaterOmgjøringInnvilgelse
import org.junit.jupiter.api.Test

class RammebehandlingPostgresRepoOmgjøringTest {

    /**
     * Før innvilgelsen sluttet å nullstilles ved nye saksopplysninger, kunne en omgjøring bli lagret uten innvilgelsesperioder og barnetillegg.
     * Slike behandlinger kan fortsatt ligge under behandling, og må kunne leses og lagres.
     */
    @Test
    fun `omgjøring til innvilgelse uten innvilgelsesperioder og barnetillegg kan lagres og hentes`() {
        withTestApplicationContextAndPostgres { tac ->
            val (sak, _, søknadsvedtak, omgjøring) = iverksettSøknadsbehandlingOgStartRevurderingOmgjøring(tac)!!
            val (_, utfyltOmgjøring) = oppdaterOmgjøringInnvilgelse(
                tac = tac,
                sakId = sak.id,
                behandlingId = omgjøring.id,
                vedtaksperiode = søknadsvedtak.periode,
            )
            utfyltOmgjøring as Revurdering
            val nullstiltOmgjøring = utfyltOmgjøring.copy(
                resultat = (utfyltOmgjøring.resultat as OmgjøringInnvilgelse).copy(
                    innvilgelsesperioder = null,
                    barnetillegg = null,
                ),
            )
            val repo = tac.behandlingContext.rammebehandlingRepo

            repo.lagre(nullstiltOmgjøring)

            repo.hent(omgjøring.id) shouldBe nullstiltOmgjøring
        }
    }
}
