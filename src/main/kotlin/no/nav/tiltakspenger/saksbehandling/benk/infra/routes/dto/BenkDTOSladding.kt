package no.nav.tiltakspenger.saksbehandling.benk.infra.routes.dto

import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.saksbehandling.infra.route.SladdetVerdi
import no.nav.tiltakspenger.saksbehandling.infra.route.skalSladdeFor

/**
 * Sladding av [BenkResponsDTO].
 * Hver rad i benken bærer fødselsnummeret til personen saken gjelder, og ventestatusen bærer saksbehandlers begrunnelse.
 * Antallene, statusene, beløpene og identene i benken er ikke identifiserende og beholdes.
 */

fun BenkResponsDTO.sladdet(): BenkResponsDTO = this.copy(
    oversikt = oversikt.sladdet(),
)

private fun BenkOversiktDTO.sladdet(): BenkOversiktDTO = this.copy(
    behandlinger = behandlinger.map { it.sladdet() },
)

private fun BenkBehandlingDTO.sladdet(): BenkBehandlingDTO = when (this) {
    is BenkSøknadsbehandlingDTO -> this.copy(fnr = SladdetVerdi, ventestatus = ventestatus.sladdet())
    is BenkRevurderingDTO -> this.copy(fnr = SladdetVerdi, ventestatus = ventestatus.sladdet())
    is BenkMeldekortDTO -> this.copy(fnr = SladdetVerdi, ventestatus = ventestatus.sladdet())
    is BenkKlagebehandlingDTO -> this.copy(fnr = SladdetVerdi, ventestatus = ventestatus.sladdet())
    is BenkTilbakekrevingDTO -> this.copy(fnr = SladdetVerdi, ventestatus = ventestatus.sladdet())
}

private fun BenkVentestatusDTO.sladdet(): BenkVentestatusDTO = this.copy(
    begrunnelse = SladdetVerdi,
)

fun BenkResponsDTO.sladdetFor(saksbehandler: Saksbehandler): BenkResponsDTO =
    if (skalSladdeFor(saksbehandler)) sladdet() else this
