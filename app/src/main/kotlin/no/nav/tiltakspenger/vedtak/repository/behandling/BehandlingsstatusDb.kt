package no.nav.tiltakspenger.vedtak.repository.behandling

import no.nav.tiltakspenger.vedtak.repository.behandling.BehandlingsstatusDb.INNVILGET
import no.nav.tiltakspenger.vedtak.repository.behandling.BehandlingsstatusDb.KLAR_TIL_BEHANDLING
import no.nav.tiltakspenger.vedtak.repository.behandling.BehandlingsstatusDb.KLAR_TIL_BESLUTNING
import no.nav.tiltakspenger.vedtak.repository.behandling.BehandlingsstatusDb.UNDER_BEHANDLING
import no.nav.tiltakspenger.vedtak.repository.behandling.BehandlingsstatusDb.UNDER_BESLUTNING
import no.nav.tiltakspenger.saksbehandling.domene.behandling.Behandlingsstatus as BehandlingsstatusDomain

/**
 * @see BehandlingsstatusDomain
 */
private enum class BehandlingsstatusDb {
    KLAR_TIL_BEHANDLING,
    UNDER_BEHANDLING,
    KLAR_TIL_BESLUTNING,
    UNDER_BESLUTNING,
    INNVILGET,
}

fun String.toBehandlingsstatus(): BehandlingsstatusDomain =
    when (BehandlingsstatusDb.valueOf(this)) {
        KLAR_TIL_BEHANDLING -> BehandlingsstatusDomain.KLAR_TIL_BEHANDLING
        UNDER_BEHANDLING -> BehandlingsstatusDomain.UNDER_BEHANDLING
        KLAR_TIL_BESLUTNING -> BehandlingsstatusDomain.KLAR_TIL_BESLUTNING
        UNDER_BESLUTNING -> BehandlingsstatusDomain.UNDER_BESLUTNING
        INNVILGET -> BehandlingsstatusDomain.INNVILGET
    }

fun BehandlingsstatusDomain.toDb(): String =
    when (this) {
        BehandlingsstatusDomain.KLAR_TIL_BEHANDLING -> KLAR_TIL_BEHANDLING
        BehandlingsstatusDomain.UNDER_BEHANDLING -> UNDER_BEHANDLING
        BehandlingsstatusDomain.KLAR_TIL_BESLUTNING -> KLAR_TIL_BESLUTNING
        BehandlingsstatusDomain.UNDER_BESLUTNING -> UNDER_BESLUTNING
        BehandlingsstatusDomain.INNVILGET -> INNVILGET
    }.toString()
