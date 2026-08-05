package no.nav.tiltakspenger.saksbehandling.sak.infra.routes

import no.nav.tiltakspenger.saksbehandling.behandling.domene.Revurdering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Søknadsbehandling
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.TilbakekrevingBehandlingsstatus
import java.time.LocalDateTime

enum class ÅpenBehandlingTypeDTO {
    SØKNAD,
    SØKNADSBEHANDLING,
    REVURDERING,
    MELDEKORT,
    KLAGE,
    TILBAKEKREVING,
}

/**
 * En minimal peker til noe som er åpent for behandling eller beslutning.
 * Frontend slår opp resten av dataene i de øvrige listene på [SakDTO] ved hjelp av [id] og [type].
 */
data class ÅpenBehandlingDTO(
    val id: String,
    val type: ÅpenBehandlingTypeDTO,
)

/**
 * Returnerer id og type for søknader, rammebehandlinger, meldekortbehandlinger, klagebehandlinger og tilbakekrevinger som er åpne for behandling eller beslutning.
 * Sortert med nyest opprettet først.
 */
fun Sak.tilÅpneBehandlingerDTO(): List<ÅpenBehandlingDTO> {
    return (
        this.åpneSøknaderUtenBehandling() +
            this.åpneRammebehandlinger() +
            this.åpneMeldekortbehandlinger() +
            this.åpneKlagebehandlinger() +
            this.åpneTilbakekrevinger()
        )
        .sortedByDescending { it.second }
        .map { it.first }
}

/**
 * Søknader som ikke har en tilknyttet søknadsbehandling.
 * Normalt skal det opprettes søknadsbehandlinger automatisk for nye søknader, men vi tar med denne for å liste ut evt. søknader der dette har feilet.
 */
private fun Sak.åpneSøknaderUtenBehandling(): List<Pair<ÅpenBehandlingDTO, LocalDateTime>> {
    return this.søknader
        .filter { søknad ->
            !søknad.erAvbrutt && rammebehandlinger.søknadsbehandlinger.none { it.søknad.id == søknad.id }
        }
        .map { ÅpenBehandlingDTO(it.id.toString(), ÅpenBehandlingTypeDTO.SØKNAD) to it.opprettet }
}

private fun Sak.åpneRammebehandlinger(): List<Pair<ÅpenBehandlingDTO, LocalDateTime>> {
    return this.rammebehandlinger.åpneBehandlinger.map {
        val type = when (it) {
            is Søknadsbehandling -> ÅpenBehandlingTypeDTO.SØKNADSBEHANDLING
            is Revurdering -> ÅpenBehandlingTypeDTO.REVURDERING
        }
        ÅpenBehandlingDTO(it.id.toString(), type) to it.opprettet
    }
}

private fun Sak.åpneMeldekortbehandlinger(): List<Pair<ÅpenBehandlingDTO, LocalDateTime>> {
    return this.meldekortbehandlinger.åpneMeldekortbehandlinger.map {
        ÅpenBehandlingDTO(it.id.toString(), ÅpenBehandlingTypeDTO.MELDEKORT) to it.opprettet
    }
}

private fun Sak.åpneKlagebehandlinger(): List<Pair<ÅpenBehandlingDTO, LocalDateTime>> {
    return this.behandlinger.klagebehandlinger
        .filter { it.erÅpen }
        .map { ÅpenBehandlingDTO(it.id.toString(), ÅpenBehandlingTypeDTO.KLAGE) to it.opprettet }
}

private fun Sak.åpneTilbakekrevinger(): List<Pair<ÅpenBehandlingDTO, LocalDateTime>> {
    return this.tilbakekrevinger
        .filter { it.status != TilbakekrevingBehandlingsstatus.AVSLUTTET }
        .map { ÅpenBehandlingDTO(it.id.toString(), ÅpenBehandlingTypeDTO.TILBAKEKREVING) to it.opprettet }
}
