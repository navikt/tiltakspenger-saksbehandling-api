package no.nav.tiltakspenger.saksbehandling.behandling.domene.tilBeslutter

import no.nav.tiltakspenger.saksbehandling.behandling.domene.OmgjøringsgrunnlagEndret
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandling
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.KanIkkeIverksetteUtbetaling
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.KunneIkkeSimulere

sealed interface KanIkkeSendeRammebehandlingTilBeslutter {
    data class BehandlingenEiesAvAnnenSaksbehandler(val eiesAvSaksbehandler: String?) : KanIkkeSendeRammebehandlingTilBeslutter

    data object MåVæreUnderBehandlingEllerAutomatisk : KanIkkeSendeRammebehandlingTilBeslutter

    data object ErPaVent : KanIkkeSendeRammebehandlingTilBeslutter

    /** Resultatet mangler eller er ikke ferdig utfylt, for eksempel uten vedtaksperiode. */
    data object ErIkkeFerdigutfylt : KanIkkeSendeRammebehandlingTilBeslutter

    data class UtbetalingFeil(val feil: KanIkkeIverksetteUtbetaling, val sak: Sak, val behandling: Rammebehandling) : KanIkkeSendeRammebehandlingTilBeslutter

    data class SimuleringFeil(val feil: KunneIkkeSimulere) : KanIkkeSendeRammebehandlingTilBeslutter

    data object UgyldigeMeldeperioderHelg : KanIkkeSendeRammebehandlingTilBeslutter

    /** Et annet vedtak har omgjort de samme periodene etter at omgjøringen sist ble oppdatert. */
    data class OmgjøringsgrunnlagetErEndret(val endring: OmgjøringsgrunnlagEndret) : KanIkkeSendeRammebehandlingTilBeslutter

    /** Notatet som journalføres er begrunnelsen for vilkårsvurderingen, så den kan ikke være tom når saksbehandler har valgt å journalføre. */
    data object MåHaBegrunnelseForÅJournalføreNotat : KanIkkeSendeRammebehandlingTilBeslutter

    /** Saksbehandler har valgt å journalføre begrunnelsen for barnetillegg, men behandlingen har ingen slik begrunnelse. */
    data object MåHaBegrunnelseForBarnetilleggForÅJournalføreNotat : KanIkkeSendeRammebehandlingTilBeslutter
}
