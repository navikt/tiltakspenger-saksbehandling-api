package no.nav.tiltakspenger.vedtak.routes.behandling.vilkår.alder

import no.nav.tiltakspenger.libs.periodisering.PeriodeMedVerdi
import no.nav.tiltakspenger.saksbehandling.domene.vilkår.alder.AlderSaksopplysning
import no.nav.tiltakspenger.saksbehandling.domene.vilkår.felles.Deltagelse
import java.time.LocalDate

internal data class AlderSaksopplysningDTO(
    val fødselsdato: LocalDate,
    val kilde: AlderKildeDTO,
)

internal fun AlderSaksopplysning.toDTO(kilde: AlderKildeDTO): AlderSaksopplysningDTO =
    AlderSaksopplysningDTO(
        fødselsdato = fødselsdato,
        kilde = kilde,
    )

internal fun List<PeriodeMedVerdi<Deltagelse>>.tilEnkelPeriode(): PeriodeMedVerdi<Deltagelse> {
    if (this.size > 1) {
        return this.single { it.verdi == Deltagelse.DELTAR }
    }
    return this.single()
}
