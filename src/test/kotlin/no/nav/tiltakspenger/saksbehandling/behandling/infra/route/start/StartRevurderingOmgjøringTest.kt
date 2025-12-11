package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.start

import no.nav.tiltakspenger.libs.dato.februar
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.dato.mars
import no.nav.tiltakspenger.libs.periodisering.til
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContext
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettRevurderingOmgjøring
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgRevurderingStans
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

class StartRevurderingOmgjøringTest {
    @Disabled("TODO jah: Midlertidig deaktivert fordi omgjøring av stans ikke fungerer atm.")
    @Test
    fun `kan omgjøre stans`() {
        withTestApplicationContext { tac ->
            val førsteInnvilgelsesperiode = 1.januar(2025) til 31.mars(2025)
            val stansFraOgMedDato = 1.februar(2025)
            val (sak, søknad, rammevedtakSøknadsbehandling, rammevedtakRevurdering, _) = iverksettSøknadsbehandlingOgRevurderingStans(
                tac = tac,
                søknadsbehandlingInnvilgelsesperiode = førsteInnvilgelsesperiode,
                stansFraOgMed = stansFraOgMedDato,
            )
            iverksettRevurderingOmgjøring(
                tac = tac,
                sakId = sak.id,
                rammevedtakIdSomOmgjøres = rammevedtakRevurdering.id,
                innvilgelsesperiode = 1.februar(2025) til 31.mars(2025),
            )
        }
    }
}
