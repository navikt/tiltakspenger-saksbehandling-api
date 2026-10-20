package no.nav.tiltakspenger.saksbehandling.behandling.infra.repo

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
            // Et lagret Treff som bare hadde fjernede ytelsetyper, blir IngenTreff, slik et nytt oppslag i dag ville gitt.
            Type.Treff -> Ytelser.fromList(
                ytelser = ytelser.filter { it.ytelsetype !in fjernedeYtelsetyper }.map { it.toDomain() },
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

/**
 * Ytelsetyper som finnes i lagrede saksopplysninger, men som aldri skulle vært hentet, hoppes over ved lesing.
 * Barnepensjon skulle ikke ligget i [Ytelsetype]: den hindrer ikke tiltakspenger (tiltakspengeforskriften § 7 første ledd), men ble hentet og vist til saksbehandler fram til 2026-10-20.
 * Alle vedtak om avslag, stans og opphør med denne opplysningen er gjennomgått, og ingen bygde på barnepensjon.
 * Det kan senere vurderes om typen skal fjernes fra de lagrede dataene.
 */
private val fjernedeYtelsetyper = setOf("Barnepensjon")

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
