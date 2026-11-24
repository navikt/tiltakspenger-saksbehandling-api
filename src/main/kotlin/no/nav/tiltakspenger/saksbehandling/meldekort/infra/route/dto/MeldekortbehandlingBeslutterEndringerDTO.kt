package no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.dto

import no.nav.tiltakspenger.saksbehandling.felles.MeldekortbehandlingBeslutterEndringer
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.distribuereVedtaksbrev.MeldekortbehandlingSkalDistribuereVedtaksbrevEndring
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.journalføreNotat.MeldekortbehandlingSkalJournalføreNotatEndring
import java.time.LocalDateTime

data class MeldekortbehandlingBeslutterEndringerDTO(
    val skalJournalføreNotat: MeldekortbehandlingSkalJournalføreNotatEndringDTO?,
    val skalDistribuereVedtaksbrev: MeldekortbehandlingSkalDistribuereVedtaksbrevEndringDTO?,
)

data class MeldekortbehandlingSkalDistribuereVedtaksbrevEndringDTO(
    val tidspunkt: LocalDateTime,
    val saksbehandler: Boolean,
    val beslutter: Boolean,
)

data class MeldekortbehandlingSkalJournalføreNotatEndringDTO(
    val tidspunkt: LocalDateTime,
    val saksbehandler: Boolean,
    val beslutter: Boolean,
)

fun MeldekortbehandlingBeslutterEndringer.tilDTO(): MeldekortbehandlingBeslutterEndringerDTO = MeldekortbehandlingBeslutterEndringerDTO(
    skalJournalføreNotat = skalJournalføreNotat?.tilDTO(),
    skalDistribuereVedtaksbrev = skalDistribuereVedtaksbrev?.tilDTO(),
)

private fun MeldekortbehandlingSkalDistribuereVedtaksbrevEndring.tilDTO(): MeldekortbehandlingSkalDistribuereVedtaksbrevEndringDTO =
    MeldekortbehandlingSkalDistribuereVedtaksbrevEndringDTO(
        tidspunkt = tidspunkt,
        saksbehandler = saksbehandler,
        beslutter = beslutter,
    )

private fun MeldekortbehandlingSkalJournalføreNotatEndring.tilDTO(): MeldekortbehandlingSkalJournalføreNotatEndringDTO =
    MeldekortbehandlingSkalJournalføreNotatEndringDTO(
        tidspunkt = tidspunkt,
        saksbehandler = saksbehandler,
        beslutter = beslutter,
    )
