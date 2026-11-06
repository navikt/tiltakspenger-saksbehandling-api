package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.domene.tiltakshistorikk

import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus

/**
 * Tiltakshistorikken som er relevant for en rammebehandling.
 * [utløsendeInnslag] er innslaget som førte til at behandlingen ble opprettet automatisk, dersom det finnes.
 * [harEndringEtterSistEndret] er true når vi har innhentet tiltakshistorikk etter at behandlingen sist ble endret, mens behandlingen fortsatt er åpen.
 */
data class TiltakshistorikkForBehandling(
    val innslag: List<Tiltakshistorikkinnslag>,
    val utløsendeInnslag: Tiltakshistorikkinnslag?,
    val harEndringEtterSistEndret: Boolean,
)

private val statuserSomKanFåNyeEndringer = setOf(
    Rammebehandlingsstatus.KLAR_TIL_BEHANDLING,
    Rammebehandlingsstatus.UNDER_BEHANDLING,
    Rammebehandlingsstatus.KLAR_TIL_BESLUTNING,
    Rammebehandlingsstatus.UNDER_BESLUTNING,
)

/**
 * Plukker ut innslagene for tiltaksdeltakelsene i behandlingens saksopplysninger, innhentet fra behandlingen ble opprettet til den ble iverksatt eller avbrutt.
 * Innslaget som utløste en automatisk opprettet behandling tas alltid med.
 */
fun List<Tiltakshistorikkinnslag>.forBehandling(behandling: Rammebehandling): TiltakshistorikkForBehandling {
    val deltakelser = behandling.saksopplysninger.tiltaksdeltakelser.map { it.internDeltakelseId }.toSet()
    val avsluttet = behandling.iverksattTidspunkt ?: behandling.avbrutt?.tidspunkt
    val utløsende = this.find { it.behandlingId == behandling.id }
    val innslag = this.filter { innslag ->
        innslag == utløsende || (
            innslag.tiltaksdeltakerId in deltakelser &&
                !innslag.innhentet.isBefore(behandling.opprettet) &&
                (avsluttet == null || !innslag.innhentet.isAfter(avsluttet))
            )
    }.sortedBy { it.innhentet }
    // Det utløsende innslaget lagres etter at behandlingen er opprettet, og skal ikke regnes som en ny endring.
    val harEndringEtterSistEndret = behandling.status in statuserSomKanFåNyeEndringer &&
        innslag.any { it != utløsende && it.innhentet.isAfter(behandling.sistEndret) }
    return TiltakshistorikkForBehandling(
        innslag = innslag,
        utløsendeInnslag = utløsende,
        harEndringEtterSistEndret = harEndringEtterSistEndret,
    )
}
