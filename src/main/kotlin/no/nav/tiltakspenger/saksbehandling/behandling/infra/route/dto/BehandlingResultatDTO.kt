package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto

import no.nav.tiltakspenger.saksbehandling.behandling.domene.BehandlingResultatType
import no.nav.tiltakspenger.saksbehandling.behandling.domene.RevurderingResultat
import no.nav.tiltakspenger.saksbehandling.behandling.domene.RevurderingType
import no.nav.tiltakspenger.saksbehandling.behandling.domene.SøknadsbehandlingResultat
import no.nav.tiltakspenger.saksbehandling.behandling.domene.SøknadsbehandlingType

enum class BehandlingResultatDTO {
    INNVILGELSE,
    AVSLAG,
    STANS,
    REVURDERING_INNVILGELSE,
    ;

    fun toDomain(): BehandlingResultatType = when (this) {
        INNVILGELSE -> SøknadsbehandlingType.INNVILGELSE
        AVSLAG -> SøknadsbehandlingType.AVSLAG
        STANS -> RevurderingType.STANS
        REVURDERING_INNVILGELSE -> RevurderingType.INNVILGELSE
    }
}

fun SøknadsbehandlingResultat.tilBehandlingResultatDTO(): BehandlingResultatDTO = when (this) {
    is SøknadsbehandlingResultat.Avslag -> BehandlingResultatDTO.AVSLAG
    is SøknadsbehandlingResultat.Innvilgelse -> BehandlingResultatDTO.INNVILGELSE
}

fun RevurderingResultat.tilBehandlingResultatDTO(): BehandlingResultatDTO = when (this) {
    is RevurderingResultat.Stans -> BehandlingResultatDTO.STANS
    is RevurderingResultat.Innvilgelse -> BehandlingResultatDTO.REVURDERING_INNVILGELSE
}
