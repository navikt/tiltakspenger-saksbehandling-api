package no.nav.tiltakspenger.saksbehandling.klage.service

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.saksbehandling.dokument.PdfA
import no.nav.tiltakspenger.saksbehandling.felles.ServiceCommand
import no.nav.tiltakspenger.saksbehandling.journalpost.DokumentInfoId
import no.nav.tiltakspenger.saksbehandling.journalpost.HentDokumentCommand
import no.nav.tiltakspenger.saksbehandling.journalpost.HentJournalpostDokumentService
import no.nav.tiltakspenger.saksbehandling.klage.domene.KlagebehandlingId
import no.nav.tiltakspenger.saksbehandling.klage.ports.KlagebehandlingRepo
import java.time.Clock

class VisInnstillingsbrevKlagebehandlingService(
    private val hentJournalpostDokumentService: HentJournalpostDokumentService,
    private val klagebehandlingRepo: KlagebehandlingRepo,
    private val clock: Clock,
) {
    suspend fun hentDokument(
        command: VisInnstillingsbrevKlagebehandlingCommand,
    ): Either<KunneIkkeViseInnstillingsbrev, PdfA> {
        val klagebehandling = klagebehandlingRepo.hentForKlagebehandlingId(command.klagebehandlingId)

        val journalpostIdInnstillingsbrev = klagebehandling?.journalpostIdInnstillingsbrev
            ?: return KunneIkkeViseInnstillingsbrev.KlagenErIkkeJournalført.left()

        return hentJournalpostDokumentService.hent(
            command = HentDokumentCommand(
                sakId = klagebehandling.sakId,
                journalpostId = journalpostIdInnstillingsbrev,
                dokumentInfoId = command.dokumentInfoId,
                saksbehandler = command.saksbehandler,
                correlationId = command.correlationId,
            ),
        ).right()
    }
}

sealed interface KunneIkkeViseInnstillingsbrev {
    data object KlagenErIkkeJournalført : KunneIkkeViseInnstillingsbrev
}

data class VisInnstillingsbrevKlagebehandlingCommand(
    val sakId: SakId,
    val klagebehandlingId: KlagebehandlingId,
    val dokumentInfoId: DokumentInfoId,
    override val saksbehandler: Saksbehandler,
    override val correlationId: CorrelationId,
) : ServiceCommand
