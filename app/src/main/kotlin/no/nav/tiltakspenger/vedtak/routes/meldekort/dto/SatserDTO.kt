package no.nav.tiltakspenger.vedtak.routes.meldekort.dto

import no.nav.tiltakspenger.libs.periodisering.PeriodeDTO
import no.nav.tiltakspenger.libs.periodisering.toDTO
import no.nav.tiltakspenger.vedtak.utbetaling.domene.Sats

data class SatsDTO(
    val periode: PeriodeDTO,
    val sats: Int,
    val satsDelvis: Int,
)

fun Sats.toDTO(): SatsDTO = SatsDTO(periode = periode.toDTO(), sats = sats, satsDelvis = satsRedusert)
