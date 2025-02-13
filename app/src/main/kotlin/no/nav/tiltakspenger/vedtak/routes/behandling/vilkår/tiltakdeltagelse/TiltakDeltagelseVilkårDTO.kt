package no.nav.tiltakspenger.vedtak.routes.behandling.vilkår.tiltakdeltagelse

import no.nav.tiltakspenger.libs.periodisering.PeriodeDTO
import no.nav.tiltakspenger.libs.periodisering.toDTO
import no.nav.tiltakspenger.saksbehandling.domene.vilkår.tiltaksdeltagelse.TiltaksdeltagelseVilkår
import no.nav.tiltakspenger.vedtak.routes.behandling.dto.LovreferanseDTO
import no.nav.tiltakspenger.vedtak.routes.behandling.dto.toDTO
import no.nav.tiltakspenger.vedtak.routes.behandling.vilkår.SamletUtfallDTO
import no.nav.tiltakspenger.vedtak.routes.behandling.vilkår.toDTO

/**
 * Har ansvar for å serialisere Tiltakvilkår til json. Kontrakt mot frontend.
 */
internal data class TiltakDeltagelseVilkårDTO(
    val registerSaksopplysning: TiltakDeltagelseSaksopplysningDTO,
    val saksbehandlerSaksopplysning: TiltakDeltagelseSaksopplysningDTO?,
    val avklartSaksopplysning: TiltakDeltagelseSaksopplysningDTO,
    val vilkårLovreferanse: LovreferanseDTO,
    val utfallperiode: PeriodeDTO,
    val samletUtfall: SamletUtfallDTO,
)

internal fun TiltaksdeltagelseVilkår.toDTO(): TiltakDeltagelseVilkårDTO =
    TiltakDeltagelseVilkårDTO(
        registerSaksopplysning = registerSaksopplysning.toDTO(),
        saksbehandlerSaksopplysning = saksbehandlerSaksopplysning?.toDTO(),
        avklartSaksopplysning = avklartSaksopplysning.toDTO(),
        vilkårLovreferanse = lovreferanse.toDTO(),
        utfallperiode = this.utfall.totalePeriode.toDTO(),
        samletUtfall = this.samletUtfall().toDTO(),
    )
