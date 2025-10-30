package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto

import no.nav.tiltakspenger.saksbehandling.behandling.domene.BehandlingResultat
import no.nav.tiltakspenger.saksbehandling.behandling.domene.BehandlingResultatType
import no.nav.tiltakspenger.saksbehandling.behandling.domene.RevurderingResultat
import no.nav.tiltakspenger.saksbehandling.behandling.domene.RevurderingType
import no.nav.tiltakspenger.saksbehandling.behandling.domene.SøknadsbehandlingResultat
import no.nav.tiltakspenger.saksbehandling.behandling.domene.SøknadsbehandlingType

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

fun SøknadsbehandlingResultat?.tilSøknadsbehandlingResultatTypeDTO(): RammebehandlingResultatTypeDTO = when (this) {
    is SøknadsbehandlingResultat.Avslag -> RammebehandlingResultatTypeDTO.AVSLAG
    is SøknadsbehandlingResultat.Innvilgelse -> RammebehandlingResultatTypeDTO.INNVILGELSE
    null -> RammebehandlingResultatTypeDTO.IKKE_VALGT
}

fun RevurderingResultat.tilRevurderingResultatTypeDTO(): RammebehandlingResultatTypeDTO = when (this) {
    is RevurderingResultat.Stans -> RammebehandlingResultatTypeDTO.STANS
    is RevurderingResultat.Innvilgelse -> RammebehandlingResultatTypeDTO.REVURDERING_INNVILGELSE
    is RevurderingResultat.Omgjøring -> RammebehandlingResultatTypeDTO.OMGJØRING
}

fun BehandlingResultat?.tilRammebehandlingResultatTypeDTO(): RammebehandlingResultatTypeDTO = when (this) {
    is SøknadsbehandlingResultat.Innvilgelse,
    is SøknadsbehandlingResultat.Avslag,
    -> this.tilSøknadsbehandlingResultatTypeDTO()
    is RevurderingResultat.Innvilgelse,
    is RevurderingResultat.Omgjøring,
    is RevurderingResultat.Stans,
    -> this.tilRevurderingResultatTypeDTO()
    null -> RammebehandlingResultatTypeDTO.IKKE_VALGT
}
