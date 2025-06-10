package no.nav.tiltakspenger.saksbehandling.behandling.infra.repo

import arrow.core.nonEmptySetOf
import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Avslagsgrunnlag
import no.nav.tiltakspenger.saksbehandling.behandling.domene.RevurderingResultat
import no.nav.tiltakspenger.saksbehandling.behandling.domene.RevurderingType
import no.nav.tiltakspenger.saksbehandling.behandling.domene.SøknadsbehandlingResultat
import no.nav.tiltakspenger.saksbehandling.behandling.domene.SøknadsbehandlingType
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import org.junit.jupiter.api.Test

class BehandlingsresultatDbTest {

    @Test
    fun `mapper til db type`() {
        val vedtattBehandling = ObjectMother.nyVedtattSøknadsbehandling()

        val søknadsbehandlingInnvilgelse = vedtattBehandling.resultat as SøknadsbehandlingResultat.Innvilgelse
        val søknadsbehandlingAvslag = SøknadsbehandlingResultat.Avslag(
            avslagsgrunner = nonEmptySetOf(Avslagsgrunnlag.Alder),
        )
        val revurderingStans = RevurderingResultat.Stans(
            valgtHjemmel = emptyList(),
        )

        søknadsbehandlingInnvilgelse.toDb() shouldBe "INNVILGELSE"
        søknadsbehandlingAvslag.toDb() shouldBe "AVSLAG"
        revurderingStans.toDb() shouldBe "STANS"
    }

    @Test
    fun `mapper til domene type`() {
        "INNVILGELSE".tilSøknadsbehandlingUtfallType() shouldBe SøknadsbehandlingType.INNVILGELSE
        "AVSLAG".tilSøknadsbehandlingUtfallType() shouldBe SøknadsbehandlingType.AVSLAG
        "STANS".tilRevurderingUtfallType() shouldBe RevurderingType.STANS
    }
}
