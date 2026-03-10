package no.nav.tiltakspenger.saksbehandling.journalføring.infra.http

import arrow.core.NonEmptyList
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalførBrevMetadata
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId
import no.nav.tiltakspenger.saksbehandling.journalpost.DokumentInfoId

data class JournalførteDokumenter(
    val journalpostId: JournalpostId,
    val dokumentInfoIder: NonEmptyList<DokumentInfoId>?,
    val metadata: JournalførBrevMetadata,
)
