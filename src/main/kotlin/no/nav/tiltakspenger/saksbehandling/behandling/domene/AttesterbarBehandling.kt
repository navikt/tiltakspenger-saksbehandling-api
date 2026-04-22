package no.nav.tiltakspenger.saksbehandling.behandling.domene

import no.nav.tiltakspenger.libs.common.BehandlingId
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksnummer
import no.nav.tiltakspenger.saksbehandling.felles.Attesteringer
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandling
import java.time.LocalDateTime

/**
 * Supertype for [no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.Meldekortbehandling] og [Rammebehandling].
 * Dersom det er ønskelig at den er sealed, må de ligge i samme pakke.
 */
interface AttesterbarBehandling : Behandling {
    override val id: BehandlingId
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
    val klagebehandling: Klagebehandling?
}
