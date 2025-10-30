package no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto

import no.nav.tiltakspenger.libs.periodisering.PeriodeDTO
import no.nav.tiltakspenger.libs.periodisering.toDTO
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Behandlingstype
import no.nav.tiltakspenger.saksbehandling.behandling.domene.RevurderingResultat
import no.nav.tiltakspenger.saksbehandling.behandling.domene.SøknadsbehandlingResultat
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.barnetillegg.BarnetilleggDTO
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.barnetillegg.toBarnetilleggDTO
import no.nav.tiltakspenger.saksbehandling.infra.route.AntallDagerPerMeldeperiodeDTO
import no.nav.tiltakspenger.saksbehandling.infra.route.tilAntallDagerPerMeldeperiodeDTO
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.ValgteTiltaksdeltakelser
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.infra.route.TiltaksdeltakelsePeriodeDTO
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltagelse.infra.route.toTiltaksdeltakelsePeriodeDTO

sealed interface RammebehandlingResultatDTO {
    val resultat: RammebehandlingResultatTypeDTO
}

sealed interface SøknadsbehandlingResultatDTO : RammebehandlingResultatDTO {

    data class Innvilgelse(
        override val innvilgelsesperiode: PeriodeDTO,
        override val valgteTiltaksdeltakelser: List<TiltaksdeltakelsePeriodeDTO>,
        override val barnetillegg: BarnetilleggDTO?,
        override val antallDagerPerMeldeperiode: List<AntallDagerPerMeldeperiodeDTO>?,
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
        override val innvilgelsesperiode: PeriodeDTO?,
        override val valgteTiltaksdeltakelser: List<TiltaksdeltakelsePeriodeDTO>?,
        override val barnetillegg: BarnetilleggDTO?,
        override val antallDagerPerMeldeperiode: List<AntallDagerPerMeldeperiodeDTO>?,
    ) : RevurderingResultatDTO,
        RammebehandlingInnvilgelseResultatDTO {
        override val resultat = RammebehandlingResultatTypeDTO.REVURDERING_INNVILGELSE
    }

    data class Stans(
        val valgtHjemmelHarIkkeRettighet: List<ValgtHjemmelForStansDTO>,
        val harValgtStansFraFørsteDagSomGirRett: Boolean?,
        val harValgtStansTilSisteDagSomGirRett: Boolean?,
    ) : RevurderingResultatDTO {
        override val resultat = RammebehandlingResultatTypeDTO.STANS
    }

    data class Omgjøring(
        override val innvilgelsesperiode: PeriodeDTO,
        override val valgteTiltaksdeltakelser: List<TiltaksdeltakelsePeriodeDTO>,
        override val barnetillegg: BarnetilleggDTO,
        override val antallDagerPerMeldeperiode: List<AntallDagerPerMeldeperiodeDTO>,
        val omgjørVedtak: String,
    ) : RevurderingResultatDTO,
        RammebehandlingInnvilgelseResultatDTO {
        override val resultat = RammebehandlingResultatTypeDTO.OMGJØRING
    }
}

sealed interface RammebehandlingInnvilgelseResultatDTO {
    val innvilgelsesperiode: PeriodeDTO?
    val valgteTiltaksdeltakelser: List<TiltaksdeltakelsePeriodeDTO>?
    val barnetillegg: BarnetilleggDTO?
    val antallDagerPerMeldeperiode: List<AntallDagerPerMeldeperiodeDTO>?
}

fun SøknadsbehandlingResultat?.tilSøknadsbehandlingResultatDTO(): SøknadsbehandlingResultatDTO {
    return when (this) {
        is SøknadsbehandlingResultat.Avslag -> SøknadsbehandlingResultatDTO.Avslag(
            avslagsgrunner = avslagsgrunner.toValgtHjemmelForAvslagDTO(),
        )

        is SøknadsbehandlingResultat.Innvilgelse -> SøknadsbehandlingResultatDTO.Innvilgelse(
            innvilgelsesperiode = innvilgelsesperiode.toDTO(),
            valgteTiltaksdeltakelser = valgteTiltaksdeltakelser.tilDTO(),
            barnetillegg = barnetillegg?.toBarnetilleggDTO(),
            antallDagerPerMeldeperiode = antallDagerPerMeldeperiode?.tilAntallDagerPerMeldeperiodeDTO(),
        )

        null -> SøknadsbehandlingResultatDTO.IkkeValgt
    }
}

fun RevurderingResultat.tilRevurderingResultatDTO(): RevurderingResultatDTO {
    return when (this) {
        is RevurderingResultat.Innvilgelse -> RevurderingResultatDTO.Innvilgelse(
            innvilgelsesperiode = innvilgelsesperiode?.toDTO(),
            valgteTiltaksdeltakelser = valgteTiltaksdeltakelser?.tilDTO(),
            barnetillegg = barnetillegg?.toBarnetilleggDTO(),
            antallDagerPerMeldeperiode = antallDagerPerMeldeperiode?.tilAntallDagerPerMeldeperiodeDTO(),
        )

        is RevurderingResultat.Stans -> RevurderingResultatDTO.Stans(
            valgtHjemmelHarIkkeRettighet = valgtHjemmel.tilValgtHjemmelForStansDTO(),
            harValgtStansFraFørsteDagSomGirRett = harValgtStansFraFørsteDagSomGirRett,
            harValgtStansTilSisteDagSomGirRett = harValgtStansTilSisteDagSomGirRett,
        )

        is RevurderingResultat.Omgjøring -> RevurderingResultatDTO.Omgjøring(
            innvilgelsesperiode = innvilgelsesperiode.toDTO(),
            valgteTiltaksdeltakelser = valgteTiltaksdeltakelser.tilDTO(),
            barnetillegg = barnetillegg.toBarnetilleggDTO(),
            antallDagerPerMeldeperiode = antallDagerPerMeldeperiode.tilAntallDagerPerMeldeperiodeDTO(),
            omgjørVedtak = omgjørRammevedtak.id.toString(),
        )
    }
}

private fun ValgteTiltaksdeltakelser.tilDTO(): List<TiltaksdeltakelsePeriodeDTO> {
    return periodisering.perioderMedVerdi.toList().map { it.toTiltaksdeltakelsePeriodeDTO() }
}
