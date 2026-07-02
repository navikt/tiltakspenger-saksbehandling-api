package no.nav.tiltakspenger.saksbehandling.infra.setup

import no.nav.tiltakspenger.libs.common.Saksbehandlerrolle
import no.nav.tiltakspenger.libs.texas.AdRolle

const val KAFKA_CONSUMER_GROUP_ID = "tiltakspenger-saksbehandling-api-consumer"

const val AUTOMATISK_SAKSBEHANDLER_ID = "tp-sak"

private fun hentConfigForMiljø(): EnvironmentConfig {
    return when (System.getenv("NAIS_CLUSTER_NAME")) {
        "prod-gcp" -> ProdConfig
        "dev-gcp" -> DevConfig
        else -> LocalConfig
    }
}

object Configuration : EnvironmentConfig by hentConfigForMiljø() {
    fun alleAdRoller(): List<AdRolle> =
        listOf(
            AdRolle(Saksbehandlerrolle.SAKSBEHANDLER, roleSaksbehandler),
            AdRolle(Saksbehandlerrolle.BESLUTTER, roleBeslutter),
            AdRolle(Saksbehandlerrolle.DRIFT, roleDrift),
        )

    fun isNais(): Boolean = profile != Profile.LOCAL

    fun isProd(): Boolean = profile == Profile.PROD

    fun isDev(): Boolean = profile == Profile.DEV

    fun gitHash(): String = appImage.substringAfterLast(":")

    data class DataBaseConf(val url: String)

    fun database(): DataBaseConf = DataBaseConf(url = dbJdbcUrl)
}
