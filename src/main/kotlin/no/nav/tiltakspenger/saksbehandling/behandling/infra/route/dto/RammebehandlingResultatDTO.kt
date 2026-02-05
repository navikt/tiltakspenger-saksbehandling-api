package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto

import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.Omgjøringsresultat
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.Omgjøringsresultat.OmgjøringInnvilgelse
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.Revurderingsresultat
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.Søknadsbehandlingsresultat
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

fun Søknadsbehandlingsresultat?.tilSøknadsbehandlingResultatDTO(): SøknadsbehandlingResultatDTO {
    return when (this) {
        is Søknadsbehandlingsresultat.Avslag -> SøknadsbehandlingResultatDTO.Avslag(
            avslagsgrunner = avslagsgrunner.toValgtHjemmelForAvslagDTO(),
        )

        is Søknadsbehandlingsresultat.Innvilgelse -> SøknadsbehandlingResultatDTO.Innvilgelse(
            innvilgelsesperioder = innvilgelsesperioder.tilDTO(),
            barnetillegg = barnetillegg.toBarnetilleggDTO(),
        )

        null -> SøknadsbehandlingResultatDTO.IkkeValgt
    }
}

fun Revurderingsresultat.tilRevurderingResultatDTO(): RevurderingResultatDTO {
    return when (this) {
        is Revurderingsresultat.Innvilgelse -> RevurderingResultatDTO.Innvilgelse(
            innvilgelsesperioder = innvilgelsesperioder?.tilDTO(),
            barnetillegg = barnetillegg?.toBarnetilleggDTO(),
        )

        is Revurderingsresultat.Stans -> RevurderingResultatDTO.Stans(
            valgtHjemmelHarIkkeRettighet = valgtHjemmel?.tilValgtHjemmelForStansDTO() ?: emptyList(),
            harValgtStansFraFørsteDagSomGirRett = harValgtStansFraFørsteDagSomGirRett,
        )

        is OmgjøringInnvilgelse -> RevurderingResultatDTO.Omgjøring(
            innvilgelsesperioder = innvilgelsesperioder?.tilDTO(),
            barnetillegg = barnetillegg?.toBarnetilleggDTO(),
            omgjørVedtak = omgjortVedtak.rammevedtakId.toString(),
        )

        is Omgjøringsresultat.OmgjøringIkkeValgt -> TODO()

        is Omgjøringsresultat.OmgjøringOpphør -> TODO()
    }
}
