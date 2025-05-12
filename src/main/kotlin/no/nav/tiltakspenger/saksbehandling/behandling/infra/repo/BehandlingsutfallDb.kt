package no.nav.tiltakspenger.saksbehandling.behandling.infra.repo

import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandlingsutfall

internal enum class BehandlingsutfallDb {
    INNVILGELSE,
    AVSLAG,
    STANS,
}

internal fun Behandlingsutfall.toDb(): String = when (this) {
    Behandlingsutfall.INNVILGELSE -> BehandlingsutfallDb.INNVILGELSE
    Behandlingsutfall.AVSLAG -> BehandlingsutfallDb.AVSLAG
    Behandlingsutfall.STANS -> BehandlingsutfallDb.STANS
}.toString()

internal fun BehandlingsutfallDb.toDomain(): Behandlingsutfall = when (this) {
    BehandlingsutfallDb.INNVILGELSE -> Behandlingsutfall.INNVILGELSE
    BehandlingsutfallDb.AVSLAG -> Behandlingsutfall.AVSLAG
    BehandlingsutfallDb.STANS -> Behandlingsutfall.STANS
}
