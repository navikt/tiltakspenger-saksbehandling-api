package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto

import no.nav.tiltakspenger.saksbehandling.dagpenger.domene.Dagpengeperiode
import java.time.LocalDate

/**
 * @param tilOgMed null betyr at rettigheten fortsatt er løpende.
 */
data class DagpengeperiodeDTO(
    val fraOgMed: LocalDate,
    val tilOgMed: LocalDate?,
    val ytelsetype: String,
    val kilde: String,
)

fun Dagpengeperiode.toDto(): DagpengeperiodeDTO = DagpengeperiodeDTO(
    fraOgMed = this.fraOgMed,
    tilOgMed = this.tilOgMed,
    ytelsetype = when (this.ytelsetype) {
        Dagpengeperiode.Ytelsetype.ORDINÆRE_DAGPENGER -> "Ordinære dagpenger"
        Dagpengeperiode.Ytelsetype.PERMITTERING -> "Dagpenger under permittering"
        Dagpengeperiode.Ytelsetype.PERMITTERING_FISKEINDUSTRI -> "Dagpenger under permittering fra fiskeindustrien"
    },
    kilde = when (this.kilde) {
        Dagpengeperiode.Kilde.ARENA -> "Arena"
        Dagpengeperiode.Kilde.DP_SAK -> "Ny dagpengeløsning"
    },
)
