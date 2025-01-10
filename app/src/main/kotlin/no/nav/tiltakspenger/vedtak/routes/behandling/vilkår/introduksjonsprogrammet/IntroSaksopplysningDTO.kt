package no.nav.tiltakspenger.vedtak.routes.behandling.vilkår.introduksjonsprogrammet

import no.nav.tiltakspenger.libs.periodisering.PeriodeMedVerdi
import no.nav.tiltakspenger.saksbehandling.domene.vilkår.felles.Deltagelse
import no.nav.tiltakspenger.saksbehandling.domene.vilkår.introduksjonsprogrammet.IntroSaksopplysning
import no.nav.tiltakspenger.vedtak.routes.behandling.vilkår.kvp.PeriodeMedDeltagelseDTO
import no.nav.tiltakspenger.vedtak.routes.behandling.vilkår.kvp.toDTO

internal data class IntroSaksopplysningDTO(
    val periodeMedDeltagelse: PeriodeMedDeltagelseDTO,
    val kilde: IntroKildeDTO,
)

internal fun IntroSaksopplysning.toDTO(kilde: IntroKildeDTO): IntroSaksopplysningDTO =
    IntroSaksopplysningDTO(
        periodeMedDeltagelse =
        this.deltar.tilEnkelPeriode().toDTO(),
        kilde = kilde,
    )

internal fun List<PeriodeMedVerdi<Deltagelse>>.tilEnkelPeriode(): PeriodeMedVerdi<Deltagelse> {
    if (this.size > 1) {
        return this.single { it.verdi == Deltagelse.DELTAR }
    }
    return this.single()
}
