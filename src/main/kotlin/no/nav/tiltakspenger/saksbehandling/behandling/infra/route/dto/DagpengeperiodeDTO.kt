package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto

import no.nav.tiltakspenger.saksbehandling.dagpenger.domene.Dagpengeperiode
import java.time.LocalDate

/**
 * @param tilOgMed null betyr at rettigheten fortsatt er løpende.
 */
data class DagpengeperiodeDTO(
    val fraOgMed: LocalDate,
    val tilOgMed: LocalDate?,
    val ytelsetype: YtelsetypeDTO,
    val kilde: KildeDTO,
) {
    enum class YtelsetypeDTO {
        ORDINÆRE_DAGPENGER,
        PERMITTERING,
        PERMITTERING_FISKEINDUSTRI,
    }

    enum class KildeDTO {
        ARENA,
        DP_SAK,
    }
}

fun Dagpengeperiode.toDto(): DagpengeperiodeDTO = DagpengeperiodeDTO(
    fraOgMed = this.fraOgMed,
    tilOgMed = this.tilOgMed,
    ytelsetype = when (this.ytelsetype) {
        Dagpengeperiode.Ytelsetype.ORDINÆRE_DAGPENGER -> DagpengeperiodeDTO.YtelsetypeDTO.ORDINÆRE_DAGPENGER
        Dagpengeperiode.Ytelsetype.PERMITTERING -> DagpengeperiodeDTO.YtelsetypeDTO.PERMITTERING
        Dagpengeperiode.Ytelsetype.PERMITTERING_FISKEINDUSTRI -> DagpengeperiodeDTO.YtelsetypeDTO.PERMITTERING_FISKEINDUSTRI
    },
    kilde = when (this.kilde) {
        Dagpengeperiode.Kilde.ARENA -> DagpengeperiodeDTO.KildeDTO.ARENA
        Dagpengeperiode.Kilde.DP_SAK -> DagpengeperiodeDTO.KildeDTO.DP_SAK
    },
)
