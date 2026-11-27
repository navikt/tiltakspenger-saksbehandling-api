package no.nav.tiltakspenger.saksbehandling.dev

import no.nav.tiltakspenger.saksbehandling.infra.setup.EnvironmentConfig
import no.nav.tiltakspenger.saksbehandling.infra.setup.Miljøkonfigurasjonsleverandør

/**
 * Registrert i `META-INF/services`.
 * [DevConfig] leser påkrevde miljøvariabler når den initialiseres, så den hentes først når klyngen er dev-gcp.
 */
class DevKonfigurasjonsleverandør : Miljøkonfigurasjonsleverandør {
    override val klynge = KLYNGE

    override fun hentKonfigurasjon(): EnvironmentConfig = DevConfig

    companion object {
        const val KLYNGE = "dev-gcp"
    }
}
