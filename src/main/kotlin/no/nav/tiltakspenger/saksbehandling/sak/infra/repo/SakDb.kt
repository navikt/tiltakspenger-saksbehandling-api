package no.nav.tiltakspenger.saksbehandling.sak.infra.repo

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.saksbehandling.sak.Saksnummer
import java.time.LocalDateTime

data class SakDb(
    val id: SakId,
    val fnr: Fnr,
    val saksnummer: Saksnummer,
    val sistEndret: LocalDateTime,
    val opprettet: LocalDateTime,
)
