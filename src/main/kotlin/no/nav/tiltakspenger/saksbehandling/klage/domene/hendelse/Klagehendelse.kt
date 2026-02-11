package no.nav.tiltakspenger.saksbehandling.klage.domene.hendelse

import java.time.LocalDateTime

data class Klagehendelse(
    val klagehendelseId: KlagehendelseId,
    val opprettet: LocalDateTime,
    val sistEndret: LocalDateTime,
    val eksternKlagehendelseId: String,
)
