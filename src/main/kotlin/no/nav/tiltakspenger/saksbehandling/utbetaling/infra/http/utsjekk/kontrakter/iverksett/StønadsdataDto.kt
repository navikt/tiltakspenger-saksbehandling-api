package no.nav.tiltakspenger.saksbehandling.utbetaling.infra.http.utsjekk.kontrakter.iverksett

import com.fasterxml.jackson.annotation.JsonCreator
import no.nav.tiltakspenger.saksbehandling.utbetaling.infra.http.utsjekk.kontrakter.felles.StønadType
import no.nav.tiltakspenger.saksbehandling.utbetaling.infra.http.utsjekk.kontrakter.felles.StønadTypeTiltakspenger
import tools.jackson.databind.JsonNode

enum class Ferietillegg {
    ORDINÆR,
    AVDØD,
}

/**
 * Stønadsdata representerer ytelsesspesifikke data for utbetalingsperioden. Se subklasser for detaljer.
 */
sealed class StønadsdataDto(open val stønadstype: StønadType) {
    companion object {
        @JsonCreator
        @JvmStatic
        fun deserialize(json: JsonNode) =
            listOf(
                StønadsdataTiltakspengerV2Dto::deserialiser,
            )
                .map { it(json) }
                .firstOrNull { it != null } ?: error("Klarte ikke deserialisere stønadsdata")
    }
}

/**
 * @property stønadstype Stønadstypene for tiltakspenger representerer tiltakstypene.
 * @property barnetillegg Settes når utbetalingsperioden gjelder et barnetillegg.
 * @property brukersNavKontor Enhetsnummeret for NAV-kontoret som brukeren tilhører.
 * @property meldekortId Id på meldekortet utbetalingen gjelder.
 */
data class StønadsdataTiltakspengerV2Dto(
    override val stønadstype: StønadTypeTiltakspenger,
    val barnetillegg: Boolean = false,
    val brukersNavKontor: String,
    val meldekortId: String,
) : StønadsdataDto(stønadstype) {
    companion object {
        fun deserialiser(json: JsonNode) = try {
            StønadsdataTiltakspengerV2Dto(
                stønadstype = StønadTypeTiltakspenger.valueOf(json["stønadstype"].asString()),
                barnetillegg = json["barnetillegg"]?.asBoolean() ?: false,
                brukersNavKontor = json["brukersNavKontor"].asString(),
                meldekortId = json["meldekortId"].asString(),
            )
        } catch (_: Exception) {
            null
        }
    }
}
