package no.nav.tiltakspenger.saksbehandling.journalpost

import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.saksbehandling.felles.ServiceCommand
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId

data class HentDokumentCommand(
    val sakId: SakId,
    val journalpostId: JournalpostId,
    val dokumentInfoId: DokumentInfoId,
    override val saksbehandler: Saksbehandler,
    override val correlationId: CorrelationId,
) : ServiceCommand
