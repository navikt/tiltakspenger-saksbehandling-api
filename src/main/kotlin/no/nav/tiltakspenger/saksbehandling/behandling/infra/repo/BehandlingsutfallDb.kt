package no.nav.tiltakspenger.saksbehandling.behandling.infra.repo

import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandlingsutfall

internal enum class BehandlingsutfallDb(val value: String) {
    INNVILGELSE("INNVILGELSE"),
    AVSLAG("AVSLAG"),
}

internal fun Behandlingsutfall.toDb(): String = when (this) {
    Behandlingsutfall.INNVILGELSE -> BehandlingsutfallDb.INNVILGELSE.value
    Behandlingsutfall.AVSLAG -> BehandlingsutfallDb.AVSLAG.value
}

internal fun BehandlingsutfallDb.toDomain(): Behandlingsutfall = when (this) {
    BehandlingsutfallDb.INNVILGELSE -> Behandlingsutfall.INNVILGELSE
    BehandlingsutfallDb.AVSLAG -> Behandlingsutfall.AVSLAG
}

internal fun String.toBehandlingsutfallDb(): BehandlingsutfallDb = BehandlingsutfallDb.valueOf(this)
