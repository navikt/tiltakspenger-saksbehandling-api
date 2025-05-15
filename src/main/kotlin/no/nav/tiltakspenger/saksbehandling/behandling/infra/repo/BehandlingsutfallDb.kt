package no.nav.tiltakspenger.saksbehandling.behandling.infra.repo

import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandlingsutfall
import no.nav.tiltakspenger.saksbehandling.behandling.domene.SøknadsbehandlingUtfall

enum class BehandlingsutfallDb {
    INNVILGELSE,
    AVSLAG,
    STANS,
}

fun Behandlingsutfall.toDb(): String = when (this) {
    Behandlingsutfall.INNVILGELSE -> BehandlingsutfallDb.INNVILGELSE
    Behandlingsutfall.AVSLAG -> BehandlingsutfallDb.AVSLAG
    Behandlingsutfall.STANS -> BehandlingsutfallDb.STANS
}.toString()

fun BehandlingsutfallDb.toDomain(): Behandlingsutfall = when (this) {
    BehandlingsutfallDb.INNVILGELSE -> Behandlingsutfall.INNVILGELSE
    BehandlingsutfallDb.AVSLAG -> Behandlingsutfall.AVSLAG
    BehandlingsutfallDb.STANS -> Behandlingsutfall.STANS
}

fun SøknadsbehandlingUtfall.toDb(): String = when (this) {
    is SøknadsbehandlingUtfall.Innvilgelse -> BehandlingsutfallDb.INNVILGELSE
    is SøknadsbehandlingUtfall.Avslag -> BehandlingsutfallDb.AVSLAG
}.toString()
