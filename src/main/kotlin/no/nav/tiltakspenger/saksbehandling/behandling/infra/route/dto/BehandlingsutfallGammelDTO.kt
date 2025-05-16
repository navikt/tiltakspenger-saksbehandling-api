package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto

import no.nav.tiltakspenger.saksbehandling.behandling.domene.BehandlingsutfallGammel
import no.nav.tiltakspenger.saksbehandling.behandling.domene.RevurderingUtfall
import no.nav.tiltakspenger.saksbehandling.behandling.domene.SøknadsbehandlingUtfall

enum class BehandlingsutfallGammelDTO {
    INNVILGELSE,
    AVSLAG,
    STANS,
    ;

    fun toDomain(): BehandlingsutfallGammel = when (this) {
        INNVILGELSE -> BehandlingsutfallGammel.INNVILGELSE
        AVSLAG -> BehandlingsutfallGammel.AVSLAG
        STANS -> BehandlingsutfallGammel.STANS
    }
}

enum class SøknadsbehandlingUtfallDTO {
    INNVILGELSE,
    AVSLAG,
}

fun SøknadsbehandlingUtfall.toBehandlingsutfallDto(): BehandlingsutfallGammelDTO = when (this) {
    is SøknadsbehandlingUtfall.Avslag -> BehandlingsutfallGammelDTO.AVSLAG
    is SøknadsbehandlingUtfall.Innvilgelse -> BehandlingsutfallGammelDTO.INNVILGELSE
}

enum class RevurderingUtfallDTO {
    STANS,
}

fun RevurderingUtfall.toBehandlingsutfallDto(): BehandlingsutfallGammelDTO = when (this) {
    is RevurderingUtfall.Stans -> BehandlingsutfallGammelDTO.STANS
}
