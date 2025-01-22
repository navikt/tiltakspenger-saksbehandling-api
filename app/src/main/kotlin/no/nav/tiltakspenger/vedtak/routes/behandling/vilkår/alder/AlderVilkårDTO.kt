package no.nav.tiltakspenger.vedtak.routes.behandling.vilkår.alder

import no.nav.tiltakspenger.libs.periodisering.PeriodeDTO
import no.nav.tiltakspenger.libs.periodisering.toDTO
import no.nav.tiltakspenger.saksbehandling.domene.vilkår.alder.AlderVilkår
import no.nav.tiltakspenger.vedtak.routes.behandling.LovreferanseDTO
import no.nav.tiltakspenger.vedtak.routes.behandling.toDTO
import no.nav.tiltakspenger.vedtak.routes.behandling.vilkår.SamletUtfallDTO
import no.nav.tiltakspenger.vedtak.routes.behandling.vilkår.toDTO

/**
 * Har ansvar for å serialisere Aldervilkår til json. Kontrakt mot frontend.
 */
internal data class AlderVilkårDTO(
    val registerSaksopplysning: AlderSaksopplysningDTO,
    val avklartSaksopplysning: AlderSaksopplysningDTO,
    val vilkårLovreferanse: LovreferanseDTO,
    val utfallperiode: PeriodeDTO,
    val samletUtfall: SamletUtfallDTO,
)

internal fun AlderVilkår.toDTO(): AlderVilkårDTO =
    AlderVilkårDTO(
        registerSaksopplysning = registerSaksopplysning.toDTO(AlderKildeDTO.PDL),
        avklartSaksopplysning =
        avklartSaksopplysning.toDTO(
            if (avklartSaksopplysning ==
                registerSaksopplysning
            ) {
                AlderKildeDTO.PDL
            } else {
                AlderKildeDTO.SAKSBEHANDLER
            },
        ),
        vilkårLovreferanse = lovreferanse.toDTO(),
        utfallperiode = this.utfall.totalePeriode.toDTO(),
        samletUtfall = this.samletUtfall().toDTO(),
    )
