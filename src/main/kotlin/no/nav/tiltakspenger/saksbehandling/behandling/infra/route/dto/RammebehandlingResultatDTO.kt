package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto

import no.nav.tiltakspenger.saksbehandling.behandling.domene.RevurderingResultat
import no.nav.tiltakspenger.saksbehandling.behandling.domene.SøknadsbehandlingResultat
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.barnetillegg.BarnetilleggDTO
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.barnetillegg.toBarnetilleggDTO

sealed interface RammebehandlingResultatDTO {
    val resultat: RammebehandlingResultatTypeDTO
}

sealed interface SøknadsbehandlingResultatDTO : RammebehandlingResultatDTO {

    data class Innvilgelse(
        override val innvilgelsesperioder: InnvilgelsesperioderDTO,
        override val barnetillegg: BarnetilleggDTO?,
    ) : SøknadsbehandlingResultatDTO,
        RammebehandlingInnvilgelseResultatDTO {
        override val resultat = RammebehandlingResultatTypeDTO.INNVILGELSE
    }

    data class Avslag(
        val avslagsgrunner: List<ValgtHjemmelForAvslagDTO>,
    ) : SøknadsbehandlingResultatDTO {
        override val resultat = RammebehandlingResultatTypeDTO.AVSLAG
    }

    data object IkkeValgt : SøknadsbehandlingResultatDTO {
        override val resultat = RammebehandlingResultatTypeDTO.IKKE_VALGT
    }
}

sealed interface RevurderingResultatDTO : RammebehandlingResultatDTO {

    data class Innvilgelse(
        override val innvilgelsesperioder: InnvilgelsesperioderDTO?,
        override val barnetillegg: BarnetilleggDTO?,
    ) : RevurderingResultatDTO,
        RammebehandlingInnvilgelseResultatDTO {
        override val resultat = RammebehandlingResultatTypeDTO.REVURDERING_INNVILGELSE
    }

    data class Stans(
        val valgtHjemmelHarIkkeRettighet: List<ValgtHjemmelForStansDTO>,
        val harValgtStansFraFørsteDagSomGirRett: Boolean?,
    ) : RevurderingResultatDTO {
        override val resultat = RammebehandlingResultatTypeDTO.STANS
    }

    data class Omgjøring(
        override val innvilgelsesperioder: InnvilgelsesperioderDTO?,
        override val barnetillegg: BarnetilleggDTO?,
        val omgjørVedtak: String,
    ) : RevurderingResultatDTO,
        RammebehandlingInnvilgelseResultatDTO {
        override val resultat = RammebehandlingResultatTypeDTO.OMGJØRING
    }
}

sealed interface RammebehandlingInnvilgelseResultatDTO {
    val innvilgelsesperioder: InnvilgelsesperioderDTO?
    val barnetillegg: BarnetilleggDTO?
}

fun SøknadsbehandlingResultat?.tilSøknadsbehandlingResultatDTO(): SøknadsbehandlingResultatDTO {
    return when (this) {
        is SøknadsbehandlingResultat.Avslag -> SøknadsbehandlingResultatDTO.Avslag(
            avslagsgrunner = avslagsgrunner.toValgtHjemmelForAvslagDTO(),
        )

        is SøknadsbehandlingResultat.Innvilgelse -> SøknadsbehandlingResultatDTO.Innvilgelse(
            innvilgelsesperioder = innvilgelsesperioder.tilDTO(),
            barnetillegg = barnetillegg.toBarnetilleggDTO(),
        )

        null -> SøknadsbehandlingResultatDTO.IkkeValgt
    }
}

fun RevurderingResultat.tilRevurderingResultatDTO(): RevurderingResultatDTO {
    return when (this) {
        is RevurderingResultat.Innvilgelse -> RevurderingResultatDTO.Innvilgelse(
            innvilgelsesperioder = innvilgelsesperioder?.tilDTO(),
            barnetillegg = barnetillegg?.toBarnetilleggDTO(),
        )

        is RevurderingResultat.Stans -> RevurderingResultatDTO.Stans(
            valgtHjemmelHarIkkeRettighet = valgtHjemmel?.tilValgtHjemmelForStansDTO() ?: emptyList(),
            harValgtStansFraFørsteDagSomGirRett = harValgtStansFraFørsteDagSomGirRett,
        )

        is RevurderingResultat.Omgjøring -> RevurderingResultatDTO.Omgjøring(
            innvilgelsesperioder = innvilgelsesperioder?.tilDTO(),
            barnetillegg = barnetillegg?.toBarnetilleggDTO(),
            omgjørVedtak = omgjortVedtak.rammevedtakId.toString(),
        )
    }
}
