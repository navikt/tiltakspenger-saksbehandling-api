package no.nav.tiltakspenger.saksbehandling.lokal

import no.nav.tiltakspenger.saksbehandling.infra.setup.EnvironmentConfig
import no.nav.tiltakspenger.saksbehandling.infra.setup.LOKAL_KLYNGE
import no.nav.tiltakspenger.saksbehandling.infra.setup.Miljøkonfigurasjonsleverandør

/** Registrert i `META-INF/services`, og gir [LocalConfig] uten klyngenavn eller med klyngen [LOKAL_KLYNGE]. */
class LokalKonfigurasjonsleverandør : Miljøkonfigurasjonsleverandør {
    override val klynge = LOKAL_KLYNGE

    override fun hentKonfigurasjon(): EnvironmentConfig = LocalConfig
}

/**
 * Lokale inngangspunkt starter bare uten klyngenavn eller med klyngen [LOKAL_KLYNGE].
 * Sperren kjøres før noe bruker `Configuration` eller kontakter andre tjenester.
 */
fun krevLokalKlynge(klynge: String?) {
    check(klynge == null || klynge == LOKAL_KLYNGE) {
        "Lokal kjøring krever at NAIS_CLUSTER_NAME mangler eller er $LOKAL_KLYNGE, men den er $klynge."
    }
}
