package no.nav.tiltakspenger.vedtak.repository.behandling.introduksjonsprogrammet

import no.nav.tiltakspenger.libs.periodisering.PeriodeMedVerdi
import no.nav.tiltakspenger.libs.periodisering.Periodisering
import no.nav.tiltakspenger.saksbehandling.domene.vilkår.felles.Deltagelse
import no.nav.tiltakspenger.saksbehandling.domene.vilkår.felles.ÅrsakTilEndring
import no.nav.tiltakspenger.saksbehandling.domene.vilkår.introduksjonsprogrammet.IntroSaksopplysning
import no.nav.tiltakspenger.vedtak.repository.felles.PeriodeDbJson
import no.nav.tiltakspenger.vedtak.repository.felles.SaksbehandlerDbJson
import no.nav.tiltakspenger.vedtak.repository.felles.toDbJson
import java.time.LocalDateTime

internal data class IntroSaksopplysningDbJson(
    val deltakelseForPeriode: List<PeriodiseringAvDeltagelseDbJson>,
    val årsakTilEndring: ÅrsakTilEndringDbJson?,
    val saksbehandler: SaksbehandlerDbJson?,
    val tidsstempel: String,
) {
    fun toDomain(): IntroSaksopplysning {
        return when {
            saksbehandler != null -> IntroSaksopplysning.Saksbehandler(
                deltar = Periodisering(
                    deltakelseForPeriode.map {
                        PeriodeMedVerdi(
                            periode = it.periode.toDomain(),
                            verdi = it.deltar.toDomain(),
                        )
                    },
                ),
                årsakTilEndring = årsakTilEndring!!.toDomain(),
                saksbehandler = saksbehandler.toDomain(),
                tidsstempel = LocalDateTime.parse(tidsstempel),
            )

            else -> {
                require(årsakTilEndring == null) { "Støtter ikke årsak til endring for IntroSaksopplysning.Søknad." }
                IntroSaksopplysning.Søknad(
                    deltar = Periodisering(
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
    }

    data class PeriodiseringAvDeltagelseDbJson(
        val periode: PeriodeDbJson,
        val deltar: DeltagelseDbJson,
    )

    enum class ÅrsakTilEndringDbJson {
        FEIL_I_INNHENTET_DATA,
        ENDRING_ETTER_SØKNADSTIDSPUNKT,
        ;

        fun toDomain(): ÅrsakTilEndring {
            return when (this) {
                FEIL_I_INNHENTET_DATA -> ÅrsakTilEndring.FEIL_I_INNHENTET_DATA
                ENDRING_ETTER_SØKNADSTIDSPUNKT -> ÅrsakTilEndring.ENDRING_ETTER_SØKNADSTIDSPUNKT
            }
        }
    }

    enum class DeltagelseDbJson {
        DELTAR,
        DELTAR_IKKE,
        ;

        fun toDomain(): Deltagelse {
            return when (this) {
                DELTAR -> Deltagelse.DELTAR
                DELTAR_IKKE -> Deltagelse.DELTAR_IKKE
            }
        }
    }
}

internal fun IntroSaksopplysning.toDbJson(): IntroSaksopplysningDbJson {
    return IntroSaksopplysningDbJson(
        deltakelseForPeriode = this.deltar.perioder().map {
            IntroSaksopplysningDbJson.PeriodiseringAvDeltagelseDbJson(
                periode = it.periode.toDbJson(),
                deltar = when (it.verdi) {
                    Deltagelse.DELTAR -> IntroSaksopplysningDbJson.DeltagelseDbJson.DELTAR
                    Deltagelse.DELTAR_IKKE -> IntroSaksopplysningDbJson.DeltagelseDbJson.DELTAR_IKKE
                },
            )
        },
        årsakTilEndring = when (årsakTilEndring) {
            ÅrsakTilEndring.FEIL_I_INNHENTET_DATA -> IntroSaksopplysningDbJson.ÅrsakTilEndringDbJson.FEIL_I_INNHENTET_DATA
            ÅrsakTilEndring.ENDRING_ETTER_SØKNADSTIDSPUNKT -> IntroSaksopplysningDbJson.ÅrsakTilEndringDbJson.ENDRING_ETTER_SØKNADSTIDSPUNKT
            null -> null
        },
        saksbehandler = saksbehandler?.toDbJson(),
        tidsstempel = tidsstempel.toString(),
    )
}
