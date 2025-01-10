package no.nav.tiltakspenger.vedtak.routes.behandling.vilkår.kravfrist

import no.nav.tiltakspenger.saksbehandling.domene.vilkår.kravfrist.KravfristSaksopplysning
import java.time.LocalDateTime

internal data class KravfristSaksopplysningDTO(
    val kravdato: LocalDateTime,
    val kilde: KravfristKildeDTO,
)

internal fun KravfristSaksopplysning.toDTO(kilde: KravfristKildeDTO): KravfristSaksopplysningDTO =
    KravfristSaksopplysningDTO(
        kravdato = kravdato,
        kilde = kilde,
    )
