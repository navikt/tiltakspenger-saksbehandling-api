package no.nav.tiltakspenger.saksbehandling.behandling.infra.repo

import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.Omgjøringsresultat
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.Omgjøringsresultat.OmgjøringInnvilgelse
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.Rammebehandlingsresultat
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.Revurderingsresultat
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.RevurderingsresultatType
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.Søknadsbehandlingsresultat
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.SøknadsbehandlingsresultatType

private enum class SøknadsbehandlingsresultatDb {
    INNVILGELSE,
    AVSLAG,
}

private enum class RevurderingsresultatDb {
    STANS,
    REVURDERING_INNVILGELSE,
    OMGJØRING,
    OMGJØRING_OPPHØR,
    OMGJØRING_IKKE_VALGT,
}

fun String.tilSøknadsbehandlingResultatType(): SøknadsbehandlingsresultatType =
    when (SøknadsbehandlingsresultatDb.valueOf(this)) {
        SøknadsbehandlingsresultatDb.INNVILGELSE -> SøknadsbehandlingsresultatType.INNVILGELSE
        SøknadsbehandlingsresultatDb.AVSLAG -> SøknadsbehandlingsresultatType.AVSLAG
    }

fun String.tilRevurderingResultatType(): RevurderingsresultatType = when (RevurderingsresultatDb.valueOf(this)) {
    RevurderingsresultatDb.STANS -> RevurderingsresultatType.STANS
    RevurderingsresultatDb.REVURDERING_INNVILGELSE -> RevurderingsresultatType.INNVILGELSE
    RevurderingsresultatDb.OMGJØRING -> RevurderingsresultatType.OMGJØRING_INNVILGELSE
    RevurderingsresultatDb.OMGJØRING_OPPHØR -> RevurderingsresultatType.OMGJØRING_OPPHØR
    RevurderingsresultatDb.OMGJØRING_IKKE_VALGT -> RevurderingsresultatType.OMGJØRING_IKKE_VALGT
}

fun Rammebehandlingsresultat.toDb(): String = when (this) {
    is Revurderingsresultat.Stans -> RevurderingsresultatDb.STANS
    is Revurderingsresultat.Innvilgelse -> RevurderingsresultatDb.REVURDERING_INNVILGELSE
    is OmgjøringInnvilgelse -> RevurderingsresultatDb.OMGJØRING
    is Søknadsbehandlingsresultat.Avslag -> SøknadsbehandlingsresultatDb.AVSLAG
    is Søknadsbehandlingsresultat.Innvilgelse -> SøknadsbehandlingsresultatDb.INNVILGELSE
    is Omgjøringsresultat.OmgjøringIkkeValgt -> RevurderingsresultatDb.OMGJØRING_IKKE_VALGT
    is Omgjøringsresultat.OmgjøringOpphør -> RevurderingsresultatDb.OMGJØRING_OPPHØR
}.toString()
