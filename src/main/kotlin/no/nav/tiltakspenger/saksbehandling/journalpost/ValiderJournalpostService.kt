package no.nav.tiltakspenger.saksbehandling.journalpost

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId
import no.nav.tiltakspenger.saksbehandling.journalpost.infra.Journalpost
import no.nav.tiltakspenger.saksbehandling.journalpost.infra.SafJournalpostClient
import no.nav.tiltakspenger.saksbehandling.journalpost.infra.route.ValiderJournalpostResponse
import java.time.LocalDateTime

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
                datoOpprettet = null,
            )
        } else {
            return ValiderJournalpostResponse(
                journalpostFinnes = true,
                gjelderInnsendtFnr = journalpost.gjelderFnr(fnr, journalpostId),
                datoOpprettet = journalpost.datoOpprettet?.let { LocalDateTime.parse(it) },
            )
        }
    }

    // OBS: Hvis innsending av papirsøknad er gjort av verge så vil avsenderMottaker.id være vergens fnr. Ideelt sett
    // burde vi ha sjekket mot bruker på journalposten, men siden det er brukers aktørid som står som brukerid får
    // vi ikke sammenlignet uten oppslag i PDL. Derfor bør eventuelle avvik i bruker på journalposten kun vises som
    // en advarsel og ikke hindre videre behandling slik implementasjonen er nå.
    private fun Journalpost.gjelderFnr(fnr: Fnr, journalpostId: JournalpostId): Boolean? =
        if (avsenderMottaker?.id != null && avsenderMottaker.type == "FNR") {
            if (fnr.verdi != avsenderMottaker.id) {
                logger.warn { "Journalpost med id $journalpostId tilhører en annen bruker enn innsendt fnr" }
                false
            } else {
                true
            }
        } else {
            null
        }
}
