package no.nav.tiltakspenger.vedtak.routes.behandling.vilkår.livsopphold

import no.nav.tiltakspenger.saksbehandling.domene.vilkår.livsopphold.LivsoppholdSaksopplysning
import java.time.LocalDateTime

/**
 * Har ansvar for å serialisere Vilkårssett til json. Kontrakt mot frontend.
 */
internal data class LivsoppholdSaksopplysningDTO(
    val harLivsoppholdYtelser: Boolean,
    val tidspunkt: LocalDateTime,
)

internal fun LivsoppholdSaksopplysning.toDTO(): LivsoppholdSaksopplysningDTO =
    LivsoppholdSaksopplysningDTO(
        harLivsoppholdYtelser = this.harLivsoppholdYtelser,
        tidspunkt = tidsstempel,
    )
