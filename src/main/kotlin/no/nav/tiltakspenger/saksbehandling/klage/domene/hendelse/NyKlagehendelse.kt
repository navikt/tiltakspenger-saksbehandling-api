package no.nav.tiltakspenger.saksbehandling.klage.domene.hendelse

import java.time.LocalDateTime

/**
 * @param eksternKlagehendelseId Brukes som dedup
 * @param key Kabal sin interne behandlingId (vil også finnes i value). Vi lagrer den for debug-formål.
 */
data class NyKlagehendelse(
    val klagehendelseId: KlagehendelseId = KlagehendelseId.random(),
    val opprettet: LocalDateTime,
    val eksternKlagehendelseId: String,
    val key: String,
    val value: String,
)
