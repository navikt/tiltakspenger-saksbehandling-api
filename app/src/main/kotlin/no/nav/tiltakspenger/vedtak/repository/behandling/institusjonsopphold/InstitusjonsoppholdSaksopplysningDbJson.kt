package no.nav.tiltakspenger.vedtak.repository.behandling.institusjonsopphold

import no.nav.tiltakspenger.libs.periodisering.PeriodeMedVerdi
import no.nav.tiltakspenger.libs.periodisering.Periodisering
import no.nav.tiltakspenger.saksbehandling.domene.vilkår.institusjonsopphold.InstitusjonsoppholdSaksopplysning
import no.nav.tiltakspenger.saksbehandling.domene.vilkår.institusjonsopphold.Opphold
import no.nav.tiltakspenger.vedtak.repository.felles.PeriodeDbJson
import no.nav.tiltakspenger.vedtak.repository.felles.SaksbehandlerDbJson
import no.nav.tiltakspenger.vedtak.repository.felles.toDbJson
import java.time.LocalDateTime

internal data class InstitusjonsoppholdSaksopplysningDbJson(
    val oppholdForPeriode: List<PeriodiseringAvOppholdDbJson>,
    val saksbehandler: SaksbehandlerDbJson?,
    val tidsstempel: String,
) {
    fun toDomain(): InstitusjonsoppholdSaksopplysning =
        when {
            saksbehandler != null -> {
                InstitusjonsoppholdSaksopplysning.Saksbehandler(
                    opphold =
                    Periodisering(
                        oppholdForPeriode.map {
                            PeriodeMedVerdi(
                                periode = it.periode.toDomain(),
                                verdi = it.opphold.toDomain(),
                            )
                        },
                    ),
                    navIdent = saksbehandler.navIdent,
                    tidsstempel = LocalDateTime.parse(tidsstempel),
                )
            }

            else -> {
                InstitusjonsoppholdSaksopplysning.Søknad(
                    opphold =
                    Periodisering(
                        oppholdForPeriode.map {
                            PeriodeMedVerdi(
                                periode = it.periode.toDomain(),
                                verdi = it.opphold.toDomain(),
                            )
                        },
                    ),
                    tidsstempel = LocalDateTime.parse(tidsstempel),
                )
            }
        }

    data class PeriodiseringAvOppholdDbJson(
        val periode: PeriodeDbJson,
        val opphold: OppholdDbJson,
    )

    enum class OppholdDbJson {
        OPPHOLD,
        IKKE_OPPHOLD,
        ;

        fun toDomain(): Opphold =
            when (this) {
                OPPHOLD -> Opphold.OPPHOLD
                IKKE_OPPHOLD -> Opphold.IKKE_OPPHOLD
            }
    }
}

internal fun InstitusjonsoppholdSaksopplysning.toDbJson(): InstitusjonsoppholdSaksopplysningDbJson =
    InstitusjonsoppholdSaksopplysningDbJson(
        oppholdForPeriode =
        this.opphold.perioderMedVerdi.map {
            InstitusjonsoppholdSaksopplysningDbJson.PeriodiseringAvOppholdDbJson(
                periode = it.periode.toDbJson(),
                opphold =
                when (it.verdi) {
                    Opphold.OPPHOLD -> InstitusjonsoppholdSaksopplysningDbJson.OppholdDbJson.OPPHOLD
                    Opphold.IKKE_OPPHOLD -> InstitusjonsoppholdSaksopplysningDbJson.OppholdDbJson.IKKE_OPPHOLD
                },
            )
        },
        saksbehandler = navIdent?.let { SaksbehandlerDbJson(it) },
        tidsstempel = tidsstempel.toString(),
    )
