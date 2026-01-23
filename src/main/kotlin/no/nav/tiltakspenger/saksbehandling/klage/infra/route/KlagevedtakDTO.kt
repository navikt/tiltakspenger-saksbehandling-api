package no.nav.tiltakspenger.saksbehandling.klage.infra.route

import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagevedtak
import no.nav.tiltakspenger.saksbehandling.klage.infra.route.KlageresultatstypeDto.Companion.toKlageresultatstypDto

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
    val resultat: KlageresultatstypeDto,
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
        resultat = this.resultat.toKlageresultatstypDto(),
    )
}
