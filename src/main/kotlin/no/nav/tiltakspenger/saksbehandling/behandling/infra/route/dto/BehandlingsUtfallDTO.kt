package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto

import no.nav.tiltakspenger.saksbehandling.behandling.domene.BehandlingUtfallType
import no.nav.tiltakspenger.saksbehandling.behandling.domene.RevurderingUtfall
import no.nav.tiltakspenger.saksbehandling.behandling.domene.RevurderingUtfallType
import no.nav.tiltakspenger.saksbehandling.behandling.domene.SøknadsbehandlingUtfall
import no.nav.tiltakspenger.saksbehandling.behandling.domene.SøknadsbehandlingUtfallType

sealed interface BehandlingsUtfallDTO {
    fun toDomain(): BehandlingUtfallType
}

enum class SøknadsbehandlingUtfallDTO : BehandlingsUtfallDTO {
    INNVILGELSE,
    AVSLAG,
    ;

    override fun toDomain(): SøknadsbehandlingUtfallType = when (this) {
        INNVILGELSE -> SøknadsbehandlingUtfallType.INNVILGELSE
        AVSLAG -> SøknadsbehandlingUtfallType.AVSLAG
    }
}
enum class RevurderingUtfallDTO : BehandlingsUtfallDTO {
    STANS,
    ;

    override fun toDomain(): RevurderingUtfallType = when (this) {
        STANS -> RevurderingUtfallType.STANS
    }
}

fun SøknadsbehandlingUtfall.tilUtfallDTO(): SøknadsbehandlingUtfallDTO = when (this) {
    is SøknadsbehandlingUtfall.Avslag -> SøknadsbehandlingUtfallDTO.AVSLAG
    is SøknadsbehandlingUtfall.Innvilgelse -> SøknadsbehandlingUtfallDTO.INNVILGELSE
}

fun RevurderingUtfall.tilUtfallDTO(): RevurderingUtfallDTO = when (this) {
    is RevurderingUtfall.Stans -> RevurderingUtfallDTO.STANS
}
