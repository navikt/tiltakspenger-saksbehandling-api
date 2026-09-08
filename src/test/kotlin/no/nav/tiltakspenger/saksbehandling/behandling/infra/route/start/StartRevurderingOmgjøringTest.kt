package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.start

import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.dato.mars
import no.nav.tiltakspenger.libs.ktor.test.common.ForventetRespons
import no.nav.tiltakspenger.libs.periode.til
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.Omgjøringsresultat
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.SøknadsbehandlingsresultatType
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContext
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.innvilgelsesperioder
import no.nav.tiltakspenger.saksbehandling.omgjøring.OmgjørRammevedtak
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettOmgjøringInnvilgelse
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.startRevurderingOmgjøring
import org.junit.jupiter.api.Test

class StartRevurderingOmgjøringTest {

    @Test
    fun `Kan starte omgjøring uten valgt resultat`() {
        withTestApplicationContext { tac ->
            val innvilgelsesperiode = 1.januar(2025) til 31.mars(2025)

            val (sak, _, søknadVedtak) = iverksettSøknadsbehandling(
                tac = tac,
                innvilgelsesperioder = innvilgelsesperioder(innvilgelsesperiode),
            )

            val (_, omgjøring) = startRevurderingOmgjøring(
                tac = tac,
                sakId = sak.id,
                rammevedtakIdSomOmgjøres = søknadVedtak.id,
            )!!

            omgjøring.resultat.shouldBeInstanceOf<Omgjøringsresultat.OmgjøringIkkeValgt>()

            omgjøring.resultat.omgjørRammevedtak shouldBe OmgjørRammevedtak.create(søknadVedtak)
        }
    }

    @Test
    fun `kan ikke starte omgjøring av et vedtak som allerede er omgjort i sin helhet`() {
        withTestApplicationContext { tac ->
            val innvilgelsesperiode = 1.januar(2025) til 31.mars(2025)

            val (sak, _, søknadVedtak) = iverksettSøknadsbehandling(
                tac = tac,
                innvilgelsesperioder = innvilgelsesperioder(innvilgelsesperiode),
            )

            iverksettOmgjøringInnvilgelse(
                tac = tac,
                sakId = sak.id,
                rammevedtakIdSomOmgjøres = søknadVedtak.id,
                innvilgelsesperioder = innvilgelsesperioder(innvilgelsesperiode),
            )

            startRevurderingOmgjøring(
                tac = tac,
                sakId = sak.id,
                rammevedtakIdSomOmgjøres = søknadVedtak.id,
                forventet = vedtakKanIkkeOmgjøres,
            ) shouldBe null
        }
    }

    @Test
    fun `kan ikke starte omgjøring av et avslagsvedtak`() {
        withTestApplicationContext { tac ->
            val (sak, _, avslagsvedtak) = iverksettSøknadsbehandling(
                tac = tac,
                resultat = SøknadsbehandlingsresultatType.AVSLAG,
            )

            startRevurderingOmgjøring(
                tac = tac,
                sakId = sak.id,
                rammevedtakIdSomOmgjøres = avslagsvedtak.id,
                forventet = vedtakKanIkkeOmgjøres,
            ) shouldBe null
        }
    }

    private val vedtakKanIkkeOmgjøres = ForventetRespons.json(
        400,
        """
        {
          "melding": "Vedtaket kan ikke omgjøres fordi det er et avslagsvedtak eller allerede er omgjort i sin helhet.",
          "kode": "vedtak_kan_ikke_omgjøres"
        }
        """.trimIndent(),
        "application/json; charset=UTF-8",
    )
}
