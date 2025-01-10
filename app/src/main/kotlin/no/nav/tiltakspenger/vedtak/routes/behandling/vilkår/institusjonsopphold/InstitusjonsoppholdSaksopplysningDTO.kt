package no.nav.tiltakspenger.vedtak.routes.behandling.vilkår.institusjonsopphold

import no.nav.tiltakspenger.libs.periodisering.PeriodeMedVerdi
import no.nav.tiltakspenger.saksbehandling.domene.vilkår.institusjonsopphold.InstitusjonsoppholdSaksopplysning
import no.nav.tiltakspenger.saksbehandling.domene.vilkår.institusjonsopphold.Opphold

internal data class InstitusjonsoppholdSaksopplysningDTO(
    val periodeMedOpphold: PeriodeMedOppholdDTO,
    val kilde: KildeDTO,
)

internal fun InstitusjonsoppholdSaksopplysning.toDTO(kilde: KildeDTO): InstitusjonsoppholdSaksopplysningDTO =
    InstitusjonsoppholdSaksopplysningDTO(
        periodeMedOpphold = this.opphold.tilEnkelPeriode().toDTO(),
        kilde = kilde,
    )

internal fun List<PeriodeMedVerdi<Opphold>>.tilEnkelPeriode(): PeriodeMedVerdi<Opphold> {
    if (this.size > 1) {
        return this.single { it.verdi == Opphold.OPPHOLD }
    }
    return this.single()
}
