package no.nav.tiltakspenger.saksbehandling.infra.setup

import no.nav.tiltakspenger.libs.common.Saksbehandlerrolle
import no.nav.tiltakspenger.libs.texas.AdRolle
import java.util.ServiceLoader

const val KAFKA_CONSUMER_GROUP_ID = "tiltakspenger-saksbehandling-api-consumer"

const val AUTOMATISK_SAKSBEHANDLER_ID = "tp-sak"

/**
 * Velger konfigurasjon ut fra Nais-klyngen.
 * Uten klynge eller med klynge `lokal` kjører appen lokalt med [LocalConfig].
 * `prod-gcp` bruker [ProdConfig] uten å lete etter leverandører.
 * Alle andre klynger krever nøyaktig én [Miljøkonfigurasjonsleverandør] på classpath for den aktuelle klyngen.
 *
 * [leverandører] kalles bare for andre navngitte klynger enn prod og lokal.
 */
fun miljøkonfigurasjonFor(
    klynge: String?,
    leverandører: () -> Iterable<Miljøkonfigurasjonsleverandør> = { ServiceLoader.load(Miljøkonfigurasjonsleverandør::class.java) },
): EnvironmentConfig =
    when (klynge) {
        null, "lokal" -> LocalConfig

        "prod-gcp" -> ProdConfig

        else -> {
            val treff = leverandører().filter { it.klynge == klynge }
            val leverandør = checkNotNull(treff.singleOrNull()) {
                "NAIS_CLUSTER_NAME er $klynge, og det krever nøyaktig én Miljøkonfigurasjonsleverandør for klyngen, men fant ${treff.size}."
            }
            leverandør.hentKonfigurasjon().also {
                check(it.environmentProfile == EnvironmentProfile.ANNEN_NAIS_KLYNGE) {
                    "Konfigurasjonen for klyngen $klynge må ha profilen ANNEN_NAIS_KLYNGE, men har ${it.environmentProfile}."
                }
            }
        }
    }

object Configuration : EnvironmentConfig by miljøkonfigurasjonFor(System.getenv("NAIS_CLUSTER_NAME")) {
    val alleAdRoller: List<AdRolle> by lazy {
        listOf(
            AdRolle(Saksbehandlerrolle.SAKSBEHANDLER, roleSaksbehandler),
            AdRolle(Saksbehandlerrolle.BESLUTTER, roleBeslutter),
            AdRolle(Saksbehandlerrolle.VEILEDER, roleVeileder),
            AdRolle(Saksbehandlerrolle.UTVIKLER, roleUtvikler),
            AdRolle(Saksbehandlerrolle.TILBAKEKREVING, roleTilbakekreving),
        )
    }

    fun isNais(): Boolean = environmentProfile != EnvironmentProfile.LOCAL

    fun isProd(): Boolean = environmentProfile == EnvironmentProfile.PROD

    fun gitHash(): String = appImage.substringAfterLast(":")

    data class DataBaseConf(val url: String)

    fun database(): DataBaseConf = DataBaseConf(url = dbJdbcUrl)

    val avroSerializablePackages: String = listOf(
        leesahAvroSerializablePackage,
        aktorV2AvroSerializablePackage,
    ).joinToString(",")
}
