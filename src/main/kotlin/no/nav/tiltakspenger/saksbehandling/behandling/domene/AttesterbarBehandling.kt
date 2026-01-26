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
interface AttesterbarBehandling : Behandling {
    override val id: Ulid
    override val opprettet: LocalDateTime
    override val sakId: SakId
    override val saksnummer: Saksnummer
    override val fnr: Fnr
    val sendtTilBeslutning: LocalDateTime?
    override val saksbehandler: String?
    val beslutter: String?
    val attesteringer: Attesteringer
    override val iverksattTidspunkt: LocalDateTime?
    override val erAvsluttet: Boolean
    override val erAvbrutt: Boolean
    val fritekstTilVedtaksbrev: FritekstTilVedtaksbrev?
}
