package no.nav.tiltakspenger.saksbehandling.behandling.infra.repo

import arrow.core.toNonEmptyListOrNull
import no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger.Ytelser
import no.nav.tiltakspenger.saksbehandling.infra.repo.dto.PeriodeDbJson
import no.nav.tiltakspenger.saksbehandling.infra.repo.dto.toDbJson
import no.nav.tiltakspenger.saksbehandling.ytelser.domene.Ytelse
import no.nav.tiltakspenger.saksbehandling.ytelser.domene.Ytelsetype
import java.time.LocalDateTime

data class YtelserDbJson(
    val ytelser: List<YtelseDbJson>,
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

    data class YtelseDbJson(
        val ytelsetype: String,
        val perioder: List<PeriodeDbJson>,
    ) {
        fun toDomain(): Ytelse {
            return Ytelse(
                ytelsetype = Ytelsetype.entries.single { it.tekstverdi == ytelsetype },
                perioder = perioder.map { it.toDomain() },
            )
        }
    }

    fun toDomain(): Ytelser {
        return when (type) {
            Type.Treff -> Ytelser.Treff(
                value = ytelser.map { it.toDomain() }.toNonEmptyListOrNull()!!,
                oppslagsperiode = oppslagsperiode!!.toDomain(),
                oppslagstidspunkt = oppslagstidspunkt!!,
            )

            Type.IngenTreff -> Ytelser.IngenTreff(
                oppslagsperiode = oppslagsperiode!!.toDomain(),
                oppslagstidspunkt = oppslagstidspunkt!!,
            )

            Type.IkkeBehandlingsgrunnlag -> Ytelser.IkkeBehandlingsgrunnlag

            Type.BehandletFørFeature -> Ytelser.BehandletFørFeature
        }
    }
}

fun Ytelser.toDbJson(): YtelserDbJson {
    return YtelserDbJson(
        ytelser = this.value.map { ytelse ->
            YtelserDbJson.YtelseDbJson(
                ytelsetype = ytelse.ytelsetype.tekstverdi,
                perioder = ytelse.perioder.map { it.toDbJson() },
            )
        },
        type = when (this) {
            Ytelser.BehandletFørFeature -> YtelserDbJson.Type.BehandletFørFeature
            Ytelser.IkkeBehandlingsgrunnlag -> YtelserDbJson.Type.IkkeBehandlingsgrunnlag
            is Ytelser.IngenTreff -> YtelserDbJson.Type.IngenTreff
            is Ytelser.Treff -> YtelserDbJson.Type.Treff
        },
        oppslagstidspunkt = this.oppslagstidspunkt,
        oppslagsperiode = this.oppslagsperiode?.toDbJson(),
    )
}
