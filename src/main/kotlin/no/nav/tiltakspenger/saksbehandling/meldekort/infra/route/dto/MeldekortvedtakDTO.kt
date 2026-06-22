package no.nav.tiltakspenger.saksbehandling.meldekort.infra.route.dto

import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortvedtak.Meldekortvedtak
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortvedtak.Meldekortvedtaksliste
import java.time.LocalDateTime

data class MeldekortvedtakDTO(
    val id: String,
    val sakId: String,
    val meldekortId: String,
    val opprettet: LocalDateTime,
    val journalpostId: String?,
)

private fun Meldekortvedtak.toDto(): MeldekortvedtakDTO = MeldekortvedtakDTO(
    id = id.toString(),
    sakId = sakId.toString(),
    meldekortId = meldekortId.toString(),
    opprettet = opprettet,
    journalpostId = journalpostId?.toString(),
)

fun Meldekortvedtaksliste.toDto(): List<MeldekortvedtakDTO> = map { it.toDto() }
