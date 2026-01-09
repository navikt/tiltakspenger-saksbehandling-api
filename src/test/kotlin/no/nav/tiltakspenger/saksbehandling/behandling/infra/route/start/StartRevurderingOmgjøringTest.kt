package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.start

import no.nav.tiltakspenger.libs.dato.februar
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.dato.mars
import no.nav.tiltakspenger.libs.periodisering.til
import no.nav.tiltakspenger.libs.periodisering.toDTO
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.InnvilgelsesperiodeDTO
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContext
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettRevurderingOmgjøring
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandling
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

    @Test
    fun `kan omgjøre selv om det er hull i meldeperiodene`() {
        // 1. Lag sak med innvilget periode februar 2025
        // 2. Send inn et meldekort for februar (vi kunne sendt inn alle og, skal ikke spille noen rolle)
        // 3. Omgjør forrige vedtak og utvid til og med januar-febuar. (merk at det her ikke blir hull)
        // 4. Send inn det første meldekortet i januar.
        // 5. Omgjør vedtaket igjen, nå med hull i meldeperiodene.

        withTestApplicationContext { tac ->
            val førsteInnvilgelsesperiode = 1.februar(2025) til 28.februar(2025)
            val omgjøringsperiode = 1.januar(2025) til 28.februar(2025)
            val (sak, søknad, rammevedtakSøknadsbehandling, _) = iverksettSøknadsbehandling(
                tac = tac,
                vedtaksperiode = førsteInnvilgelsesperiode,
                tiltaksdeltakelse = ObjectMother.tiltaksdeltakelseTac(
                    fom = omgjøringsperiode.fraOgMed,
                    tom = omgjøringsperiode.tilOgMed,
                ),
            )
            iverksettRevurderingOmgjøring(
                tac = tac,
                sakId = sak.id,
                rammevedtakIdSomOmgjøres = rammevedtakSøknadsbehandling.id,
                innvilgelsesperiode = omgjøringsperiode,
                innvilgelsesperioder = listOf(
                    InnvilgelsesperiodeDTO(
                        periode = omgjøringsperiode.toDTO(),
                        antallDagerPerMeldeperiode = 10,
                        tiltaksdeltakelseId = rammevedtakSøknadsbehandling.behandling.saksopplysninger.tiltaksdeltakelser.first().eksternDeltakelseId,
                        internDeltakelseId = rammevedtakSøknadsbehandling.behandling.saksopplysninger.tiltaksdeltakelser.first().internDeltakelseId?.toString(),
                    ),
                ),
            )
            // TODO jah: Fullfør etter vi har tilstrekkelig route-builder/verktøy i meldekort
        }
    }
}
