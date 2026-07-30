package no.nav.tiltakspenger.saksbehandling.sak.infra.routes

import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.sak.infra.routes.ÅpenBehandlingDTO.ÅpenBehandlingTypeDTO

/**
 * En minimal peker til en åpen behandling.
 * Frontend slår opp resten av dataene i de øvrige listene på [SakDTO] ved hjelp av [id] og [type].
 */
data class ÅpenBehandlingIdDTO(
    val id: String,
    val type: ÅpenBehandlingTypeDTO,
)

/**
 * Samme utvalg og sortering som [tilÅpneBehandlingerDTO], men kun id og type.
 */
fun Sak.tilÅpneBehandlingerIderDTO(): List<ÅpenBehandlingIdDTO> =
    this.tilÅpneBehandlingerDTO().map { ÅpenBehandlingIdDTO(id = it.id, type = it.type) }
