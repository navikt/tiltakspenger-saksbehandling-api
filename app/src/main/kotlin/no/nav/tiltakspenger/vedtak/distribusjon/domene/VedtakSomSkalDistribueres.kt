package no.nav.tiltakspenger.vedtak.distribusjon.domene

import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.vedtak.felles.journalføring.JournalpostId

data class VedtakSomSkalDistribueres(
    val id: VedtakId,
    val journalpostId: JournalpostId,
)
