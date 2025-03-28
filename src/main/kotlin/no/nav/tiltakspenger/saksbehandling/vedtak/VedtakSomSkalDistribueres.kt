package no.nav.tiltakspenger.saksbehandling.vedtak

import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId

data class VedtakSomSkalDistribueres(
    val id: VedtakId,
    val journalpostId: JournalpostId,
)
