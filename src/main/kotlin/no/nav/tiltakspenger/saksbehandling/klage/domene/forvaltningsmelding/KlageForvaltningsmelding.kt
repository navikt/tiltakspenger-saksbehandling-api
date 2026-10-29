package no.nav.tiltakspenger.saksbehandling.klage.domene.forvaltningsmelding

import no.nav.tiltakspenger.saksbehandling.distribusjon.DistribusjonId
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId
import no.nav.tiltakspenger.saksbehandling.klage.domene.brev.Brevtekster
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * En forvaltningsmelding er et informasjonsbrev til bruker om forventet saksbehandlingstid for klagen.
 */
data class KlageForvaltningsmelding(
    val brevtekst: Brevtekster,
    val saksbehandler: String,
    val sendtTidspunkt: LocalDateTime,
    val brevdato: LocalDate?,
    val journalpostId: JournalpostId?,
    val journalføringstidspunkt: LocalDateTime?,
    val distribusjonId: DistribusjonId?,
    val distribusjonstidspunkt: LocalDateTime?,
) {
    val erJournalført: Boolean = journalpostId != null
    val erDistribuert: Boolean = distribusjonId != null

    fun oppdaterJournalpost(
        brevdato: LocalDate,
        journalpostId: JournalpostId,
        tidspunkt: LocalDateTime,
    ): KlageForvaltningsmelding {
        require(!erJournalført) { "Kan kun journalføre forvaltningsmeldingen én gang" }
        return copy(
            brevdato = brevdato,
            journalpostId = journalpostId,
            journalføringstidspunkt = tidspunkt,
        )
    }

    fun oppdaterDistribusjon(
        distribusjonId: DistribusjonId,
        tidspunkt: LocalDateTime,
    ): KlageForvaltningsmelding {
        require(erJournalført && !erDistribuert) {
            "Kan kun distribuere forvaltningsmeldingen én gang, og den må være journalført først"
        }
        return copy(
            distribusjonId = distribusjonId,
            distribusjonstidspunkt = tidspunkt,
        )
    }

    init {
        require(brevtekst.isNotEmpty()) { "Forvaltningsmeldingen må ha minst én tekstblokk" }
        require((journalpostId == null) == (journalføringstidspunkt == null) && (journalpostId == null) == (brevdato == null)) {
            "journalpostId, journalføringstidspunkt og brevdato må enten alle være satt eller alle være null"
        }
        require((distribusjonId == null) == (distribusjonstidspunkt == null)) {
            "distribusjonId og distribusjonstidspunkt må enten begge være satt eller begge være null"
        }
        if (distribusjonId != null) {
            require(journalpostId != null) { "Forvaltningsmeldingen må være journalført før den kan være distribuert" }
        }
    }

    companion object {
        fun ny(
            brevtekst: Brevtekster,
            saksbehandler: String,
            sendtTidspunkt: LocalDateTime,
        ): KlageForvaltningsmelding = KlageForvaltningsmelding(
            brevtekst = brevtekst,
            saksbehandler = saksbehandler,
            sendtTidspunkt = sendtTidspunkt,
            brevdato = null,
            journalpostId = null,
            journalføringstidspunkt = null,
            distribusjonId = null,
            distribusjonstidspunkt = null,
        )
    }
}
