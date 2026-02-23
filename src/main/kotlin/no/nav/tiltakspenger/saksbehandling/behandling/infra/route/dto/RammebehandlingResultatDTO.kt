package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto

import arrow.core.NonEmptySet
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.Omgjøringsresultat
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.Revurderingsresultat
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.Søknadsbehandlingsresultat
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.barnetillegg.BarnetilleggDTO
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.barnetillegg.toBarnetilleggDTO
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.RevurderingResultatDTO.Innvilgelse
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.RevurderingResultatDTO.OmgjøringIkkeValgt
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.RevurderingResultatDTO.OmgjøringInnvilgelse
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.RevurderingResultatDTO.OmgjøringOpphør
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.RevurderingResultatDTO.Stans

sealed interface RammebehandlingResultatDTO {
    val resultat: RammebehandlingResultatTypeDTO
}

sealed interface SøknadsbehandlingResultatDTO : RammebehandlingResultatDTO {

    data class Innvilgelse(
        override val innvilgelsesperioder: InnvilgelsesperioderDTO,
        override val barnetillegg: BarnetilleggDTO?,
    ) : SøknadsbehandlingResultatDTO,
        RevurderingResultatDTO.RammebehandlingInnvilgelseResultatDTO {
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
        val valgtHjemmelHarIkkeRettighet: List<HjemmelForStansDTO>,
        val harValgtStansFraFørsteDagSomGirRett: Boolean?,
    ) : RevurderingResultatDTO {
        override val resultat = RammebehandlingResultatTypeDTO.STANS
    }

    data class OmgjøringInnvilgelse(
        override val innvilgelsesperioder: InnvilgelsesperioderDTO?,
        override val barnetillegg: BarnetilleggDTO?,
        val omgjørVedtak: String,
    ) : RevurderingResultatDTO,
        RammebehandlingInnvilgelseResultatDTO {
        override val resultat = RammebehandlingResultatTypeDTO.OMGJØRING
    }

    data class OmgjøringOpphør(
        val omgjørVedtak: String,
        val valgteHjemler: NonEmptySet<HjemmelForOpphørDTO>,
    ) : RevurderingResultatDTO {
        override val resultat = RammebehandlingResultatTypeDTO.OMGJØRING_OPPHØR
    }

    data class OmgjøringIkkeValgt(val omgjørVedtak: String) : RevurderingResultatDTO {
        override val resultat = RammebehandlingResultatTypeDTO.OMGJØRING_IKKE_VALGT
    }

    sealed interface RammebehandlingInnvilgelseResultatDTO {
        val innvilgelsesperioder: InnvilgelsesperioderDTO?
        val barnetillegg: BarnetilleggDTO?
    }
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
        is Revurderingsresultat.Innvilgelse -> Innvilgelse(
            innvilgelsesperioder = innvilgelsesperioder?.tilDTO(),
            barnetillegg = barnetillegg?.toBarnetilleggDTO(),
        )

        is Revurderingsresultat.Stans -> Stans(
            valgtHjemmelHarIkkeRettighet = valgtHjemmel?.tilHjemmelForStansDTO() ?: emptyList(),
            harValgtStansFraFørsteDagSomGirRett = harValgtStansFraFørsteDagSomGirRett,
        )

        is Omgjøringsresultat.OmgjøringInnvilgelse -> OmgjøringInnvilgelse(
            innvilgelsesperioder = innvilgelsesperioder?.tilDTO(),
            barnetillegg = barnetillegg?.toBarnetilleggDTO(),
            omgjørVedtak = omgjortVedtak.rammevedtakId.toString(),
        )

        is Omgjøringsresultat.OmgjøringOpphør -> OmgjøringOpphør(
            omgjørVedtak = omgjortVedtak.rammevedtakId.toString(),
            valgteHjemler = valgteHjemler.tilHjemmelForOpphørDTO(),
        )

        is Omgjøringsresultat.OmgjøringIkkeValgt -> OmgjøringIkkeValgt(
            omgjørVedtak = omgjortVedtak.rammevedtakId.toString(),
        )
    }
}
