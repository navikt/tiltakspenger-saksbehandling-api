package no.nav.tiltakspenger.vedtak.repository.behandling.introduksjonsprogrammet

import no.nav.tiltakspenger.libs.periodisering.PeriodeMedVerdi
import no.nav.tiltakspenger.libs.periodisering.Periodisering
import no.nav.tiltakspenger.saksbehandling.domene.vilkår.felles.Deltagelse
import no.nav.tiltakspenger.saksbehandling.domene.vilkår.introduksjonsprogrammet.IntroSaksopplysning
import no.nav.tiltakspenger.vedtak.repository.felles.PeriodeDbJson
import no.nav.tiltakspenger.vedtak.repository.felles.SaksbehandlerDbJson
import no.nav.tiltakspenger.vedtak.repository.felles.toDbJson
import java.time.LocalDateTime

internal data class IntroSaksopplysningDbJson(
    val deltakelseForPeriode: List<PeriodiseringAvDeltagelseDbJson>,
    val saksbehandler: SaksbehandlerDbJson?,
    val tidsstempel: String,
) {
    fun toDomain(): IntroSaksopplysning =
        when {
            saksbehandler != null -> {
                IntroSaksopplysning.Saksbehandler(
                    deltar =
                    Periodisering(
                        deltakelseForPeriode.map {
                            PeriodeMedVerdi(
                                periode = it.periode.toDomain(),
                                verdi = it.deltar.toDomain(),
                            )
                        },
                    ),
                    navIdent = saksbehandler.navIdent,
                    tidsstempel = LocalDateTime.parse(tidsstempel),
                )
            }

            else -> {
                IntroSaksopplysning.Søknad(
                    deltar =
                    Periodisering(
                        deltakelseForPeriode.map {
                            PeriodeMedVerdi(
                                periode = it.periode.toDomain(),
                                verdi = it.deltar.toDomain(),
                            )
                        },
                    ),
                    tidsstempel = LocalDateTime.parse(tidsstempel),
                )
            }
        }

    data class PeriodiseringAvDeltagelseDbJson(
        val periode: PeriodeDbJson,
        val deltar: DeltagelseDbJson,
    )

    enum class DeltagelseDbJson {
        DELTAR,
        DELTAR_IKKE,
        ;

        fun toDomain(): Deltagelse =
            when (this) {
                DELTAR -> Deltagelse.DELTAR
                DELTAR_IKKE -> Deltagelse.DELTAR_IKKE
            }
    }
}

internal fun IntroSaksopplysning.toDbJson(): IntroSaksopplysningDbJson =
    IntroSaksopplysningDbJson(
        deltakelseForPeriode =
        this.deltar.perioderMedVerdi.map {
            IntroSaksopplysningDbJson.PeriodiseringAvDeltagelseDbJson(
                periode = it.periode.toDbJson(),
                deltar =
                when (it.verdi) {
                    Deltagelse.DELTAR -> IntroSaksopplysningDbJson.DeltagelseDbJson.DELTAR
                    Deltagelse.DELTAR_IKKE -> IntroSaksopplysningDbJson.DeltagelseDbJson.DELTAR_IKKE
                },
            )
        },
        saksbehandler = navIdent?.let { SaksbehandlerDbJson(it) },
        tidsstempel = tidsstempel.toString(),
    )
