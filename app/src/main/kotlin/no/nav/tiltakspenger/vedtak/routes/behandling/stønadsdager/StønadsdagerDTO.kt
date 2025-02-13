package no.nav.tiltakspenger.vedtak.routes.behandling.stønadsdager

import no.nav.tiltakspenger.saksbehandling.domene.stønadsdager.Stønadsdager
import no.nav.tiltakspenger.vedtak.routes.behandling.dto.LovreferanseDTO
import no.nav.tiltakspenger.vedtak.routes.behandling.dto.toDTO

/**
 * Har ansvar for å serialisere Stønadsdager til json. Kontrakt mot frontend.
 */
internal data class StønadsdagerDTO(
    val registerSaksopplysning: StønadsdagerSaksopplysningDTO,
    val lovreferanse: LovreferanseDTO,
)

internal fun Stønadsdager.toDTO(): StønadsdagerDTO =
    StønadsdagerDTO(
        registerSaksopplysning = registerSaksopplysning.toDTO(),
        lovreferanse = lovreferanse.toDTO(),
    )
