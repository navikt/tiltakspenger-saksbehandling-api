package no.nav.tiltakspenger.saksbehandling.person.identhendelser.repo

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.saksbehandling.person.identhendelser.Personident
import java.time.LocalDateTime
import java.util.UUID

data class IdenthendelseDb(
    val id: UUID,
    val gammeltFnr: Fnr,
    val nyttFnr: Fnr,
    val personidenter: List<Personident>,
    val sakId: SakId,
    val produsertHendelse: LocalDateTime?,
    val oppdatertDatabase: LocalDateTime?,
)
