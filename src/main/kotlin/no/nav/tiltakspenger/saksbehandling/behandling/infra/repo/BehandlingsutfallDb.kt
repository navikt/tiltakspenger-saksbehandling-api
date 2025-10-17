package no.nav.tiltakspenger.saksbehandling.behandling.infra.repo

import no.nav.tiltakspenger.saksbehandling.behandling.domene.BehandlingResultat
import no.nav.tiltakspenger.saksbehandling.behandling.domene.RevurderingResultat
import no.nav.tiltakspenger.saksbehandling.behandling.domene.RevurderingType
import no.nav.tiltakspenger.saksbehandling.behandling.domene.SøknadsbehandlingResultat
import no.nav.tiltakspenger.saksbehandling.behandling.domene.SøknadsbehandlingType

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

fun BehandlingResultat.toDb(): String = when (this) {
    is RevurderingResultat.Stans -> RevurderingUtfallDb.STANS
    is RevurderingResultat.Innvilgelse -> RevurderingUtfallDb.REVURDERING_INNVILGELSE
    is RevurderingResultat.Omgjøring -> RevurderingUtfallDb.OMGJØRING
    is SøknadsbehandlingResultat.Avslag -> SøknadsbehandlingUtfallDb.AVSLAG
    is SøknadsbehandlingResultat.Innvilgelse -> SøknadsbehandlingUtfallDb.INNVILGELSE
}.toString()
