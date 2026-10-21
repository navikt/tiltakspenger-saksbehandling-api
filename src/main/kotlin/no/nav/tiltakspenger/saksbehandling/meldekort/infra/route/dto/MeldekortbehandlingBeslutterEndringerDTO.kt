package no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.dto

import no.nav.tiltakspenger.saksbehandling.felles.MeldekortbehandlingBeslutterEndringer
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.journalføreNotat.MeldekortbehandlingSkalJournalføreNotatEndring
import java.time.LocalDateTime

data class MeldekortbehandlingBeslutterEndringerDTO(
    val skalJournalføreNotat: MeldekortbehandlingSkalJournalføreNotatEndringDTO?,
)

data class MeldekortbehandlingSkalJournalføreNotatEndringDTO(
    val tidspunkt: LocalDateTime,
    val saksbehandler: Boolean,
    val beslutter: Boolean,
)

fun MeldekortbehandlingBeslutterEndringer.tilDTO(): MeldekortbehandlingBeslutterEndringerDTO = MeldekortbehandlingBeslutterEndringerDTO(
    skalJournalføreNotat = skalJournalføreNotat?.tilDTO(),
)

private fun MeldekortbehandlingSkalJournalføreNotatEndring.tilDTO(): MeldekortbehandlingSkalJournalføreNotatEndringDTO =
    MeldekortbehandlingSkalJournalføreNotatEndringDTO(
        tidspunkt = tidspunkt,
        saksbehandler = saksbehandler,
        beslutter = beslutter,
    )
