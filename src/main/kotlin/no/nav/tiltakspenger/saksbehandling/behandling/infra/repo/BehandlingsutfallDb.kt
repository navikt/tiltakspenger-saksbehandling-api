package no.nav.tiltakspenger.saksbehandling.behandling.infra.repo

import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.Omgjøringsresultat
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.Omgjøringsresultat.OmgjøringInnvilgelse
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.Rammebehandlingsresultat
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.RevurderingType
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.Revurderingsresultat
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.SøknadsbehandlingType
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.Søknadsbehandlingsresultat

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
    is OmgjøringInnvilgelse -> RevurderingUtfallDb.OMGJØRING
    is Søknadsbehandlingsresultat.Avslag -> SøknadsbehandlingUtfallDb.AVSLAG
    is Søknadsbehandlingsresultat.Innvilgelse -> SøknadsbehandlingUtfallDb.INNVILGELSE
    is Omgjøringsresultat.OmgjøringIkkeValgt -> TODO()
    is Omgjøringsresultat.OmgjøringOpphør -> TODO()
}.toString()
