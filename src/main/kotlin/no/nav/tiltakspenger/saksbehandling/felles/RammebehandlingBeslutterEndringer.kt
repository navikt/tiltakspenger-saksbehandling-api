package no.nav.tiltakspenger.saksbehandling.felles

import no.nav.tiltakspenger.saksbehandling.behandling.domene.SkalJournalføreNotat
import no.nav.tiltakspenger.saksbehandling.behandling.domene.journalføreNotat.BarnetilleggSkalJournalføreNotatEndring
import no.nav.tiltakspenger.saksbehandling.behandling.domene.journalføreNotat.RammebehandlingSkalJournalføreNotatEndring
import no.nav.tiltakspenger.saksbehandling.behandling.domene.journalføreNotat.VilkårsvurderingSkalJournalføreNotatEndring
import java.time.LocalDateTime

data class RammebehandlingBeslutterEndringer(
    val skalJournalføreNotat: RammebehandlingSkalJournalføreNotatEndring?,
) {
    fun oppdater(
        gjeldendeValg: SkalJournalføreNotat,
        besluttersValg: SkalJournalføreNotat,
        tidspunkt: LocalDateTime,
    ): RammebehandlingBeslutterEndringer {
        val saksbehandlersVilkårsvurdering = skalJournalføreNotat?.vilkårsvurdering?.saksbehandler ?: gjeldendeValg.vilkårsvurdering
        val vilkårsvurdering = if (besluttersValg.vilkårsvurdering == saksbehandlersVilkårsvurdering) {
            null
        } else {
            VilkårsvurderingSkalJournalføreNotatEndring(saksbehandlersVilkårsvurdering, besluttersValg.vilkårsvurdering)
        }

        val saksbehandlersBarnetillegg = skalJournalføreNotat?.barnetillegg?.saksbehandler ?: gjeldendeValg.barnetillegg
        val barnetillegg = if (besluttersValg.barnetillegg == saksbehandlersBarnetillegg) {
            null
        } else {
            BarnetilleggSkalJournalføreNotatEndring(saksbehandlersBarnetillegg, besluttersValg.barnetillegg)
        }

        return copy(
            skalJournalføreNotat = if (vilkårsvurdering == null && barnetillegg == null) {
                null
            } else {
                RammebehandlingSkalJournalføreNotatEndring(tidspunkt, vilkårsvurdering, barnetillegg)
            },
        )
    }
}
