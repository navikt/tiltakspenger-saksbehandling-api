package no.nav.tiltakspenger.saksbehandling.infra.setup

import no.nav.tiltakspenger.libs.common.Saksbehandlerrolle
import no.nav.tiltakspenger.libs.texas.AdRolle
import java.util.ServiceLoader

const val KAFKA_CONSUMER_GROUP_ID = "tiltakspenger-saksbehandling-api-consumer"

const val AUTOMATISK_SAKSBEHANDLER_ID = "tp-sak"

/**
 * Velger konfigurasjon ut fra Nais-klyngen.
 * `prod-gcp` bruker [ProdConfig] uten å lete etter leverandører.
 * Uten klynge brukes leverandøren for klyngen [LOKAL_KLYNGE], som bare finnes i lokalmodulen.
 * Produksjons- og utviklingsimaget har den ikke, så de feiler ved oppstart uten `NAIS_CLUSTER_NAME`.
 * Alle andre klynger, også [LOKAL_KLYNGE], krever nøyaktig én [Miljøkonfigurasjonsleverandør] på classpath for den aktuelle klyngen.
 *
 * [leverandører] kalles ikke for prod.
 */
fun miljøkonfigurasjonFor(
    klynge: String?,
    leverandører: () -> Iterable<Miljøkonfigurasjonsleverandør> = { ServiceLoader.load(Miljøkonfigurasjonsleverandør::class.java) },
): EnvironmentConfig {
    if (klynge == "prod-gcp") return ProdConfig
    val ønsketKlynge = klynge ?: LOKAL_KLYNGE
    val treff = leverandører().filter { it.klynge == ønsketKlynge }
    val leverandør = checkNotNull(treff.singleOrNull()) {
        if (klynge == null) {
            "NAIS_CLUSTER_NAME mangler, og lokal kjøring krever nøyaktig én Miljøkonfigurasjonsleverandør for $LOKAL_KLYNGE fra lokalmodulen, men fant ${treff.size}."
        } else {
            "NAIS_CLUSTER_NAME er $klynge, og det krever nøyaktig én Miljøkonfigurasjonsleverandør for klyngen, men fant ${treff.size}."
        }
    }
    val forventetProfil = if (ønsketKlynge == LOKAL_KLYNGE) EnvironmentProfile.LOCAL else EnvironmentProfile.ANNEN_NAIS_KLYNGE
    return leverandør.hentKonfigurasjon().also {
        check(it.environmentProfile == forventetProfil) {
            "Konfigurasjonen for klyngen $ønsketKlynge må ha profilen $forventetProfil, men har ${it.environmentProfile}."
        }
    }
}

/** Klyngenavnet lokal kjøring bruker, og som leverandøren i lokalmodulen registrerer seg for. */
const val LOKAL_KLYNGE = "lokal"

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
