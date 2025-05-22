package no.nav.tiltakspenger.saksbehandling.behandling.infra.repo

import no.nav.tiltakspenger.saksbehandling.behandling.domene.BehandlingUtfall
import no.nav.tiltakspenger.saksbehandling.behandling.domene.RevurderingUtfall
import no.nav.tiltakspenger.saksbehandling.behandling.domene.RevurderingUtfallType
import no.nav.tiltakspenger.saksbehandling.behandling.domene.SøknadsbehandlingUtfall
import no.nav.tiltakspenger.saksbehandling.behandling.domene.SøknadsbehandlingUtfallType

private enum class SøknadsbehandlingUtfallDb {
    INNVILGELSE,
    AVSLAG,
}

private enum class RevurderingUtfallDb {
    STANS,
}

fun String.tilSøknadsbehandlingUtfallType(): SøknadsbehandlingUtfallType = when (SøknadsbehandlingUtfallDb.valueOf(this)) {
    SøknadsbehandlingUtfallDb.INNVILGELSE -> SøknadsbehandlingUtfallType.INNVILGELSE
    SøknadsbehandlingUtfallDb.AVSLAG -> SøknadsbehandlingUtfallType.AVSLAG
}

fun String.tilRevurderingUtfallType(): RevurderingUtfallType = when (RevurderingUtfallDb.valueOf(this)) {
    RevurderingUtfallDb.STANS -> RevurderingUtfallType.STANS
}

fun SøknadsbehandlingUtfall.toDb(): String = when (this) {
    is SøknadsbehandlingUtfall.Innvilgelse -> SøknadsbehandlingUtfallDb.INNVILGELSE
    is SøknadsbehandlingUtfall.Avslag -> SøknadsbehandlingUtfallDb.AVSLAG
}.toString()

fun RevurderingUtfall.toDb(): String = when (this) {
    is RevurderingUtfall.Stans -> RevurderingUtfallDb.STANS
}.toString()

fun BehandlingUtfall.toDb(): String = when (this) {
    is RevurderingUtfall.Stans -> RevurderingUtfallDb.STANS
    is SøknadsbehandlingUtfall.Avslag -> SøknadsbehandlingUtfallDb.AVSLAG
    is SøknadsbehandlingUtfall.Innvilgelse -> SøknadsbehandlingUtfallDb.INNVILGELSE
}.toString()
