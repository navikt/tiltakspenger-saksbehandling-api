package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto

import no.nav.tiltakspenger.saksbehandling.behandling.domene.SkalJournalføreNotat

data class SkalJournalføreNotatDTO(
    val vilkårsvurdering: Boolean,
    val barnetillegg: Boolean,
) {
    fun toDomain(): SkalJournalføreNotat = SkalJournalføreNotat(
        vilkårsvurdering = vilkårsvurdering,
        barnetillegg = barnetillegg,
    )

    companion object {
        val INGEN = SkalJournalføreNotatDTO(vilkårsvurdering = false, barnetillegg = false)
    }
}

fun SkalJournalføreNotat.tilDTO(): SkalJournalføreNotatDTO = SkalJournalføreNotatDTO(
    vilkårsvurdering = vilkårsvurdering,
    barnetillegg = barnetillegg,
)
