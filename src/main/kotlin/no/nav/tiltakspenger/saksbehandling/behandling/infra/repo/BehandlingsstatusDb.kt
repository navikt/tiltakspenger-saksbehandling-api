package no.nav.tiltakspenger.saksbehandling.behandling.infra.repo

import no.nav.tiltakspenger.saksbehandling.behandling.infra.repo.BehandlingsstatusDb.AVBRUTT
import no.nav.tiltakspenger.saksbehandling.behandling.infra.repo.BehandlingsstatusDb.KLAR_TIL_BEHANDLING
import no.nav.tiltakspenger.saksbehandling.behandling.infra.repo.BehandlingsstatusDb.KLAR_TIL_BESLUTNING
import no.nav.tiltakspenger.saksbehandling.behandling.infra.repo.BehandlingsstatusDb.UNDER_BEHANDLING
import no.nav.tiltakspenger.saksbehandling.behandling.infra.repo.BehandlingsstatusDb.UNDER_BESLUTNING
import no.nav.tiltakspenger.saksbehandling.behandling.infra.repo.BehandlingsstatusDb.VEDTATT
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandlingsstatus as BehandlingsstatusDomain

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
        AVBRUTT -> BehandlingsstatusDomain.AVBRUTT
    }

fun BehandlingsstatusDomain.toDb(): String =
    when (this) {
        BehandlingsstatusDomain.KLAR_TIL_BEHANDLING -> KLAR_TIL_BEHANDLING
        BehandlingsstatusDomain.UNDER_BEHANDLING -> UNDER_BEHANDLING
        BehandlingsstatusDomain.KLAR_TIL_BESLUTNING -> KLAR_TIL_BESLUTNING
        BehandlingsstatusDomain.UNDER_BESLUTNING -> UNDER_BESLUTNING
        BehandlingsstatusDomain.VEDTATT -> VEDTATT
        BehandlingsstatusDomain.AVBRUTT -> AVBRUTT
    }.toString()
