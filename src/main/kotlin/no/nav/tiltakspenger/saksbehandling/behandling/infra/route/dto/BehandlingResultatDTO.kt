package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto

import no.nav.tiltakspenger.saksbehandling.behandling.domene.BehandlingResultatType
import no.nav.tiltakspenger.saksbehandling.behandling.domene.RevurderingResultat
import no.nav.tiltakspenger.saksbehandling.behandling.domene.RevurderingType
import no.nav.tiltakspenger.saksbehandling.behandling.domene.SøknadsbehandlingResultat
import no.nav.tiltakspenger.saksbehandling.behandling.domene.SøknadsbehandlingType

sealed interface BehandlingResultatDTO {

    fun toDomain(): BehandlingResultatType = when (this) {
        RevurderingResultatDTO.STANS -> RevurderingType.STANS
        RevurderingResultatDTO.INNVILGELSE -> RevurderingType.INNVILGELSE
        SøknadsbehandlingResultatDTO.INNVILGELSE -> SøknadsbehandlingType.INNVILGELSE
        SøknadsbehandlingResultatDTO.AVSLAG -> SøknadsbehandlingType.AVSLAG
    }
}

enum class SøknadsbehandlingResultatDTO : BehandlingResultatDTO {
    INNVILGELSE,
    AVSLAG,
}

enum class RevurderingResultatDTO : BehandlingResultatDTO {
    INNVILGELSE,
    STANS,
}

fun SøknadsbehandlingResultat.tilUtfallDTO(): SøknadsbehandlingResultatDTO = when (this) {
    is SøknadsbehandlingResultat.Avslag -> SøknadsbehandlingResultatDTO.AVSLAG
    is SøknadsbehandlingResultat.Innvilgelse -> SøknadsbehandlingResultatDTO.INNVILGELSE
}

fun RevurderingResultat.tilUtfallDTO(): RevurderingResultatDTO = when (this) {
    is RevurderingResultat.Stans -> RevurderingResultatDTO.STANS
    is RevurderingResultat.Innvilgelse -> RevurderingResultatDTO.INNVILGELSE
}
