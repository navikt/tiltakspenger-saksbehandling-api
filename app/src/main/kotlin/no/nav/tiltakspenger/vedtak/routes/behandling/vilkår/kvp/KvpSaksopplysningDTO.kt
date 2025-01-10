package no.nav.tiltakspenger.vedtak.routes.behandling.vilkår.kvp

import no.nav.tiltakspenger.libs.periodisering.PeriodeMedVerdi
import no.nav.tiltakspenger.saksbehandling.domene.vilkår.felles.Deltagelse
import no.nav.tiltakspenger.saksbehandling.domene.vilkår.kvp.KvpSaksopplysning

internal data class KvpSaksopplysningDTO(
    val periodeMedDeltagelse: PeriodeMedDeltagelseDTO,
    val kilde: KildeDTO,
)

internal fun KvpSaksopplysning.toDTO(kilde: KildeDTO): KvpSaksopplysningDTO =
    KvpSaksopplysningDTO(
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
