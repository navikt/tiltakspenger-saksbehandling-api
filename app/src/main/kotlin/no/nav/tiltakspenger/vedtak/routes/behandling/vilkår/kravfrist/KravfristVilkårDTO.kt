package no.nav.tiltakspenger.vedtak.routes.behandling.vilkår.kravfrist

import no.nav.tiltakspenger.libs.periodisering.PeriodeDTO
import no.nav.tiltakspenger.libs.periodisering.toDTO
import no.nav.tiltakspenger.saksbehandling.domene.vilkår.kravfrist.KravfristVilkår
import no.nav.tiltakspenger.vedtak.routes.behandling.LovreferanseDTO
import no.nav.tiltakspenger.vedtak.routes.behandling.toDTO
import no.nav.tiltakspenger.vedtak.routes.behandling.vilkår.SamletUtfallDTO
import no.nav.tiltakspenger.vedtak.routes.behandling.vilkår.toDTO

/**
 * Har ansvar for å serialisere Vilkårssett til json. Kontrakt mot frontend.
 */
internal data class KravfristVilkårDTO(
    val søknadSaksopplysning: KravfristSaksopplysningDTO,
    val avklartSaksopplysning: KravfristSaksopplysningDTO,
    val vilkårLovreferanse: LovreferanseDTO,
    val utfallperiode: PeriodeDTO,
    val samletUtfall: SamletUtfallDTO,
)

internal fun KravfristVilkår.toDTO(): KravfristVilkårDTO =
    KravfristVilkårDTO(
        søknadSaksopplysning = søknadSaksopplysning.toDTO(KravfristKildeDTO.SØKNAD),
        avklartSaksopplysning =
        avklartSaksopplysning.toDTO(
            if (avklartSaksopplysning ==
                søknadSaksopplysning
            ) {
                KravfristKildeDTO.SØKNAD
            } else {
                KravfristKildeDTO.SAKSBEHANDLER
            },
        ),
        vilkårLovreferanse = lovreferanse.toDTO(),
        utfallperiode = this.utfall.totalePeriode.toDTO(),
        samletUtfall = this.samletUtfall().toDTO(),
    )
