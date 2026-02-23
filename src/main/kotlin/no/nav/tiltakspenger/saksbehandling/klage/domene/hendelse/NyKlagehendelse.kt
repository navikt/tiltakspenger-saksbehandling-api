package no.nav.tiltakspenger.saksbehandling.klage.domene.hendelse

import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.saksbehandling.klage.domene.KlagebehandlingId
import java.time.LocalDateTime

/**
 * @param eksternKlagehendelseId Brukes som dedup
 * @param key Kabal sin interne behandlingId (vil også finnes i value). Vi lagrer den for debug-formål.
 */
data class NyKlagehendelse(
    val klagehendelseId: KlagehendelseId = KlagehendelseId.random(),
    val opprettet: LocalDateTime,
    val sistEndret: LocalDateTime,
    val eksternKlagehendelseId: String,
    val key: String,
    val value: String,
    val sakId: SakId?,
    val klagebehandlingId: KlagebehandlingId?,
) {
    fun leggTilSakidOgKlagebehandlingId(sakId: SakId, klagebehandlingId: KlagebehandlingId, sistEndret: LocalDateTime): NyKlagehendelse {
        return this.copy(
            sakId = sakId,
            klagebehandlingId = klagebehandlingId,
            sistEndret = sistEndret,
        )
    }
}
