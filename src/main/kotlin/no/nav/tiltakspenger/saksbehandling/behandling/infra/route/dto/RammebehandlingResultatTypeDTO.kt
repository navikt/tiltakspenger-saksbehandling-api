package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto

import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.Omgjøringsresultat
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.RammebehandlingsesultatType
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.Rammebehandlingsresultat
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.Revurderingsresultat
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.RevurderingsresultatType
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.Søknadsbehandlingsresultat
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.SøknadsbehandlingsresultatType

enum class RammebehandlingResultatTypeDTO {
    INNVILGELSE,
    AVSLAG,
    IKKE_VALGT,
    STANS,
    REVURDERING_INNVILGELSE,

    // TODO abn: rename til OMGJØRING_INNVILGELSE her og i frontend
    OMGJØRING,
    OMGJØRING_OPPHØR,
    OMGJØRING_IKKE_VALGT,
    ;

    fun toDomain(): RammebehandlingsesultatType? = when (this) {
        INNVILGELSE -> SøknadsbehandlingsresultatType.INNVILGELSE
        AVSLAG -> SøknadsbehandlingsresultatType.AVSLAG
        STANS -> RevurderingsresultatType.STANS
        REVURDERING_INNVILGELSE -> RevurderingsresultatType.INNVILGELSE
        OMGJØRING -> RevurderingsresultatType.OMGJØRING_INNVILGELSE
        OMGJØRING_OPPHØR -> RevurderingsresultatType.OMGJØRING_OPPHØR
        OMGJØRING_IKKE_VALGT -> RevurderingsresultatType.OMGJØRING_IKKE_VALGT
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
    is Omgjøringsresultat.OmgjøringInnvilgelse -> RammebehandlingResultatTypeDTO.OMGJØRING
    is Omgjøringsresultat.OmgjøringOpphør -> RammebehandlingResultatTypeDTO.OMGJØRING_OPPHØR
    is Omgjøringsresultat.OmgjøringIkkeValgt -> RammebehandlingResultatTypeDTO.OMGJØRING_IKKE_VALGT
}

fun Rammebehandlingsresultat?.tilRammebehandlingResultatTypeDTO(): RammebehandlingResultatTypeDTO = when (this) {
    is Søknadsbehandlingsresultat.Innvilgelse,
    is Søknadsbehandlingsresultat.Avslag,
    -> this.tilSøknadsbehandlingResultatTypeDTO()

    is Revurderingsresultat.Innvilgelse,
    is Omgjøringsresultat.OmgjøringInnvilgelse,
    is Revurderingsresultat.Stans,
    is Omgjøringsresultat.OmgjøringIkkeValgt,
    is Omgjøringsresultat.OmgjøringOpphør,
    -> this.tilRevurderingResultatTypeDTO()

    null -> RammebehandlingResultatTypeDTO.IKKE_VALGT
}
