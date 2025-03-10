package no.nav.tiltakspenger.vedtak.repository.sats

import no.nav.tiltakspenger.vedtak.repository.felles.PeriodeDbJson
import no.nav.tiltakspenger.vedtak.repository.felles.toDbJson
import no.nav.tiltakspenger.vedtak.utbetaling.domene.Sats

data class SatsDbJson(
    val periode: PeriodeDbJson,
    val sats: Int,
    val satsDelvis: Int,
    val satsBarnetillegg: Int,
    val satsBarnetilleggDelvis: Int,
) {
    fun toDomain(): Sats = Sats(
        periode = periode.toDomain(),
        sats = sats,
        satsRedusert = satsDelvis,
        satsBarnetillegg = satsBarnetillegg,
        satsBarnetilleggRedusert = satsBarnetilleggDelvis,
    )
}

internal fun Sats.toDbJson(): SatsDbJson =
    SatsDbJson(
        periode = periode.toDbJson(),
        sats = sats,
        satsDelvis = satsRedusert,
        satsBarnetillegg = satsBarnetillegg,
        satsBarnetilleggDelvis = satsBarnetilleggRedusert,
    )
