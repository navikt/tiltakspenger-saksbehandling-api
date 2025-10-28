package no.nav.tiltakspenger.saksbehandling.journalpost

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId
import no.nav.tiltakspenger.saksbehandling.journalpost.infra.SafJournalpostClient
import no.nav.tiltakspenger.saksbehandling.journalpost.infra.route.ValiderJournalpostResponse

class ValiderJournalpostService(
    private val safJournalpostClient: SafJournalpostClient,
) {
    val logger = KotlinLogging.logger { }

    suspend fun hentOgValiderJournalpost(
        fnr: Fnr,
        journalpostId: JournalpostId,
    ): ValiderJournalpostResponse {
        val journalpost = safJournalpostClient.hentJournalpost(journalpostId)
        if (journalpost == null) {
            logger.warn { "Fant ikke journalpost med id $journalpostId" }
            return ValiderJournalpostResponse(
                journalpostFinnes = false,
                gjelderInnsendtFnr = null,
            )
        } else {
            val gjelderInnsendtFnr = if (journalpost.bruker?.id == null) {
                null
            } else if (fnr.verdi != journalpost.bruker.id) {
                logger.warn { "Journalpost med id $journalpostId tilhører en annen bruker enn innsendt fnr" }
                false
            } else {
                true
            }
            return ValiderJournalpostResponse(
                journalpostFinnes = true,
                gjelderInnsendtFnr = gjelderInnsendtFnr,
            )
        }
    }
}
