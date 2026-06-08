package no.nav.tiltakspenger.saksbehandling.behandling.domene

import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksnummer
import java.time.LocalDateTime

interface Behandling {
    val id: BehandlingId
    val opprettet: LocalDateTime
    val sakId: SakId
    val saksnummer: Saksnummer
    val fnr: Fnr
    val saksbehandler: String?
    val iverksattTidspunkt: LocalDateTime?
    val erAvsluttet: Boolean
    val erAvbrutt: Boolean
}
