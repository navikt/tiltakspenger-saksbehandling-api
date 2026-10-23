package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto

import arrow.core.NonEmptySet
import no.nav.tiltakspenger.libs.periode.PeriodeDTO
import no.nav.tiltakspenger.libs.periode.toDTO
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
import no.nav.tiltakspenger.saksbehandling.omgjøring.OmgjørRammevedtak
import no.nav.tiltakspenger.saksbehandling.vedtak.infra.route.OmgjøringsgradDTO
import no.nav.tiltakspenger.saksbehandling.vedtak.infra.route.OmgjøringsgradDTO.Companion.tilDTO
import java.time.LocalDate

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

    /**
     * @param forlengerRammevedtakId Vedtaket som forlenges.
     * @param forlengesFraOgMed Dagen etter siste innvilgede dag i vedtaket som forlenges.
     * Til og med-datoen saksbehandler har valgt, er slutten på vedtaksperioden.
     * @param harValgtBarnetillegg Om saksbehandler har valgt barnetillegget selv, i stedet for at det er utledet fra vedtaket som forlenges.
     */
    data class Forlengelse(
        val forlengerRammevedtakId: String,
        val forlengesFraOgMed: LocalDate,
        val harValgtBarnetillegg: Boolean,
        override val innvilgelsesperioder: InnvilgelsesperioderDTO?,
        override val barnetillegg: BarnetilleggDTO?,
    ) : RevurderingResultatDTO,
        RammebehandlingInnvilgelseResultatDTO {
        override val resultat = RammebehandlingResultatTypeDTO.FORLENGELSE
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
        override val omgjørVedtak: String?,
        override val omgjørRammevedtak: List<OmgjørRammevedtakDTO>,
    ) : RevurderingResultatDTO,
        RammebehandlingInnvilgelseResultatDTO,
        OmgjøringResultatDTO {
        override val resultat = RammebehandlingResultatTypeDTO.OMGJØRING
    }

    data class OmgjøringOpphør(
        override val omgjørVedtak: String?,
        override val omgjørRammevedtak: List<OmgjørRammevedtakDTO>,
        val valgteHjemler: NonEmptySet<HjemmelForOpphørDTO>,
    ) : RevurderingResultatDTO,
        OmgjøringResultatDTO {
        override val resultat = RammebehandlingResultatTypeDTO.OMGJØRING_OPPHØR
    }

    data class OmgjøringIkkeValgt(
        override val omgjørVedtak: String?,
        override val omgjørRammevedtak: List<OmgjørRammevedtakDTO>,
    ) : RevurderingResultatDTO,
        OmgjøringResultatDTO {
        override val resultat = RammebehandlingResultatTypeDTO.OMGJØRING_IKKE_VALGT
    }

    /**
     * @param omgjørVedtak Det eldste vedtaket som omgjøres, eller null dersom ingen vedtak omgjøres ennå.
     * Beholdes for bakoverkompatibilitet til frontend har tatt i bruk [omgjørRammevedtak].
     * @param omgjørRammevedtak Alle periodene som omgjøres, sortert på fraOgMed.
     * Kan omfatte flere vedtak og ha hull.
     */
    sealed interface OmgjøringResultatDTO {
        val omgjørVedtak: String?
        val omgjørRammevedtak: List<OmgjørRammevedtakDTO>
    }

    data class OmgjørRammevedtakDTO(
        val vedtakId: String,
        val periode: PeriodeDTO,
        val omgjøringsgrad: OmgjøringsgradDTO,
    )

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

        is Revurderingsresultat.Forlengelse -> RevurderingResultatDTO.Forlengelse(
            forlengerRammevedtakId = forlenger.rammevedtakId.toString(),
            forlengesFraOgMed = forlenger.forlengesFraOgMed,
            harValgtBarnetillegg = harValgtBarnetillegg,
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
            omgjørVedtak = omgjørRammevedtak.eldsteVedtakId(),
            omgjørRammevedtak = omgjørRammevedtak.tilOmgjørRammevedtakDTO(),
        )

        is Omgjøringsresultat.OmgjøringOpphør -> OmgjøringOpphør(
            omgjørVedtak = omgjørRammevedtak.eldsteVedtakId(),
            omgjørRammevedtak = omgjørRammevedtak.tilOmgjørRammevedtakDTO(),
            valgteHjemler = valgteHjemler.tilHjemmelForOpphørDTO(),
        )

        is Omgjøringsresultat.OmgjøringIkkeValgt -> OmgjøringIkkeValgt(
            omgjørVedtak = omgjørRammevedtak.eldsteVedtakId(),
            omgjørRammevedtak = omgjørRammevedtak.tilOmgjørRammevedtakDTO(),
        )
    }
}

private fun OmgjørRammevedtak.eldsteVedtakId(): String? = rammevedtakIDer.firstOrNull()?.toString()

private fun OmgjørRammevedtak.tilOmgjørRammevedtakDTO(): List<RevurderingResultatDTO.OmgjørRammevedtakDTO> =
    omgjøringsperioder.sortedBy { it.periode.fraOgMed }.map {
        RevurderingResultatDTO.OmgjørRammevedtakDTO(
            vedtakId = it.rammevedtakId.toString(),
            periode = it.periode.toDTO(),
            omgjøringsgrad = it.omgjøringsgrad.tilDTO(),
        )
    }
