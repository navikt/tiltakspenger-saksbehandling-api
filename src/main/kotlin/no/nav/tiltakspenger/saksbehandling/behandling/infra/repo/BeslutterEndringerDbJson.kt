package no.nav.tiltakspenger.saksbehandling.behandling.infra.repo

import no.nav.tiltakspenger.libs.json.deserialize
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.saksbehandling.behandling.domene.journalføreNotat.BarnetilleggSkalJournalføreNotatEndring
import no.nav.tiltakspenger.saksbehandling.behandling.domene.journalføreNotat.RammebehandlingSkalJournalføreNotatEndring
import no.nav.tiltakspenger.saksbehandling.behandling.domene.journalføreNotat.VilkårsvurderingSkalJournalføreNotatEndring
import no.nav.tiltakspenger.saksbehandling.felles.RammebehandlingBeslutterEndringer
import java.time.LocalDateTime

/**
 * Lagres i `beslutter_endringer`-kolonnen på `behandling`.
 * Begrunnelser beslutter ikke har endret valget for, lagres som null.
 * Eksempel: `{"skalJournalføreNotat": {"tidspunkt": "…", "vilkårsvurdering": {"saksbehandler": true, "beslutter": false}, "barnetillegg": null}}`.
 */
private data class BeslutterEndringerDbJson(
    val skalJournalføreNotat: SkalJournalføreNotatEndringDbJson?,
)

private data class SkalJournalføreNotatEndringDbJson(
    val tidspunkt: String,
    val vilkårsvurdering: VilkårsvurderingEndringDbJson?,
    val barnetillegg: BarnetilleggEndringDbJson?,
)

private data class VilkårsvurderingEndringDbJson(
    val saksbehandler: Boolean,
    val beslutter: Boolean,
)

private data class BarnetilleggEndringDbJson(
    val saksbehandler: Boolean,
    val beslutter: Boolean,
)

fun RammebehandlingBeslutterEndringer.toDbJson(): String = serialize(
    BeslutterEndringerDbJson(
        skalJournalføreNotat = skalJournalføreNotat?.let {
            SkalJournalføreNotatEndringDbJson(
                tidspunkt = it.tidspunkt.toString(),
                vilkårsvurdering = it.vilkårsvurdering?.let { VilkårsvurderingEndringDbJson(it.saksbehandler, it.beslutter) },
                barnetillegg = it.barnetillegg?.let { BarnetilleggEndringDbJson(it.saksbehandler, it.beslutter) },
            )
        },
    ),
)

fun String.toRammebehandlingBeslutterEndringer(): RammebehandlingBeslutterEndringer {
    val json = deserialize<BeslutterEndringerDbJson>(this)
    return RammebehandlingBeslutterEndringer(
        skalJournalføreNotat = json.skalJournalføreNotat?.let {
            RammebehandlingSkalJournalføreNotatEndring(
                tidspunkt = LocalDateTime.parse(it.tidspunkt),
                vilkårsvurdering = it.vilkårsvurdering?.let { VilkårsvurderingSkalJournalføreNotatEndring(it.saksbehandler, it.beslutter) },
                barnetillegg = it.barnetillegg?.let { BarnetilleggSkalJournalføreNotatEndring(it.saksbehandler, it.beslutter) },
            )
        },
    )
}
