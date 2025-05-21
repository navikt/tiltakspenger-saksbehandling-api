package no.nav.tiltakspenger.saksbehandling.behandling.infra.repo

import arrow.core.nonEmptySetOf
import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Avslagsgrunnlag
import no.nav.tiltakspenger.saksbehandling.behandling.domene.RevurderingUtfall
import no.nav.tiltakspenger.saksbehandling.behandling.domene.RevurderingUtfallType
import no.nav.tiltakspenger.saksbehandling.behandling.domene.SøknadsbehandlingUtfall
import no.nav.tiltakspenger.saksbehandling.behandling.domene.SøknadsbehandlingUtfallType
import org.junit.jupiter.api.Test

class BehandlingsutfallDbTest {

    @Test
    fun `mapper til db type`() {
        val søknadsbehandlingInnvilgelse = SøknadsbehandlingUtfall.Innvilgelse(
            valgteTiltaksdeltakelser = null,
            barnetillegg = null,
        )
        val søknadsbehandlingAvslag = SøknadsbehandlingUtfall.Avslag(
            avslagsgrunner = nonEmptySetOf(Avslagsgrunnlag.Alder),
        )
        val revurderingStans = RevurderingUtfall.Stans(
            valgtHjemmelHarIkkeRettighet = emptyList(),
        )

        søknadsbehandlingInnvilgelse.toDb() shouldBe "INNVILGELSE"
        søknadsbehandlingAvslag.toDb() shouldBe "AVSLAG"
        revurderingStans.toDb() shouldBe "STANS"
    }

    @Test
    fun `mapper til domene type`() {
        "INNVILGELSE".tilSøknadsbehandlingUtfallType() shouldBe SøknadsbehandlingUtfallType.INNVILGELSE
        "AVSLAG".tilSøknadsbehandlingUtfallType() shouldBe SøknadsbehandlingUtfallType.AVSLAG
        "STANS".tilRevurderingUtfallType() shouldBe RevurderingUtfallType.STANS
    }
}
