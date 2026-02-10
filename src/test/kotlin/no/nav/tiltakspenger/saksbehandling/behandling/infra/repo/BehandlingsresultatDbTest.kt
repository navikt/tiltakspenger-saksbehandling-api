package no.nav.tiltakspenger.saksbehandling.behandling.infra.repo

import arrow.core.nonEmptySetOf
import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.periode.til
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Avslagsgrunnlag
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.Revurderingsresultat
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.RevurderingsresultatType
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.Søknadsbehandlingsresultat
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.SøknadsbehandlingsresultatType
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.omgjøring.OmgjørRammevedtak
import no.nav.tiltakspenger.saksbehandling.omgjøring.Omgjøringsgrad
import no.nav.tiltakspenger.saksbehandling.omgjøring.Omgjøringsperiode
import org.junit.jupiter.api.Test

class BehandlingsresultatDbTest {

    @Test
    fun `mapper til db type`() {
        // jan-mars
        val vedtattBehandling = ObjectMother.nyVedtattSøknadsbehandling()

        val søknadsbehandlingInnvilgelse = vedtattBehandling.resultat as Søknadsbehandlingsresultat.Innvilgelse
        val søknadsbehandlingAvslag = Søknadsbehandlingsresultat.Avslag(
            avslagsgrunner = nonEmptySetOf(Avslagsgrunnlag.Alder),
            avslagsperiode = 1 til 10.januar(2025),
        )
        val revurderingStans = Revurderingsresultat.Stans(
            valgtHjemmel = null,
            harValgtStansFraFørsteDagSomGirRett = false,
            stansperiode = 1 til 10.januar(2025),
            omgjørRammevedtak = OmgjørRammevedtak(
                Omgjøringsperiode(
                    rammevedtakId = VedtakId.random(),
                    omgjøringsgrad = Omgjøringsgrad.DELVIS,
                    periode = 1 til 10.januar(2025),
                ),
            ),
        )
        søknadsbehandlingInnvilgelse.toDb() shouldBe "INNVILGELSE"
        søknadsbehandlingAvslag.toDb() shouldBe "AVSLAG"
        revurderingStans.toDb() shouldBe "STANS"
    }

    @Test
    fun `mapper til domene type`() {
        "INNVILGELSE".tilSøknadsbehandlingResultatType() shouldBe SøknadsbehandlingsresultatType.INNVILGELSE
        "AVSLAG".tilSøknadsbehandlingResultatType() shouldBe SøknadsbehandlingsresultatType.AVSLAG
        "STANS".tilRevurderingResultatType() shouldBe RevurderingsresultatType.STANS
    }
}
