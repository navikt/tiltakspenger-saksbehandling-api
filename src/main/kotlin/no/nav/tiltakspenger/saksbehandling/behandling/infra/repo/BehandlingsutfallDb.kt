package no.nav.tiltakspenger.saksbehandling.behandling.infra.repo

import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsresultat
import no.nav.tiltakspenger.saksbehandling.behandling.domene.RevurderingType
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Revurderingsresultat
import no.nav.tiltakspenger.saksbehandling.behandling.domene.SøknadsbehandlingType
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Søknadsbehandlingsresultat

private enum class SøknadsbehandlingUtfallDb {
    INNVILGELSE,
    AVSLAG,
}

private enum class RevurderingUtfallDb {
    STANS,
    REVURDERING_INNVILGELSE,
    OMGJØRING,
}

fun String.tilSøknadsbehandlingResultatType(): SøknadsbehandlingType = when (SøknadsbehandlingUtfallDb.valueOf(this)) {
    SøknadsbehandlingUtfallDb.INNVILGELSE -> SøknadsbehandlingType.INNVILGELSE
    SøknadsbehandlingUtfallDb.AVSLAG -> SøknadsbehandlingType.AVSLAG
}

fun String.tilRevurderingResultatType(): RevurderingType = when (RevurderingUtfallDb.valueOf(this)) {
    RevurderingUtfallDb.STANS -> RevurderingType.STANS
    RevurderingUtfallDb.REVURDERING_INNVILGELSE -> RevurderingType.INNVILGELSE
    RevurderingUtfallDb.OMGJØRING -> RevurderingType.OMGJØRING
}

fun Rammebehandlingsresultat.toDb(): String = when (this) {
    is Revurderingsresultat.Stans -> RevurderingUtfallDb.STANS
    is Revurderingsresultat.Innvilgelse -> RevurderingUtfallDb.REVURDERING_INNVILGELSE
    is Revurderingsresultat.Omgjøring -> RevurderingUtfallDb.OMGJØRING
    is Søknadsbehandlingsresultat.Avslag -> SøknadsbehandlingUtfallDb.AVSLAG
    is Søknadsbehandlingsresultat.Innvilgelse -> SøknadsbehandlingUtfallDb.INNVILGELSE
}.toString()
