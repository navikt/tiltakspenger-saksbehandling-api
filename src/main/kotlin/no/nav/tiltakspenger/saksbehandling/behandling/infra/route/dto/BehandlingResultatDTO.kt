package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto

import no.nav.tiltakspenger.saksbehandling.behandling.domene.BehandlingResultatType
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Revurdering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.RevurderingResultat
import no.nav.tiltakspenger.saksbehandling.behandling.domene.RevurderingType
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Søknadsbehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.SøknadsbehandlingResultat
import no.nav.tiltakspenger.saksbehandling.behandling.domene.SøknadsbehandlingType

enum class BehandlingResultatDTO {
    INNVILGELSE,
    AVSLAG,
    STANS,
    REVURDERING_INNVILGELSE,
    IKKE_VALGT,
    ;

    fun toDomain(): BehandlingResultatType? = when (this) {
        INNVILGELSE -> SøknadsbehandlingType.INNVILGELSE
        AVSLAG -> SøknadsbehandlingType.AVSLAG
        STANS -> RevurderingType.STANS
        REVURDERING_INNVILGELSE -> RevurderingType.INNVILGELSE
        IKKE_VALGT -> null
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

fun Rammebehandling.tilBehandlingResultatDTO(): BehandlingResultatDTO? = when (this) {
    is Revurdering -> resultat.tilBehandlingResultatDTO()
    is Søknadsbehandling -> resultat?.tilBehandlingResultatDTO()
}
