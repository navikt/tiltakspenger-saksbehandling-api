package no.nav.tiltakspenger.domene.behandling

import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.tiltakspenger.domene.saksopplysning.Kilde
import no.nav.tiltakspenger.domene.saksopplysning.TypeSaksopplysning
import no.nav.tiltakspenger.domene.vilkår.Vilkår
import no.nav.tiltakspenger.felles.Periode
import no.nav.tiltakspenger.felles.februar
import no.nav.tiltakspenger.felles.januar
import no.nav.tiltakspenger.felles.mars
import no.nav.tiltakspenger.objectmothers.ObjectMother.behandlingVilkårsvurdertInnvilget
import no.nav.tiltakspenger.objectmothers.ObjectMother.saksopplysning
import org.junit.jupiter.api.Test

internal class BehandlingVilkårsvurdertTest {

    @Test
    fun `sjekk at delvis innvilget blir innvilget`() {
        val foreldrepenger = saksopplysning(
            fom = 1.januar(2024),
            tom = 31.januar(2024),
            kilde = Kilde.K9SAK,
            vilkår = Vilkår.FORELDREPENGER,
            type = TypeSaksopplysning.HAR_YTELSE,
            saksbehandler = null,
        )

        val behandlingInnvilget = behandlingVilkårsvurdertInnvilget(
            periode = Periode(1.januar(2024), 31.mars(2024)),
        )

        // Alt ok
        behandlingInnvilget.shouldBeInstanceOf<BehandlingVilkårsvurdert.Innvilget>()

        // Legg til Foreldrepenger i januar. Skal fortsatt være innvilget med januar git ikke rett
        val behandling = behandlingInnvilget.leggTilSaksopplysning(foreldrepenger).behandling
        behandling.shouldBeInstanceOf<BehandlingVilkårsvurdert.Innvilget>()

        behandling.utfallsperioder shouldContainExactlyInAnyOrder listOf(
            BehandlingVilkårsvurdert.Utfallsperiode(
                fom = 1.januar(2024),
                tom = 31.januar(2024),
                antallBarn = 0,
                tiltak = listOf(),
                antDagerMedTiltak = 0,
                utfall = BehandlingVilkårsvurdert.UtfallForPeriode.GIR_IKKE_RETT_TILTAKSPENGER,
            ),
            BehandlingVilkårsvurdert.Utfallsperiode(
                fom = 1.februar(2024),
                tom = 31.mars(2024),
                antallBarn = 0,
                tiltak = listOf(),
                antDagerMedTiltak = 0,
                utfall = BehandlingVilkårsvurdert.UtfallForPeriode.GIR_RETT_TILTAKSPENGER,
            ),
        )

        // Legg til pensjon i mars. Skal fortsatt være innvilget, men ingen rett i januar og mars
        val pensjon = saksopplysning(
            fom = 1.mars(2024),
            tom = 31.mars(2024),
            kilde = Kilde.PESYS,
            vilkår = Vilkår.GJENLEVENDEPENSJON,
            type = TypeSaksopplysning.HAR_YTELSE,
            saksbehandler = null,
        )

        val behandlingMedYtelseStartOgSlutt = behandling.leggTilSaksopplysning(pensjon).behandling
        behandlingMedYtelseStartOgSlutt.shouldBeInstanceOf<BehandlingVilkårsvurdert.Innvilget>()

        behandlingMedYtelseStartOgSlutt.utfallsperioder shouldContainExactlyInAnyOrder listOf(
            BehandlingVilkårsvurdert.Utfallsperiode(
                fom = 1.januar(2024),
                tom = 31.januar(2024),
                antallBarn = 0,
                tiltak = listOf(),
                antDagerMedTiltak = 0,
                utfall = BehandlingVilkårsvurdert.UtfallForPeriode.GIR_IKKE_RETT_TILTAKSPENGER,
            ),
            BehandlingVilkårsvurdert.Utfallsperiode(
                fom = 1.februar(2024),
                tom = 29.februar(2024),
                antallBarn = 0,
                tiltak = listOf(),
                antDagerMedTiltak = 0,
                utfall = BehandlingVilkårsvurdert.UtfallForPeriode.GIR_RETT_TILTAKSPENGER,
            ),
            BehandlingVilkårsvurdert.Utfallsperiode(
                fom = 1.mars(2024),
                tom = 31.mars(2024),
                antallBarn = 0,
                tiltak = listOf(),
                antDagerMedTiltak = 0,
                utfall = BehandlingVilkårsvurdert.UtfallForPeriode.GIR_IKKE_RETT_TILTAKSPENGER,
            ),
        )

        // Legg til kvp i februar. Ingen perioder med rett er igjen så da blir hele avslag
        val kvp = saksopplysning(
            fom = 1.februar(2024),
            tom = 29.februar(2024),
            kilde = Kilde.SAKSB,
            vilkår = Vilkår.KVP,
            type = TypeSaksopplysning.HAR_YTELSE,
            saksbehandler = "Z12345",
        )

        val behandlingAvslag = behandlingMedYtelseStartOgSlutt.leggTilSaksopplysning(kvp).behandling
        behandlingAvslag.shouldBeInstanceOf<BehandlingVilkårsvurdert.Avslag>()

        behandlingAvslag.utfallsperioder shouldContainExactlyInAnyOrder listOf(
            BehandlingVilkårsvurdert.Utfallsperiode(
                fom = 1.januar(2024),
                tom = 31.mars(2024),
                antallBarn = 0,
                tiltak = listOf(),
                antDagerMedTiltak = 0,
                utfall = BehandlingVilkårsvurdert.UtfallForPeriode.GIR_IKKE_RETT_TILTAKSPENGER,
            ),
        )
    }
}
