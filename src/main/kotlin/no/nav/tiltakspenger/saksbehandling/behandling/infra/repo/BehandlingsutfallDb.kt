package no.nav.tiltakspenger.saksbehandling.behandling.infra.repo

import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandlingsutfall
// TODO raq - test
internal enum class BehandlingsutfallDb(val value: String) {
    INNVILGELSE("INNVILGELSE"),
    AVSLAG("AVSLAG"),
    STANS("STANS"),
}

// TODO raq - test
internal fun Behandlingsutfall.toDb(): String = when (this) {
    Behandlingsutfall.INNVILGELSE -> BehandlingsutfallDb.INNVILGELSE.value
    Behandlingsutfall.AVSLAG -> BehandlingsutfallDb.AVSLAG.value
    Behandlingsutfall.STANS -> BehandlingsutfallDb.STANS.value
}

// TODO raq - test
internal fun BehandlingsutfallDb.toDomain(): Behandlingsutfall = when (this) {
    BehandlingsutfallDb.INNVILGELSE -> Behandlingsutfall.INNVILGELSE
    BehandlingsutfallDb.AVSLAG -> Behandlingsutfall.AVSLAG
    BehandlingsutfallDb.STANS -> Behandlingsutfall.STANS
}

// TODO raq - test
internal fun String.toBehandlingsutfallDb(): BehandlingsutfallDb = BehandlingsutfallDb.valueOf(this)
