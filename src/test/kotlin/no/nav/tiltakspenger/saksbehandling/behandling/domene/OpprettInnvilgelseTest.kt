package no.nav.tiltakspenger.saksbehandling.behandling.domene

import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.dato.juni
import no.nav.tiltakspenger.libs.periodisering.til
import no.nav.tiltakspenger.libs.periodisering.toDTO
import no.nav.tiltakspenger.saksbehandling.barnetillegg.Barnetillegg
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.barnetillegg.toBarnetilleggDTO
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.OppdaterSøknadsbehandlingDTO
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContext
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.oppdaterBehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSøknadsbehandlingUnderBehandling
import no.nav.tiltakspenger.saksbehandling.søknad.infra.repo.SøknadPostgresRepoTest
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.route.TiltaksdeltakelsePeriodeDTO
import org.junit.jupiter.api.Test

class OpprettInnvilgelseTest {
    val innvilgelsesperiode = 1.januar(2025) til 30.juni(2025)

    @Test
    fun `lol test`() {
        withTestApplicationContext { tac ->
            val (sak, _, behandling) = opprettSøknadsbehandlingUnderBehandling(
                tac = tac,
            )

//            oppdaterBehandling(
//                tac = tac,
//                sakId = sak.id,
//                behandlingId = behandling.id,
//                oppdaterBehandlingDTO = OppdaterSøknadsbehandlingDTO.Innvilgelse(
//                    innvilgelsesperiode = innvilgelsesperiode.toDTO(),
//                    valgteTiltaksdeltakelser = listOf(
//                        TiltaksdeltakelsePeriodeDTO(
//                            eksternDeltagelseId = tiltaksdeltakelse.eksternDeltakelseId,
//                            periode = innvilgelsesperiode.toDTO(),
//                        )
//                    ),
//                    barnetillegg = Barnetillegg.utenBarnetillegg(innvilgelsesperiode).toBarnetilleggDTO(),
//                    antallDagerPerMeldeperiodeForPerioder = listOf(),
//                    fritekstTilVedtaksbrev = null,
//                    begrunnelseVilkårsvurdering = null,
//                ),
//            )
        }
    }
}
