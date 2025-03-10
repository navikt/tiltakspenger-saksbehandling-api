package no.nav.tiltakspenger.saksbehandling.repository.behandling.felles

import no.nav.tiltakspenger.libs.periodisering.PeriodeMedVerdi
import no.nav.tiltakspenger.libs.periodisering.Periodisering
import no.nav.tiltakspenger.saksbehandling.repository.felles.PeriodeDbJson
import no.nav.tiltakspenger.saksbehandling.repository.felles.toDbJson
import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.vilkår.Utfallsperiode

internal data class PeriodisertUtfallDbJson(
    val utfall: UtfallDbJson,
    val periode: PeriodeDbJson,
) {
    enum class UtfallDbJson {
        // TODO: Lag db-migreringskript fra OPPFYLT->RETT_TIL_TILTAKSPENGER og IKKE_OPPFYLT->IKKE_RETT_TIL_TILTAKSPENGER og rename disse.
        OPPFYLT,
        IKKE_OPPFYLT,
        ;

        fun toDomain(): Utfallsperiode =
            when (this) {
                OPPFYLT -> Utfallsperiode.RETT_TIL_TILTAKSPENGER
                IKKE_OPPFYLT -> Utfallsperiode.IKKE_RETT_TIL_TILTAKSPENGER
            }
    }
}

internal fun Periodisering<Utfallsperiode>.toDbJson(): List<PeriodisertUtfallDbJson> {
    return this.perioderMedVerdi.map {
        PeriodisertUtfallDbJson(
            utfall = it.verdi.toDbJson(),
            periode = it.periode.toDbJson(),
        )
    }
}

internal fun Utfallsperiode.toDbJson(): PeriodisertUtfallDbJson.UtfallDbJson =
    when (this) {
        Utfallsperiode.RETT_TIL_TILTAKSPENGER -> PeriodisertUtfallDbJson.UtfallDbJson.OPPFYLT
        Utfallsperiode.IKKE_RETT_TIL_TILTAKSPENGER -> PeriodisertUtfallDbJson.UtfallDbJson.IKKE_OPPFYLT
    }

internal fun List<PeriodisertUtfallDbJson>.toDomain(): Periodisering<Utfallsperiode> =
    Periodisering(
        this.map {
            PeriodeMedVerdi(
                periode = it.periode.toDomain(),
                verdi = it.utfall.toDomain(),
            )
        },
    )
