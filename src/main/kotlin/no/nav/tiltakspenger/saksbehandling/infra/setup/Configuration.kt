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
    val alleAdRoller: List<AdRolle> by lazy {
        listOf(
            AdRolle(Saksbehandlerrolle.SAKSBEHANDLER, roleSaksbehandler),
            AdRolle(Saksbehandlerrolle.BESLUTTER, roleBeslutter),
            AdRolle(Saksbehandlerrolle.VEILEDER, roleVeileder),
            AdRolle(Saksbehandlerrolle.UTVIKLER, roleUtvikler),
        )
    }

    fun isNais(): Boolean = environmentProfile != EnvironmentProfile.LOCAL

    fun isProd(): Boolean = environmentProfile == EnvironmentProfile.PROD

    fun isDev(): Boolean = environmentProfile == EnvironmentProfile.DEV

    fun gitHash(): String = appImage.substringAfterLast(":")

    data class DataBaseConf(val url: String)

    fun database(): DataBaseConf = DataBaseConf(url = dbJdbcUrl)

    val avroSerializablePackages: String = listOf(
        leesahAvroSerializablePackage,
        aktorV2AvroSerializablePackage,
    ).joinToString(",")
}
