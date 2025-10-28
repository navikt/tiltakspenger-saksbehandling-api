package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto

import no.nav.tiltakspenger.saksbehandling.behandling.domene.BehandlingResultatType
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Revurdering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.RevurderingResultat
import no.nav.tiltakspenger.saksbehandling.behandling.domene.RevurderingType
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Søknadsbehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.SøknadsbehandlingResultat
import no.nav.tiltakspenger.saksbehandling.behandling.domene.SøknadsbehandlingType

enum class RammebehandlingResultatDTO {
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

fun SøknadsbehandlingResultat?.tilBehandlingResultatDTO(): RammebehandlingResultatDTO = when (this) {
    is SøknadsbehandlingResultat.Avslag -> RammebehandlingResultatDTO.AVSLAG
    is SøknadsbehandlingResultat.Innvilgelse -> RammebehandlingResultatDTO.INNVILGELSE
    null -> RammebehandlingResultatDTO.IKKE_VALGT
}

fun RevurderingResultat.tilBehandlingResultatDTO(): RammebehandlingResultatDTO = when (this) {
    is RevurderingResultat.Stans -> RammebehandlingResultatDTO.STANS
    is RevurderingResultat.Innvilgelse -> RammebehandlingResultatDTO.REVURDERING_INNVILGELSE
    is RevurderingResultat.Omgjøring -> RammebehandlingResultatDTO.OMGJØRING
}

fun Rammebehandling.tilBehandlingResultatDTO(): RammebehandlingResultatDTO? = when (this) {
    is Revurdering -> resultat.tilBehandlingResultatDTO()
    is Søknadsbehandling -> resultat?.tilBehandlingResultatDTO()
}
