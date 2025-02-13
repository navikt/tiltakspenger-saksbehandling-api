package no.nav.tiltakspenger.vedtak.routes.behandling.vilkår.livsopphold

import no.nav.tiltakspenger.libs.periodisering.PeriodeDTO
import no.nav.tiltakspenger.libs.periodisering.toDTO
import no.nav.tiltakspenger.saksbehandling.domene.vilkår.livsopphold.LivsoppholdVilkår
import no.nav.tiltakspenger.vedtak.routes.behandling.dto.LovreferanseDTO
import no.nav.tiltakspenger.vedtak.routes.behandling.dto.toDTO
import no.nav.tiltakspenger.vedtak.routes.behandling.vilkår.SamletUtfallDTO

internal data class LivsoppholdVilkårDTO(
    val avklartSaksopplysning: LivsoppholdSaksopplysningDTO?,
    val vilkårLovreferanse: LovreferanseDTO,
    val utfallperiode: PeriodeDTO,
    val samletUtfall: SamletUtfallDTO,
)

internal fun LivsoppholdVilkår.toDTO(): LivsoppholdVilkårDTO {
    val samletUtfall =
        when (avklartSaksopplysning?.harLivsoppholdYtelser) {
            true -> SamletUtfallDTO.IKKE_OPPFYLT
            false -> SamletUtfallDTO.OPPFYLT
            null -> SamletUtfallDTO.UAVKLART
        }

    return LivsoppholdVilkårDTO(
        avklartSaksopplysning = avklartSaksopplysning?.toDTO(),
        vilkårLovreferanse = lovreferanse.toDTO(),
        utfallperiode = this.utfall.totalePeriode.toDTO(),
        samletUtfall = samletUtfall,
    )
}
