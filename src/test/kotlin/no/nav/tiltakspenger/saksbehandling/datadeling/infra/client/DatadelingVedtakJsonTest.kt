package no.nav.tiltakspenger.saksbehandling.datadeling.infra.client

import arrow.core.nonEmptySetOf
import io.kotest.assertions.json.shouldEqualJson
import no.nav.tiltakspenger.libs.json.lesTre
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Avslagsgrunnlag
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.SøknadsbehandlingsresultatType
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import org.junit.jupiter.api.Test

class DatadelingVedtakJsonTest {
    @Test
    fun `avslag med grunn InnvilgetTiltakspenger sendes som LIVSOPPHOLDSYTELSER til datadeling`() {
        val behandling = ObjectMother.nyVedtattSøknadsbehandling(
            resultat = SøknadsbehandlingsresultatType.AVSLAG,
            avslagsgrunner = nonEmptySetOf(Avslagsgrunnlag.InnvilgetTiltakspenger),
        )
        val avslag = ObjectMother.nyRammevedtakAvslag(
            sakId = behandling.sakId,
            fnr = behandling.fnr,
            saksnummer = behandling.saksnummer,
            behandling = behandling,
        )

        // Datadeling kjenner ikke den nye koden, så grunnen sendes som en hjemmel de allerede har.
        lesTre(avslag.toDatadelingJson()).get("valgteHjemlerHarIkkeRettighet").toString()
            .shouldEqualJson("""["LIVSOPPHOLDSYTELSER"]""")
    }
}
