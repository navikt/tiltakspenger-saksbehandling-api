package no.nav.tiltakspenger.saksbehandling.behandling.infra.repo

import no.nav.tiltakspenger.saksbehandling.behandling.domene.BehandlingUtfall
import no.nav.tiltakspenger.saksbehandling.behandling.domene.BehandlingsutfallGammel
import no.nav.tiltakspenger.saksbehandling.behandling.domene.RevurderingUtfall
import no.nav.tiltakspenger.saksbehandling.behandling.domene.SøknadsbehandlingUtfall

enum class BehandlingsutfallDb {
    INNVILGELSE,
    AVSLAG,
    STANS,
}

fun BehandlingsutfallGammel.toDb(): String = when (this) {
    BehandlingsutfallGammel.INNVILGELSE -> BehandlingsutfallDb.INNVILGELSE
    BehandlingsutfallGammel.AVSLAG -> BehandlingsutfallDb.AVSLAG
    BehandlingsutfallGammel.STANS -> BehandlingsutfallDb.STANS
}.toString()

fun BehandlingsutfallDb.toDomain(): BehandlingsutfallGammel = when (this) {
    BehandlingsutfallDb.INNVILGELSE -> BehandlingsutfallGammel.INNVILGELSE
    BehandlingsutfallDb.AVSLAG -> BehandlingsutfallGammel.AVSLAG
    BehandlingsutfallDb.STANS -> BehandlingsutfallGammel.STANS
}

fun SøknadsbehandlingUtfall.toDb(): String = when (this) {
    is SøknadsbehandlingUtfall.Innvilgelse -> BehandlingsutfallDb.INNVILGELSE
    is SøknadsbehandlingUtfall.Avslag -> BehandlingsutfallDb.AVSLAG
}.toString()

fun RevurderingUtfall.toDb(): String = when (this) {
    is RevurderingUtfall.Stans -> BehandlingsutfallDb.STANS
}.toString()

fun BehandlingUtfall.toDb(): String = when (this) {
    is RevurderingUtfall.Stans -> BehandlingsutfallDb.STANS
    is SøknadsbehandlingUtfall.Avslag -> BehandlingsutfallDb.AVSLAG
    is SøknadsbehandlingUtfall.Innvilgelse -> BehandlingsutfallDb.INNVILGELSE
}.toString()
