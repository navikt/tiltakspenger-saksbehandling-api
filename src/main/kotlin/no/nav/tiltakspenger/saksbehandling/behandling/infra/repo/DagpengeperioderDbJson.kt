package no.nav.tiltakspenger.saksbehandling.behandling.infra.repo

import arrow.core.toNonEmptyListOrNull
import no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger.Dagpengeperioder
import no.nav.tiltakspenger.saksbehandling.dagpenger.domene.Dagpengeperiode
import no.nav.tiltakspenger.saksbehandling.infra.repo.dto.PeriodeDbJson
import no.nav.tiltakspenger.saksbehandling.infra.repo.dto.toDbJson
import java.time.LocalDate
import java.time.LocalDateTime

data class DagpengeperioderDbJson(
    val perioder: List<DagpengeperiodeDbJson>,
    val type: Type,
    val oppslagstidspunkt: LocalDateTime?,
    val oppslagsperiode: PeriodeDbJson?,
) {
    enum class Type {
        Treff,
        IngenTreff,
        IkkeBehandlingsgrunnlag,
        BehandletFørFeature,
    }

    data class DagpengeperiodeDbJson(
        val fraOgMed: LocalDate,
        val tilOgMed: LocalDate?,
        val ytelsetype: String,
        val kilde: String,
    ) {
        fun toDomain(): Dagpengeperiode {
            return Dagpengeperiode(
                fraOgMed = fraOgMed,
                tilOgMed = tilOgMed,
                ytelsetype = when (ytelsetype) {
                    "ORDINÆRE_DAGPENGER" -> Dagpengeperiode.Ytelsetype.ORDINÆRE_DAGPENGER
                    "PERMITTERING" -> Dagpengeperiode.Ytelsetype.PERMITTERING
                    "PERMITTERING_FISKEINDUSTRI" -> Dagpengeperiode.Ytelsetype.PERMITTERING_FISKEINDUSTRI
                    else -> throw IllegalStateException("Ukjent ytelsetype for dagpenger i databasen: $ytelsetype")
                },
                kilde = when (kilde) {
                    "ARENA" -> Dagpengeperiode.Kilde.ARENA
                    "DP_SAK" -> Dagpengeperiode.Kilde.DP_SAK
                    else -> throw IllegalStateException("Ukjent kilde for dagpenger i databasen: $kilde")
                },
            )
        }
    }

    fun toDomain(): Dagpengeperioder {
        return when (type) {
            Type.Treff -> Dagpengeperioder.Treff(
                value = perioder.map { it.toDomain() }.toNonEmptyListOrNull()!!,
                oppslagsperiode = oppslagsperiode!!.toDomain(),
                oppslagstidspunkt = oppslagstidspunkt!!,
            )

            Type.IngenTreff -> Dagpengeperioder.IngenTreff(
                oppslagsperiode = oppslagsperiode!!.toDomain(),
                oppslagstidspunkt = oppslagstidspunkt!!,
            )

            Type.IkkeBehandlingsgrunnlag -> Dagpengeperioder.IkkeBehandlingsgrunnlag

            Type.BehandletFørFeature -> Dagpengeperioder.BehandletFørFeature
        }
    }
}

fun Dagpengeperioder.toDbJson(): DagpengeperioderDbJson {
    return DagpengeperioderDbJson(
        perioder = this.value.map { it.toDbJson() },
        type = when (this) {
            is Dagpengeperioder.BehandletFørFeature -> DagpengeperioderDbJson.Type.BehandletFørFeature
            is Dagpengeperioder.IkkeBehandlingsgrunnlag -> DagpengeperioderDbJson.Type.IkkeBehandlingsgrunnlag
            is Dagpengeperioder.IngenTreff -> DagpengeperioderDbJson.Type.IngenTreff
            is Dagpengeperioder.Treff -> DagpengeperioderDbJson.Type.Treff
        },
        oppslagstidspunkt = this.oppslagstidspunkt,
        oppslagsperiode = this.oppslagsperiode?.toDbJson(),
    )
}

fun Dagpengeperiode.toDbJson(): DagpengeperioderDbJson.DagpengeperiodeDbJson {
    return DagpengeperioderDbJson.DagpengeperiodeDbJson(
        fraOgMed = fraOgMed,
        tilOgMed = tilOgMed,
        ytelsetype = when (ytelsetype) {
            Dagpengeperiode.Ytelsetype.ORDINÆRE_DAGPENGER -> "ORDINÆRE_DAGPENGER"
            Dagpengeperiode.Ytelsetype.PERMITTERING -> "PERMITTERING"
            Dagpengeperiode.Ytelsetype.PERMITTERING_FISKEINDUSTRI -> "PERMITTERING_FISKEINDUSTRI"
        },
        kilde = when (kilde) {
            Dagpengeperiode.Kilde.ARENA -> "ARENA"
            Dagpengeperiode.Kilde.DP_SAK -> "DP_SAK"
        },
    )
}
