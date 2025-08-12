package no.nav.tiltakspenger.saksbehandling.benk.domene

import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.saksbehandling.felles.ServiceCommand

data class HentÅpneBehandlingerCommand(
    val åpneBehandlingerFiltrering: ÅpneBehandlingerFiltrering,
    val sortering: BenkSortering,
    override val saksbehandler: Saksbehandler,
    override val correlationId: CorrelationId,
) : ServiceCommand

data class ÅpneBehandlingerFiltrering(
    val benktype: BehandlingssammendragBenktype = BehandlingssammendragBenktype.KLAR,
    val behandlingstype: List<BehandlingssammendragType>?,
    val status: List<BehandlingssammendragStatus>?,
    val identer: List<String>?,
)

data class BenkSortering(
    val kolonne: BenkSorteringKolonne,
    val retning: SorteringRetning,
) {
    companion object {
        fun fromString(sortering: String): BenkSortering {
            val parts = sortering.split(",")
            return BenkSortering(
                kolonne = BenkSorteringKolonne.fromString(parts.getOrNull(0)),
                retning = SorteringRetning.fromString(parts.getOrNull(1)),
            )
        }
    }
}

// Tillater bare sortering på enkelte kolonner.
enum class BenkSorteringKolonne {
    STARTET,
    SIST_ENDRET,
    ;

    companion object {
        fun fromString(kolonne: String?): BenkSorteringKolonne = kolonne.toEnumOrDefault(STARTET)
    }
}

enum class SorteringRetning {
    ASC,
    DESC,
    ;

    companion object {
        fun fromString(retning: String?): SorteringRetning = retning.toEnumOrDefault(ASC)
    }
}

inline fun <reified T : Enum<T>> String?.toEnumOrDefault(default: T): T =
    enumValues<T>().firstOrNull { it.name.equals(this, ignoreCase = true) } ?: default
