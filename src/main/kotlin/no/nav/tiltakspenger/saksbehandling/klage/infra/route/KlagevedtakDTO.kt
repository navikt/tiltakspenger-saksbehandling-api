package no.nav.tiltakspenger.saksbehandling.klage.infra.route

import no.nav.tiltakspenger.saksbehandling.distribusjon.DistribusjonId
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagevedtak
import java.time.LocalDateTime

data class KlagevedtakDTO(
    val klagevedtakId: String,
    val klagebehandlingId: String,
    val sakId: String,
    val opprettet: String,
    val journalpostId: String?,
    val journalføringstidspunkt: String?,
    val distribusjonId: String?,
    val distribusjonstidspunkt: String?,
    val vedtaksdato: String?,
)

fun Klagevedtak.tilKlagevedtakDTO(): KlagevedtakDTO {
    return KlagevedtakDTO(
        klagevedtakId = id.toString(),
        klagebehandlingId = behandling.id.toString(),
        sakId = sakId.toString(),
        opprettet = opprettet.toString(),
        journalpostId = journalpostId?.toString(),
        journalføringstidspunkt = journalføringstidspunkt?.toString(),
        distribusjonId = distribusjonId?.toString(),
        distribusjonstidspunkt = distribusjonstidspunkt?.toString(),
        vedtaksdato = vedtaksdato?.toString(),
    )
}
