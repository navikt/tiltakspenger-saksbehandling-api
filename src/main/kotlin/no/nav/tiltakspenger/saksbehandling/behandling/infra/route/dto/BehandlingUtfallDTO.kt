package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto

import no.nav.tiltakspenger.saksbehandling.behandling.domene.BehandlingResultatType
import no.nav.tiltakspenger.saksbehandling.behandling.domene.RevurderingResultat
import no.nav.tiltakspenger.saksbehandling.behandling.domene.RevurderingType
import no.nav.tiltakspenger.saksbehandling.behandling.domene.SøknadsbehandlingResultat
import no.nav.tiltakspenger.saksbehandling.behandling.domene.SøknadsbehandlingType

enum class BehandlingUtfallDTO {
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

fun SøknadsbehandlingResultat.tilUtfallDTO(): BehandlingUtfallDTO = when (this) {
    is SøknadsbehandlingResultat.Avslag -> BehandlingUtfallDTO.AVSLAG
    is SøknadsbehandlingResultat.Innvilgelse -> BehandlingUtfallDTO.INNVILGELSE
}

fun RevurderingResultat.tilUtfallDTO(): BehandlingUtfallDTO = when (this) {
    is RevurderingResultat.Stans -> BehandlingUtfallDTO.STANS
    is RevurderingResultat.Innvilgelse -> BehandlingUtfallDTO.REVURDERING_INNVILGELSE
}
