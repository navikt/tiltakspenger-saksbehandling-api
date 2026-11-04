package no.nav.tiltakspenger.saksbehandling.behandling.infra.repo

import no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger.Ytelser
import no.nav.tiltakspenger.saksbehandling.infra.repo.dto.PeriodeDbJson
import no.nav.tiltakspenger.saksbehandling.infra.repo.dto.toDbJson
import no.nav.tiltakspenger.saksbehandling.ytelser.domene.Ytelse
import no.nav.tiltakspenger.saksbehandling.ytelser.infra.repo.tilYtelsetypeFraDb
import no.nav.tiltakspenger.saksbehandling.ytelser.infra.repo.toDb
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

    /** [ytelsetype] er vår egen kode og er null for en ukjent ytelse; [kildetekst] er teksten fra sokos-utbetaldata. */
    data class YtelseDbJson(
        val ytelsetype: String?,
        val kildetekst: String?,
        val perioder: List<PeriodeDbJson>,
    ) {
        fun toDomain(): Ytelse {
            val perioder = perioder.map { it.toDomain() }
            return if (ytelsetype == null) {
                Ytelse.Ukjent(kildetekst = kildetekst, perioder = perioder)
            } else {
                Ytelse.Kjent(ytelsetype = ytelsetype.tilYtelsetypeFraDb(), kildetekst = kildetekst!!, perioder = perioder)
            }
        }
    }

    fun toDomain(): Ytelser {
        return when (type) {
            Type.Treff -> Ytelser.fromList(
                ytelser = ytelser.map { it.toDomain() },
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
                ytelsetype = when (ytelse) {
                    is Ytelse.Kjent -> ytelse.ytelsetype.toDb()
                    is Ytelse.Ukjent -> null
                },
                kildetekst = ytelse.kildetekst,
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
