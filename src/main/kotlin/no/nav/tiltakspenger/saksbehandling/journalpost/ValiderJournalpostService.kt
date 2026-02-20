package no.nav.tiltakspenger.saksbehandling.journalpost

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId
import no.nav.tiltakspenger.saksbehandling.journalpost.infra.Journalpost
import no.nav.tiltakspenger.saksbehandling.journalpost.infra.SafJournalpostClient
import no.nav.tiltakspenger.saksbehandling.journalpost.infra.route.ValiderJournalpostResponse
import no.nav.tiltakspenger.saksbehandling.person.Identtype
import no.nav.tiltakspenger.saksbehandling.person.PersonKlient
import java.time.LocalDateTime

class ValiderJournalpostService(
    private val safJournalpostClient: SafJournalpostClient,
    private val personKlient: PersonKlient,
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

    private suspend fun Journalpost.gjelderFnr(fnr: Fnr, journalpostId: JournalpostId): Boolean? {
        val fnrFraJournalpost = finnFnrFraJournalpost(journalpostId) ?: return null
        return if (fnr != fnrFraJournalpost) {
            logger.warn { "Journalpost med id $journalpostId tilhører en annen bruker enn innsendt fnr" }
            false
        } else {
            true
        }
    }

    private suspend fun Journalpost.finnFnrFraJournalpost(journalpostId: JournalpostId): Fnr? {
        return if (bruker == null || bruker.id == null) {
            if (avsenderMottaker?.id != null && avsenderMottaker.type == "FNR") {
                Fnr.tryFromString(avsenderMottaker.id)
            } else {
                null
            }
        } else {
            if (bruker.type == "FNR") {
                Fnr.tryFromString(bruker.id)
            } else if (bruker.type == "AKTOERID") {
                hentFnrFraPdl(bruker.id, journalpostId)
            } else {
                null
            }
        }
    }

    private suspend fun hentFnrFraPdl(aktorId: String, journalpostId: JournalpostId): Fnr? {
        val identerFraPdl = personKlient.hentIdenter(aktorId)
        val fnrFraPdl = identerFraPdl.filter { !it.historisk && it.identtype == Identtype.FOLKEREGISTERIDENT }
        if (fnrFraPdl.size != 1) {
            logger.warn { "Kunne ikke avgjøre fødselsnummer fra PDL for journalpostId $journalpostId" }
            return null
        } else {
            return Fnr.tryFromString(fnrFraPdl.first().ident)
        }
    }
}
