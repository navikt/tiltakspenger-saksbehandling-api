package no.nav.tiltakspenger.saksbehandling.vedtak.infra.route

import no.nav.tiltakspenger.libs.periodisering.PeriodeDTO
import no.nav.tiltakspenger.libs.periodisering.toDTO
import no.nav.tiltakspenger.saksbehandling.vedtak.Rammevedtakskommando
import no.nav.tiltakspenger.saksbehandling.vedtak.Rammevedtakskommandoer

/**
 * Se også [no.nav.tiltakspenger.saksbehandling.vedtak.Rammevedtakskommando]
 */
sealed interface RammevedtakKommandoDTO {
    val type: KommandoType

    enum class KommandoType {
        OMGJØR,
        OPPHØR,
        STANS,
    }

    data class Omgjør(
        val tvungenOmgjøringsperiode: PeriodeDTO,
    ) : RammevedtakKommandoDTO {
        override val type = KommandoType.OMGJØR
    }

    data class Opphør(
        val innvilgelsesperioder: List<PeriodeDTO>,
    ) : RammevedtakKommandoDTO {
        override val type = KommandoType.OPPHØR
    }

    data class Stans(
        val tidligsteFraOgMedDato: String,
        val tvungenStansTilOgMedDato: String,
    ) : RammevedtakKommandoDTO {
        override val type = KommandoType.STANS
    }
}

fun Rammevedtakskommandoer.toDTO(): Map<RammevedtakKommandoDTO.KommandoType, RammevedtakKommandoDTO> {
    return this.map { it.toDTO() }.associateBy { it.type }
}

fun Rammevedtakskommando.toDTO(): RammevedtakKommandoDTO {
    return when (this) {
        is Rammevedtakskommando.Omgjør -> RammevedtakKommandoDTO.Omgjør(
            tvungenOmgjøringsperiode = this.tvungenOmgjøringsperiode.toDTO(),
        )
        is Rammevedtakskommando.Opphør -> RammevedtakKommandoDTO.Opphør(
            innvilgelsesperioder = this.innvilgelsesperioder.map { it.toDTO() },
        )
        is Rammevedtakskommando.Stans -> RammevedtakKommandoDTO.Stans(
            tidligsteFraOgMedDato = this.tidligsteFraOgMedDato.toString(),
            tvungenStansTilOgMedDato = this.tvungenStansTilOgMedDato.toString(),
        )
    }
}
