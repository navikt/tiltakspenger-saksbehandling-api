package no.nav.tiltakspenger.saksbehandling.behandling.infra.repo

import no.nav.tiltakspenger.libs.json.deserialize
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.saksbehandling.behandling.domene.SkalJournalføreNotat

/** Lagres i `skal_journalfore_notat`-kolonnen på `behandling`. */
private data class SkalJournalføreNotatDbJson(
    val vilkårsvurdering: Boolean,
    val barnetillegg: Boolean,
)

fun SkalJournalføreNotat.toDbJson(): String = serialize(
    SkalJournalføreNotatDbJson(
        vilkårsvurdering = vilkårsvurdering,
        barnetillegg = barnetillegg,
    ),
)

fun String.toSkalJournalføreNotat(): SkalJournalføreNotat {
    val json = deserialize<SkalJournalføreNotatDbJson>(this)
    return SkalJournalføreNotat(
        vilkårsvurdering = json.vilkårsvurdering,
        barnetillegg = json.barnetillegg,
    )
}
