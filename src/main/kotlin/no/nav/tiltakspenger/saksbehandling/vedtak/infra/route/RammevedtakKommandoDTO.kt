package no.nav.tiltakspenger.saksbehandling.vedtak.infra.route

import no.nav.tiltakspenger.libs.periodisering.PeriodeDTO
import no.nav.tiltakspenger.libs.periodisering.toDTO
import no.nav.tiltakspenger.saksbehandling.vedtak.Rammevedtakskommando
import no.nav.tiltakspenger.saksbehandling.vedtak.Rammevedtakskommandoer

/**
 * Se også [no.nav.tiltakspenger.saksbehandling.vedtak.Rammevedtakskommando]
 */
sealed interface RammevedtakKommandoDTO {
    val type: String

    data class Omgjør(
        val tvungenOmgjøringsperiode: PeriodeDTO,
    ) : RammevedtakKommandoDTO {
        override val type: String = "OMGJØR"
    }

    data class Opphør(
        val innvilgelsesperioder: List<PeriodeDTO>,
    ) : RammevedtakKommandoDTO {
        override val type: String = "OPPHØR"
    }

    data class Stans(
        val tidligsteFraOgMedDato: String,
        val tvungenStansTilOgMedDato: String,
    ) : RammevedtakKommandoDTO {
        override val type: String = "STANS"
    }
}

fun Rammevedtakskommandoer.toDTO(): Set<RammevedtakKommandoDTO> {
    return this.map { it.toDTO() }.toSet()
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
