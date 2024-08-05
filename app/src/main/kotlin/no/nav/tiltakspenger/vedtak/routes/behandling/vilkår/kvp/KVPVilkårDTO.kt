package no.nav.tiltakspenger.vedtak.routes.behandling.vilkår.kvp

import no.nav.tiltakspenger.saksbehandling.domene.vilkår.kvp.KVPVilkår
import no.nav.tiltakspenger.vedtak.routes.behandling.LovreferanseDTO
import no.nav.tiltakspenger.vedtak.routes.behandling.toDTO
import no.nav.tiltakspenger.vedtak.routes.behandling.vilkår.SamletUtfallDTO
import no.nav.tiltakspenger.vedtak.routes.behandling.vilkår.kravfrist.toDTO
import no.nav.tiltakspenger.vedtak.routes.behandling.vilkår.toDTO
import no.nav.tiltakspenger.vedtak.routes.dto.PeriodeDTO
import no.nav.tiltakspenger.vedtak.routes.dto.toDTO

/**
 * Har ansvar for å serialisere Vilkårssett til json. Kontrakt mot frontend.
 */
internal data class KVPVilkårDTO(
    val søknadSaksopplysning: KvpSaksopplysningDTO,
    val avklartSaksopplysning: KvpSaksopplysningDTO,
    val vilkårLovreferanse: LovreferanseDTO,
    val utfallperiode: PeriodeDTO,
    val samletUtfall: SamletUtfallDTO,
)

internal fun KVPVilkår.toDTO(): KVPVilkårDTO {
    return KVPVilkårDTO(
        søknadSaksopplysning = søknadSaksopplysning.toDTO(KildeDTO.SØKNAD),
        avklartSaksopplysning = avklartSaksopplysning.toDTO(if (avklartSaksopplysning == søknadSaksopplysning) KildeDTO.SØKNAD else KildeDTO.SAKSBEHANDLER),
        vilkårLovreferanse = lovreferanse.toDTO(),
        utfallperiode = this.utfall().totalePeriode.toDTO(),
        samletUtfall = this.samletUtfall().toDTO(),
    )
}
