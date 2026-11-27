package no.nav.tiltakspenger.saksbehandling.infra.setup

/**
 * Leverer konfigurasjonen for en navngitt Nais-klynge som appen ikke selv har konfigurasjon for.
 * Implementasjonen lastes med [java.util.ServiceLoader] og må ligge i en egen modul som bare er med i imaget for den klyngen.
 */
interface Miljøkonfigurasjonsleverandør {
    /** Verdien av `NAIS_CLUSTER_NAME` leverandøren gjelder for. */
    val klynge: String

    fun hentKonfigurasjon(): EnvironmentConfig
}
