package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto

import no.nav.tiltakspenger.saksbehandling.behandling.domene.BehandlingResultatType
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsresultat
import no.nav.tiltakspenger.saksbehandling.behandling.domene.RevurderingType
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Revurderingsresultat
import no.nav.tiltakspenger.saksbehandling.behandling.domene.SøknadsbehandlingType
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Søknadsbehandlingsresultat

enum class RammebehandlingResultatTypeDTO {
    INNVILGELSE,
    AVSLAG,
    STANS,
    REVURDERING_INNVILGELSE,
    OMGJØRING,
    IKKE_VALGT,
    ;

    fun toDomain(): BehandlingResultatType? = when (this) {
        INNVILGELSE -> SøknadsbehandlingType.INNVILGELSE
        AVSLAG -> SøknadsbehandlingType.AVSLAG
        STANS -> RevurderingType.STANS
        REVURDERING_INNVILGELSE -> RevurderingType.INNVILGELSE
        OMGJØRING -> RevurderingType.OMGJØRING
        IKKE_VALGT -> null
    }
}

fun Søknadsbehandlingsresultat?.tilSøknadsbehandlingResultatTypeDTO(): RammebehandlingResultatTypeDTO = when (this) {
    is Søknadsbehandlingsresultat.Avslag -> RammebehandlingResultatTypeDTO.AVSLAG
    is Søknadsbehandlingsresultat.Innvilgelse -> RammebehandlingResultatTypeDTO.INNVILGELSE
    null -> RammebehandlingResultatTypeDTO.IKKE_VALGT
}

fun Revurderingsresultat.tilRevurderingResultatTypeDTO(): RammebehandlingResultatTypeDTO = when (this) {
    is Revurderingsresultat.Stans -> RammebehandlingResultatTypeDTO.STANS
    is Revurderingsresultat.Innvilgelse -> RammebehandlingResultatTypeDTO.REVURDERING_INNVILGELSE
    is Revurderingsresultat.Omgjøring -> RammebehandlingResultatTypeDTO.OMGJØRING
}

fun Rammebehandlingsresultat?.tilRammebehandlingResultatTypeDTO(): RammebehandlingResultatTypeDTO = when (this) {
    is Søknadsbehandlingsresultat.Innvilgelse,
    is Søknadsbehandlingsresultat.Avslag,
    -> this.tilSøknadsbehandlingResultatTypeDTO()
    is Revurderingsresultat.Innvilgelse,
    is Revurderingsresultat.Omgjøring,
    is Revurderingsresultat.Stans,
    -> this.tilRevurderingResultatTypeDTO()
    null -> RammebehandlingResultatTypeDTO.IKKE_VALGT
}
