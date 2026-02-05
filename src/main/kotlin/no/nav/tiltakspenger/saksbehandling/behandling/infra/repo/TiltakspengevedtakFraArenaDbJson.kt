package no.nav.tiltakspenger.saksbehandling.behandling.infra.repo

import arrow.core.toNonEmptyListOrNull
import no.nav.tiltakspenger.saksbehandling.arenavedtak.domene.ArenaTPVedtak
import no.nav.tiltakspenger.saksbehandling.arenavedtak.domene.ArenaTPVedtak.Rettighet
import no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger.TiltakspengevedtakFraArena
import no.nav.tiltakspenger.saksbehandling.infra.repo.dto.PeriodeDbJson
import no.nav.tiltakspenger.saksbehandling.infra.repo.dto.toDbJson
import java.time.LocalDate
import java.time.LocalDateTime

data class TiltakspengevedtakFraArenaDbJson(
    val tiltakspengevedtakFraArena: List<ArenaTPVedtakDbJson>,
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

    data class ArenaTPVedtakDbJson(
        val fraOgMed: LocalDate,
        val tilOgMed: LocalDate?,
        val rettighet: String,
        val vedtakId: Long,
    ) {
        fun toDomain(): ArenaTPVedtak {
            return ArenaTPVedtak(
                fraOgMed = fraOgMed,
                tilOgMed = tilOgMed,
                rettighet = Rettighet.valueOf(rettighet),
                vedtakId = vedtakId,
            )
        }
    }

    fun toDomain(): TiltakspengevedtakFraArena {
        return when (type) {
            Type.Treff -> TiltakspengevedtakFraArena.Treff(
                value = tiltakspengevedtakFraArena.map { it.toDomain() }.toNonEmptyListOrNull()!!,
                oppslagsperiode = oppslagsperiode!!.toDomain(),
                oppslagstidspunkt = oppslagstidspunkt!!,
            )

            Type.IngenTreff -> TiltakspengevedtakFraArena.IngenTreff(
                oppslagsperiode = oppslagsperiode!!.toDomain(),
                oppslagstidspunkt = oppslagstidspunkt!!,
            )

            Type.IkkeBehandlingsgrunnlag -> TiltakspengevedtakFraArena.IkkeBehandlingsgrunnlag

            Type.BehandletFørFeature -> TiltakspengevedtakFraArena.BehandletFørFeature
        }
    }
}

fun TiltakspengevedtakFraArena.toDbJson(): TiltakspengevedtakFraArenaDbJson {
    return TiltakspengevedtakFraArenaDbJson(
        tiltakspengevedtakFraArena = this.value.map {
            TiltakspengevedtakFraArenaDbJson.ArenaTPVedtakDbJson(
                fraOgMed = it.fraOgMed,
                tilOgMed = it.tilOgMed,
                rettighet = it.rettighet.name,
                vedtakId = it.vedtakId,
            )
        },
        type = when (this) {
            is TiltakspengevedtakFraArena.BehandletFørFeature -> TiltakspengevedtakFraArenaDbJson.Type.BehandletFørFeature
            is TiltakspengevedtakFraArena.IkkeBehandlingsgrunnlag -> TiltakspengevedtakFraArenaDbJson.Type.IkkeBehandlingsgrunnlag
            is TiltakspengevedtakFraArena.IngenTreff -> TiltakspengevedtakFraArenaDbJson.Type.IngenTreff
            is TiltakspengevedtakFraArena.Treff -> TiltakspengevedtakFraArenaDbJson.Type.Treff
        },
        oppslagstidspunkt = this.oppslagstidspunkt,
        oppslagsperiode = this.oppslagsperiode?.toDbJson(),
    )
}
