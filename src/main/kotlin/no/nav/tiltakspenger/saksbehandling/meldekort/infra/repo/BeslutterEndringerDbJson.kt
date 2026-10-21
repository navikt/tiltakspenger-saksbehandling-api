package no.nav.tiltakspenger.saksbehandling.meldekort.infra.repo

import no.nav.tiltakspenger.libs.json.deserialize
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.saksbehandling.felles.MeldekortbehandlingBeslutterEndringer
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.journalføreNotat.MeldekortbehandlingSkalJournalføreNotatEndring
import java.time.LocalDateTime

/**
 * Lagres i `beslutter_endringer`-kolonnen på `meldekortbehandling`.
 * Eksempel: `{"skalJournalføreNotat": {"tidspunkt": "…", "saksbehandler": true, "beslutter": false}}`.
 */
private data class BeslutterEndringerDbJson(
    val skalJournalføreNotat: SkalJournalføreNotatEndringDbJson? = null,
)

private data class SkalJournalføreNotatEndringDbJson(
    val tidspunkt: String,
    val saksbehandler: Boolean,
    val beslutter: Boolean,
)

fun MeldekortbehandlingBeslutterEndringer.toDbJson(): String = serialize(
    BeslutterEndringerDbJson(
        skalJournalføreNotat = skalJournalføreNotat?.let {
            SkalJournalføreNotatEndringDbJson(
                tidspunkt = it.tidspunkt.toString(),
                saksbehandler = it.saksbehandler,
                beslutter = it.beslutter,
            )
        },
    ),
)

fun String.toMeldekortbehandlingBeslutterEndringer(): MeldekortbehandlingBeslutterEndringer {
    val json = deserialize<BeslutterEndringerDbJson>(this)
    return MeldekortbehandlingBeslutterEndringer(
        skalJournalføreNotat = json.skalJournalføreNotat?.let {
            MeldekortbehandlingSkalJournalføreNotatEndring(
                tidspunkt = LocalDateTime.parse(it.tidspunkt),
                saksbehandler = it.saksbehandler,
                beslutter = it.beslutter,
            )
        },
    )
}
