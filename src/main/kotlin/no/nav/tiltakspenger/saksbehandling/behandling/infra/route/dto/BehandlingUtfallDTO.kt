package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto

import no.nav.tiltakspenger.saksbehandling.behandling.domene.BehandlingUtfallType
import no.nav.tiltakspenger.saksbehandling.behandling.domene.RevurderingUtfall
import no.nav.tiltakspenger.saksbehandling.behandling.domene.RevurderingUtfallType
import no.nav.tiltakspenger.saksbehandling.behandling.domene.SøknadsbehandlingUtfall
import no.nav.tiltakspenger.saksbehandling.behandling.domene.SøknadsbehandlingUtfallType

enum class BehandlingUtfallDTO {
    INNVILGELSE,
    AVSLAG,
    STANS,
    REVURDERING_INNVILGELSE,
    ;

    fun toDomain(): BehandlingUtfallType = when (this) {
        INNVILGELSE -> SøknadsbehandlingUtfallType.INNVILGELSE
        AVSLAG -> SøknadsbehandlingUtfallType.AVSLAG
        STANS -> RevurderingUtfallType.STANS
        REVURDERING_INNVILGELSE -> RevurderingUtfallType.INNVILGELSE
    }
}

fun SøknadsbehandlingUtfall.tilUtfallDTO(): BehandlingUtfallDTO = when (this) {
    is SøknadsbehandlingUtfall.Avslag -> BehandlingUtfallDTO.AVSLAG
    is SøknadsbehandlingUtfall.Innvilgelse -> BehandlingUtfallDTO.INNVILGELSE
}

fun RevurderingUtfall.tilUtfallDTO(): BehandlingUtfallDTO = when (this) {
    is RevurderingUtfall.Stans -> BehandlingUtfallDTO.STANS
    is RevurderingUtfall.Innvilgelse -> BehandlingUtfallDTO.REVURDERING_INNVILGELSE
}
