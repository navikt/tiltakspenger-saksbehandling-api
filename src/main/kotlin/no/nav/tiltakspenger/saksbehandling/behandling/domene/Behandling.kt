package no.nav.tiltakspenger.saksbehandling.behandling.domene

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Ulid
import no.nav.tiltakspenger.saksbehandling.felles.Attesteringer
import no.nav.tiltakspenger.saksbehandling.sak.Saksnummer
import java.time.LocalDateTime

/**
 * Supertype for [no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandling] og [Rammebehandling].
 * Dersom det er ønskelig at den er sealed, må de ligge i samme pakke.
 */
interface Behandling {
    val id: Ulid
    val opprettet: LocalDateTime
    val sakId: SakId
    val saksnummer: Saksnummer
    val fnr: Fnr
    val sendtTilBeslutning: LocalDateTime?
    val saksbehandler: String?
    val beslutter: String?
    val attesteringer: Attesteringer
    val iverksattTidspunkt: LocalDateTime?
    val erAvsluttet: Boolean
    val erAvbrutt: Boolean
}
