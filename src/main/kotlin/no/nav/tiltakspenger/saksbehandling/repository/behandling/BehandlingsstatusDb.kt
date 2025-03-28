package no.nav.tiltakspenger.saksbehandling.repository.behandling

import no.nav.tiltakspenger.saksbehandling.repository.behandling.BehandlingsstatusDb.KLAR_TIL_BEHANDLING
import no.nav.tiltakspenger.saksbehandling.repository.behandling.BehandlingsstatusDb.KLAR_TIL_BESLUTNING
import no.nav.tiltakspenger.saksbehandling.repository.behandling.BehandlingsstatusDb.UNDER_BEHANDLING
import no.nav.tiltakspenger.saksbehandling.repository.behandling.BehandlingsstatusDb.UNDER_BESLUTNING
import no.nav.tiltakspenger.saksbehandling.repository.behandling.BehandlingsstatusDb.VEDTATT
import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.behandling.Behandlingsstatus as BehandlingsstatusDomain

/**
 * @see BehandlingsstatusDomain
 */
private enum class BehandlingsstatusDb {
    KLAR_TIL_BEHANDLING,
    UNDER_BEHANDLING,
    KLAR_TIL_BESLUTNING,
    UNDER_BESLUTNING,
    VEDTATT,
    AVBRUTT,
}

fun String.toBehandlingsstatus(): BehandlingsstatusDomain =
    when (BehandlingsstatusDb.valueOf(this)) {
        KLAR_TIL_BEHANDLING -> BehandlingsstatusDomain.KLAR_TIL_BEHANDLING
        UNDER_BEHANDLING -> BehandlingsstatusDomain.UNDER_BEHANDLING
        KLAR_TIL_BESLUTNING -> BehandlingsstatusDomain.KLAR_TIL_BESLUTNING
        UNDER_BESLUTNING -> BehandlingsstatusDomain.UNDER_BESLUTNING
        VEDTATT -> BehandlingsstatusDomain.VEDTATT
        BehandlingsstatusDb.AVBRUTT -> BehandlingsstatusDomain.AVBRUTT
    }

fun BehandlingsstatusDomain.toDb(): String =
    when (this) {
        BehandlingsstatusDomain.KLAR_TIL_BEHANDLING -> KLAR_TIL_BEHANDLING
        BehandlingsstatusDomain.UNDER_BEHANDLING -> UNDER_BEHANDLING
        BehandlingsstatusDomain.KLAR_TIL_BESLUTNING -> KLAR_TIL_BESLUTNING
        BehandlingsstatusDomain.UNDER_BESLUTNING -> UNDER_BESLUTNING
        BehandlingsstatusDomain.VEDTATT -> VEDTATT
        BehandlingsstatusDomain.AVBRUTT -> BehandlingsstatusDb.AVBRUTT
    }.toString()
