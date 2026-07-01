package no.nav.tiltakspenger.saksbehandling.infra.setup

import no.nav.tiltakspenger.libs.common.Saksbehandlerrolle
import no.nav.tiltakspenger.libs.texas.AdRolle

private const val APPLICATION_NAME = "tiltakspenger-saksbehandling-api"
const val KAFKA_CONSUMER_GROUP_ID = "$APPLICATION_NAME-consumer"

const val AUTOMATISK_SAKSBEHANDLER_ID = "tp-sak"

enum class Profile {
    LOCAL,
    DEV,
    PROD,
}

object Configuration : EnvironmentConfig by when (System.getenv("NAIS_CLUSTER_NAME")) {
    "dev-gcp" -> DevConfig
    "prod-gcp" -> ProdConfig
    else -> LocalConfig
} {
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
