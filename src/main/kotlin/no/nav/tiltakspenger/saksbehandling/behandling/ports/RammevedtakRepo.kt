package no.nav.tiltakspenger.saksbehandling.behandling.ports

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.libs.persistering.domene.TransactionContext
import no.nav.tiltakspenger.saksbehandling.behandling.domene.vedtak.Rammevedtak
import no.nav.tiltakspenger.saksbehandling.distribusjon.DistribusjonId
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId
import no.nav.tiltakspenger.saksbehandling.vedtak.VedtakSomSkalDistribueres
import java.time.LocalDate
import java.time.LocalDateTime

interface RammevedtakRepo {
    fun hentForVedtakId(vedtakId: VedtakId): Rammevedtak?

    fun lagre(
        vedtak: Rammevedtak,
        context: TransactionContext? = null,
    )

    fun hentForFnr(fnr: Fnr): List<Rammevedtak>

    fun hentRammevedtakSomSkalJournalføres(limit: Int = 10): List<Rammevedtak>

    fun hentRammevedtakSomSkalDistribueres(limit: Int = 10): List<VedtakSomSkalDistribueres>

    fun markerJournalført(id: VedtakId, vedtaksdato: LocalDate, brevJson: String, journalpostId: JournalpostId, tidspunkt: LocalDateTime)

    fun markerDistribuert(id: VedtakId, distribusjonId: DistribusjonId, tidspunkt: LocalDateTime)

    fun hentRammevedtakTilDatadeling(limit: Int = 10): List<Rammevedtak>

    fun markerSendtTilDatadeling(id: VedtakId, tidspunkt: LocalDateTime)
}
