package no.nav.tiltakspenger.saksbehandling.behandling.domene

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksnummer
import no.nav.tiltakspenger.libs.common.Ulid
import java.time.LocalDateTime

interface Behandling {
    val id: Ulid
    val opprettet: LocalDateTime
    val sakId: SakId
    val saksnummer: Saksnummer
    val fnr: Fnr
    val saksbehandler: String?
    val iverksattTidspunkt: LocalDateTime?
    val erAvsluttet: Boolean
    val erAvbrutt: Boolean
}
