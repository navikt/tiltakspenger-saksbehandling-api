package no.nav.tiltakspenger.vedtak.routes.behandling.vilkår.kvp

import no.nav.tiltakspenger.libs.periodisering.PeriodeDTO
import no.nav.tiltakspenger.libs.periodisering.toDTO
import no.nav.tiltakspenger.saksbehandling.domene.vilkår.kvp.KVPVilkår
import no.nav.tiltakspenger.vedtak.routes.behandling.dto.LovreferanseDTO
import no.nav.tiltakspenger.vedtak.routes.behandling.dto.toDTO
import no.nav.tiltakspenger.vedtak.routes.behandling.vilkår.SamletUtfallDTO
import no.nav.tiltakspenger.vedtak.routes.behandling.vilkår.toDTO

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

internal fun KVPVilkår.toDTO(): KVPVilkårDTO =
    KVPVilkårDTO(
        søknadSaksopplysning = søknadSaksopplysning.toDTO(KildeDTO.SØKNAD),
        avklartSaksopplysning =
        avklartSaksopplysning.toDTO(
            if (avklartSaksopplysning ==
                søknadSaksopplysning
            ) {
                KildeDTO.SØKNAD
            } else {
                KildeDTO.SAKSBEHANDLER
            },
        ),
        vilkårLovreferanse = lovreferanse.toDTO(),
        utfallperiode = this.utfall.totalePeriode.toDTO(),
        samletUtfall = this.samletUtfall().toDTO(),
    )
