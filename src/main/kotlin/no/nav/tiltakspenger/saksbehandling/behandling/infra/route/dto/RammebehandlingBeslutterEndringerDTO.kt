package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto

import no.nav.tiltakspenger.saksbehandling.behandling.domene.journalføreNotat.RammebehandlingSkalJournalføreNotatEndring
import no.nav.tiltakspenger.saksbehandling.felles.RammebehandlingBeslutterEndringer
import java.time.LocalDateTime

data class RammebehandlingBeslutterEndringerDTO(
    val skalJournalføreNotat: RammebehandlingSkalJournalføreNotatEndringDTO?,
)

data class RammebehandlingSkalJournalføreNotatEndringDTO(
    val tidspunkt: LocalDateTime,
    val vilkårsvurdering: VilkårsvurderingSkalJournalføreNotatEndringDTO?,
    val barnetillegg: BarnetilleggSkalJournalføreNotatEndringDTO?,
)

data class VilkårsvurderingSkalJournalføreNotatEndringDTO(
    val saksbehandler: Boolean,
    val beslutter: Boolean,
)

data class BarnetilleggSkalJournalføreNotatEndringDTO(
    val saksbehandler: Boolean,
    val beslutter: Boolean,
)

fun RammebehandlingBeslutterEndringer.tilDTO(): RammebehandlingBeslutterEndringerDTO = RammebehandlingBeslutterEndringerDTO(
    skalJournalføreNotat = skalJournalføreNotat?.tilDTO(),
)

private fun RammebehandlingSkalJournalføreNotatEndring.tilDTO(): RammebehandlingSkalJournalføreNotatEndringDTO =
    RammebehandlingSkalJournalføreNotatEndringDTO(
        tidspunkt = tidspunkt,
        vilkårsvurdering = vilkårsvurdering?.let { VilkårsvurderingSkalJournalføreNotatEndringDTO(it.saksbehandler, it.beslutter) },
        barnetillegg = barnetillegg?.let { BarnetilleggSkalJournalføreNotatEndringDTO(it.saksbehandler, it.beslutter) },
    )
